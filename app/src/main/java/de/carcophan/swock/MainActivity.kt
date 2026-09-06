package de.carcophan.swock

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import de.carcophan.swock.data.preferences.UserProfileRepository
import de.carcophan.swock.data.remote.GeminiFoodService
import de.carcophan.swock.data.repository.MealRepository
import de.carcophan.swock.model.FoodAnalysisResult
import androidx.lifecycle.ViewModelProvider
import de.carcophan.swock.ui.MainViewModel
import de.carcophan.swock.ui.confirm.ConfirmFoodDialog
import de.carcophan.swock.ui.dashboard.DashboardScreen
import de.carcophan.swock.ui.history.HistoryScreen
import de.carcophan.swock.ui.settings.SettingsScreen
import de.carcophan.swock.ui.theme.SwockTheme

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("Heute", Icons.Default.Dashboard),
    HISTORY("Verlauf", Icons.Default.History),
    SETTINGS("Profil", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SwockTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val profile by viewModel.profileRepository.profile.collectAsState()
    val healthCaloriesResult by viewModel.healthCaloriesForSelectedDate.collectAsState()
    val isHealthConnectAuthorized by viewModel.isHealthConnectAuthorized.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.mealRepository.refreshData()
                viewModel.refreshHealthConnect()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel.currentScreen) {
        if (viewModel.currentScreen == Screen.DASHBOARD) {
            viewModel.mealRepository.refreshData()
            viewModel.refreshHealthConnect()
        }
    }

    val isAnalyzing = viewModel.isAnalyzing
    val analysisErrorMessage = viewModel.analysisErrorMessage
    val pendingAnalysisResult = viewModel.pendingAnalysisResult

    // Camera Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onCameraResult(success)
    }

    // Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = viewModel.createCameraUri()
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Kamerazugriff wird für Fotos benötigt.", Toast.LENGTH_LONG).show()
        }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.processImageUri(uri)
        }
    }

    fun startCameraCapture() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val uri = viewModel.createCameraUri()
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun startGalleryPick() {
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp
            ) {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = viewModel.currentScreen == screen,
                        onClick = {
                            if (viewModel.currentScreen != screen) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.currentScreen = screen
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = viewModel.currentScreen,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() }
                        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))).togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() }
                            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        )
                    } else {
                        (slideInHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            initialOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() }
                        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))).togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() }
                            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        )
                    }
                },
                label = "ScreenTransition",
                modifier = Modifier.fillMaxSize()
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.DASHBOARD -> DashboardScreen(
                        mealRepository = viewModel.mealRepository,
                        profileRepository = viewModel.profileRepository,
                        onCapturePhoto = { startCameraCapture() },
                        onPickImage = { startGalleryPick() },
                        onDescribeMeal = { text -> viewModel.processTextDescription(text) },
                        healthCaloriesResult = healthCaloriesResult,
                        onRefresh = {
                            viewModel.mealRepository.refreshData()
                            viewModel.refreshHealthConnect()
                        }
                    )
                    Screen.HISTORY -> HistoryScreen(
                        mealRepository = viewModel.mealRepository,
                        profileRepository = viewModel.profileRepository,
                        onSelectDayAndNavigateToDashboard = { dateKey ->
                            viewModel.mealRepository.selectDate(dateKey)
                            viewModel.currentScreen = Screen.DASHBOARD
                        }
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        profileRepository = viewModel.profileRepository,
                        geminiFoodService = viewModel.geminiFoodService,
                        healthConnectManager = viewModel.healthConnectManager,
                        isHealthConnectAuthorized = isHealthConnectAuthorized,
                        onRefreshHealthConnect = { viewModel.refreshHealthConnect() },
                        backupManager = viewModel.backupManager,
                        googleDriveAuthManager = viewModel.googleDriveAuthManager,
                        googleDriveService = viewModel.googleDriveService,
                        onDataRestored = { viewModel.mealRepository.refreshData() }
                    )
                }
            }

            // Loading Dialog during Gemini Analysis
            if (isAnalyzing) {
                Dialog(onDismissRequest = {}) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gemini analysiert dein Essen...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Nahrungsmittel, Kalorien und Nährwerte werden präzise berechnet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Error Dialog
            analysisErrorMessage?.let { errMsg ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissError() },
                    icon = {
                        Icon(
                            imageVector = if (profile.geminiApiKey.isBlank()) Icons.Default.Key else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = {
                        Text(
                            text = if (profile.geminiApiKey.isBlank()) "API-Schlüssel erforderlich" else "Analyse fehlgeschlagen"
                        )
                    },
                    text = { Text(errMsg) },
                    confirmButton = {
                        if (profile.geminiApiKey.isBlank()) {
                            Button(onClick = {
                                viewModel.dismissError()
                                viewModel.currentScreen = Screen.SETTINGS
                            }) {
                                Text("Zu den Einstellungen")
                            }
                        } else {
                            Button(onClick = { viewModel.dismissError() }) {
                                Text("Verstanden")
                            }
                        }
                    },
                    dismissButton = {
                        if (profile.geminiApiKey.isBlank()) {
                            TextButton(onClick = { viewModel.dismissError() }) {
                                Text("Später")
                            }
                        }
                    }
                )
            }

            // Confirmation and Adjustment Dialog
            pendingAnalysisResult?.let { (imagePath, analysis) ->
                ConfirmFoodDialog(
                    imagePath = imagePath,
                    initialAnalysis = analysis,
                    onDismiss = { viewModel.dismissAnalysisResult() },
                    onConfirm = { confirmedMeal ->
                        viewModel.saveConfirmedMeal(confirmedMeal) {
                            Toast.makeText(context, "${confirmedMeal.name} gespeichert!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}