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
import de.carcophan.swock.data.health.HealthCaloriesResult
import de.carcophan.swock.data.health.HealthConnectManager
import de.carcophan.swock.data.repository.MealRepository
import de.carcophan.swock.model.FoodAnalysisResult
import de.carcophan.swock.model.MealEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import de.carcophan.swock.util.SymbolicFoodImageGenerator

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val mealRepository = MealRepository(application)
    val profileRepository = UserProfileRepository(application)
    val geminiFoodService = GeminiFoodService(application)
    val healthConnectManager = HealthConnectManager(application)
    val backupManager = de.carcophan.swock.data.backup.BackupManager(application, mealRepository, profileRepository)
    val googleDriveAuthManager = de.carcophan.swock.data.backup.GoogleDriveAuthManager(application)
    val googleDriveService = de.carcophan.swock.data.backup.GoogleDriveService()

    private val _healthCaloriesForSelectedDate = MutableStateFlow<HealthCaloriesResult?>(null)
    val healthCaloriesForSelectedDate: StateFlow<HealthCaloriesResult?> = _healthCaloriesForSelectedDate.asStateFlow()

    private val _isHealthConnectAuthorized = MutableStateFlow(false)
    val isHealthConnectAuthorized: StateFlow<Boolean> = _isHealthConnectAuthorized.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.profile.collect { prof ->
                if (prof.autoBackupEnabled && prof.googleAccountEmail.isNotBlank()) {
                    de.carcophan.swock.data.backup.BackupScheduler.schedule(
                        application,
                        prof.backupIntervalDays,
                        prof.backupWifiOnly
                    )
                } else {
                    de.carcophan.swock.data.backup.BackupScheduler.cancel(application)
                }
            }
        }

        viewModelScope.launch {
            combine(
                mealRepository.selectedDateKey,
                profileRepository.profile
            ) { dateKey, profile ->
                Pair(dateKey, profile.syncHealthConnect)
            }.collect { (dateKey, syncEnabled) ->
                if (syncEnabled && healthConnectManager.isAvailable()) {
                    val hasPerms = healthConnectManager.hasAllPermissions()
                    _isHealthConnectAuthorized.value = hasPerms
                    if (hasPerms) {
                        loadHealthCaloriesForDate(dateKey)
                    } else {
                        _healthCaloriesForSelectedDate.value = null
                    }
                } else {
                    _healthCaloriesForSelectedDate.value = null
                }
            }
        }
    }

    fun refreshHealthConnect() {
        viewModelScope.launch {
            if (healthConnectManager.isAvailable()) {
                val hasPerms = healthConnectManager.hasAllPermissions()
                _isHealthConnectAuthorized.value = hasPerms
                if (hasPerms) {
                    loadHealthCaloriesForDate(mealRepository.selectedDateKey.value)
                }
            }
        }
    }

    private suspend fun loadHealthCaloriesForDate(dateKey: String) {
        try {
            val localDate = LocalDate.parse(dateKey)
            val isToday = dateKey == mealRepository.currentDateKey()
            val startOfDay = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val result = if (isToday) {
                val upToNow = healthConnectManager.getBurnedCalories(startOfDay, Instant.now())
                val fullDay = healthConnectManager.getBurnedCalories(startOfDay, endOfDay)
                HealthCaloriesResult(
                    totalCaloriesBurned = upToNow.totalCaloriesBurned,
                    activeCaloriesBurned = upToNow.activeCaloriesBurned,
                    projectedTotalForDay = fullDay.totalCaloriesBurned
                )
            } else {
                val fullDay = healthConnectManager.getBurnedCalories(startOfDay, endOfDay)
                HealthCaloriesResult(
                    totalCaloriesBurned = fullDay.totalCaloriesBurned,
                    activeCaloriesBurned = fullDay.activeCaloriesBurned,
                    projectedTotalForDay = fullDay.totalCaloriesBurned
                )
            }
            _healthCaloriesForSelectedDate.value = result
        } catch (e: Exception) {
            _healthCaloriesForSelectedDate.value = null
        }
    }

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

                val analysisResult = result.getOrThrow()
                
                // Try generating symbolic image: attempt AI image if available, else local symbolic generator
                var symbolicImagePath = ""
                try {
                    val aiBytes = geminiFoodService.generateAiFoodImage(analysisResult.name, profile.geminiApiKey)
                    if (aiBytes != null) {
                        val context = getApplication<Application>()
                        val imagesDir = File(context.filesDir, "food_images").apply { mkdirs() }
                        val file = File(imagesDir, "symbolic_ai_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png")
                        file.writeBytes(aiBytes)
                        symbolicImagePath = file.absolutePath
                    }
                } catch (e: Exception) {
                    // Fall back to local generator
                }

                if (symbolicImagePath.isBlank()) {
                    symbolicImagePath = SymbolicFoodImageGenerator.generateSymbolicFoodImage(
                        context = getApplication(),
                        name = analysisResult.name,
                        categoryHint = analysisResult.category,
                        emojiHint = analysisResult.emoji,
                        healthRating = analysisResult.healthRating
                    )
                }

                isAnalyzing = false
                pendingAnalysisResult = Pair(symbolicImagePath, analysisResult)
            } catch (e: Exception) {
                isAnalyzing = false
                analysisErrorMessage = e.localizedMessage ?: "Fehler bei der Analyse der Mahlzeit."
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
