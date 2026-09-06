package de.carcophan.swock.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import de.carcophan.swock.Screen
import de.carcophan.swock.data.preferences.UserProfileRepository
import de.carcophan.swock.data.remote.GeminiFoodService
import de.carcophan.swock.data.repository.MealRepository
import de.carcophan.swock.model.FoodAnalysisResult
import de.carcophan.swock.model.MealEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val mealRepository = MealRepository(application)
    val profileRepository = UserProfileRepository(application)
    val geminiFoodService = GeminiFoodService(application)

    var currentScreen by mutableStateOf(Screen.DASHBOARD)

    var isAnalyzing by mutableStateOf(false)
        private set

    var analysisErrorMessage by mutableStateOf<String?>(null)

    var pendingAnalysisResult by mutableStateOf<Pair<String, FoodAnalysisResult>?>(null)

    var tempCameraUri: Uri?
        get() = savedStateHandle.get<String>("temp_camera_uri")?.let { Uri.parse(it) }
        set(value) { savedStateHandle["temp_camera_uri"] = value?.toString() }

    fun createCameraUri(): Uri {
        val context = getApplication<Application>()
        val cacheImagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val tempFile = File(cacheImagesDir, "camera_photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        tempCameraUri = uri
        return uri
    }

    fun onCameraResult(success: Boolean) {
        val uri = tempCameraUri
        if (success && uri != null) {
            processImageUri(uri)
        }
    }

    fun processImageUri(uri: Uri) {
        viewModelScope.launch {
            val profile = profileRepository.profile.first()
            if (profile.geminiApiKey.isBlank()) {
                analysisErrorMessage = "Bitte hinterlege zuerst deinen kostenlosen Gemini API-Schlüssel in den Profileinstellungen."
                return@launch
            }

            isAnalyzing = true
            analysisErrorMessage = null

            try {
                val savedLocalPath = mealRepository.saveImageFromUri(uri)
                val result = geminiFoodService.analyzeFoodImage(
                    imagePath = savedLocalPath,
                    apiKey = profile.geminiApiKey,
                    preferredModel = profile.geminiModel
                )

                isAnalyzing = false
                result.fold(
                    onSuccess = { analysis ->
                        pendingAnalysisResult = Pair(savedLocalPath, analysis)
                    },
                    onFailure = { error ->
                        analysisErrorMessage = error.localizedMessage ?: "Fehler bei der Bildanalyse durch Gemini."
                    }
                )
            } catch (e: Exception) {
                isAnalyzing = false
                analysisErrorMessage = "Fehler beim Laden des Bildes: ${e.localizedMessage}"
            }
        }
    }

    fun processTextDescription(description: String) {
        viewModelScope.launch {
            val profile = profileRepository.profile.first()
            if (profile.geminiApiKey.isBlank()) {
                analysisErrorMessage = "Bitte hinterlege zuerst deinen kostenlosen Gemini API-Schlüssel in den Profileinstellungen."
                return@launch
            }

            isAnalyzing = true
            analysisErrorMessage = null

            try {
                val result = geminiFoodService.analyzeFoodDescription(
                    description = description,
                    apiKey = profile.geminiApiKey,
                    preferredModel = profile.geminiModel
                )

                isAnalyzing = false
                result.fold(
                    onSuccess = { analysis ->
                        pendingAnalysisResult = Pair("", analysis)
                    },
                    onFailure = { error ->
                        analysisErrorMessage = error.localizedMessage ?: "Fehler bei der Textanalyse durch Gemini."
                    }
                )
            } catch (e: Exception) {
                isAnalyzing = false
                analysisErrorMessage = "Fehler bei der Analyse: ${e.localizedMessage}"
            }
        }
    }

    fun dismissAnalysisResult() {
        pendingAnalysisResult = null
    }

    fun dismissError() {
        analysisErrorMessage = null
    }

    fun saveConfirmedMeal(meal: MealEntry, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            mealRepository.saveMeal(meal)
            mealRepository.selectDate(meal.dateKey)
            pendingAnalysisResult = null
            currentScreen = Screen.DASHBOARD
            onSaved()
        }
    }
}
