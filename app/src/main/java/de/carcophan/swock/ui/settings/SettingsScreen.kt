package de.carcophan.swock.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import de.carcophan.swock.data.backup.BackupManager
import de.carcophan.swock.data.backup.DriveBackupFile
import de.carcophan.swock.data.backup.GoogleDriveAuthManager
import de.carcophan.swock.data.backup.GoogleDriveService
import de.carcophan.swock.data.health.HealthConnectManager
import de.carcophan.swock.data.preferences.UserProfileRepository
import de.carcophan.swock.data.remote.GeminiFoodService
import de.carcophan.swock.domain.CalorieCalculator
import de.carcophan.swock.model.ActivityLevel
import de.carcophan.swock.model.CalorieGoal
import de.carcophan.swock.model.Gender
import de.carcophan.swock.model.UserProfile
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profileRepository: UserProfileRepository,
    geminiFoodService: GeminiFoodService,
    healthConnectManager: HealthConnectManager? = null,
    isHealthConnectAuthorized: Boolean = false,
    onRefreshHealthConnect: () -> Unit = {},
    backupManager: BackupManager? = null,
    googleDriveAuthManager: GoogleDriveAuthManager? = null,
    googleDriveService: GoogleDriveService? = null,
    onDataRestored: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentProfile by profileRepository.profile.collectAsState()

    var ageText by remember(currentProfile) { mutableStateOf(currentProfile.age.toString()) }
    var gender by remember(currentProfile) { mutableStateOf(currentProfile.gender) }
    var heightText by remember(currentProfile) { mutableStateOf(currentProfile.heightCm.toInt().toString()) }
    var weightText by remember(currentProfile) { mutableStateOf(currentProfile.weightKg.toString()) }
    var activityLevel by remember(currentProfile) { mutableStateOf(currentProfile.activityLevel) }
    var calorieGoal by remember(currentProfile) { mutableStateOf(currentProfile.calorieGoal) }
    var geminiApiKey by remember(currentProfile) { mutableStateOf(currentProfile.geminiApiKey) }
    var geminiModel by remember(currentProfile) {
        mutableStateOf(
            if (currentProfile.geminiModel != "gemini-3.8-flash") "gemini-3.8-flash"
            else currentProfile.geminiModel
        )
    }
    var syncHealthConnect by remember(currentProfile) { mutableStateOf(currentProfile.syncHealthConnect) }

    var autoBackupEnabled by remember(currentProfile) { mutableStateOf(currentProfile.autoBackupEnabled) }
    var backupIntervalDays by remember(currentProfile) { mutableStateOf(currentProfile.backupIntervalDays) }
    var backupWifiOnly by remember(currentProfile) { mutableStateOf(currentProfile.backupWifiOnly) }
    var googleAccountEmail by remember(currentProfile) { mutableStateOf(currentProfile.googleAccountEmail) }

    var isCreatingCloudBackup by remember { mutableStateOf(false) }
    var isRestoringCloudBackup by remember { mutableStateOf(false) }
    var isFetchingBackups by remember { mutableStateOf(false) }
    var cloudBackupsList by remember { mutableStateOf<List<DriveBackupFile>?>(null) }
    var showCloudBackupsDialog by remember { mutableStateOf(false) }
    var selectedBackupForRestore by remember { mutableStateOf<DriveBackupFile?>(null) }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var showGoogleCloudHelpDialog by remember { mutableStateOf(false) }

    var isExportingLocal by remember { mutableStateOf(false) }
    var isImportingLocal by remember { mutableStateOf(false) }
    var pendingLocalImportUri by remember { mutableStateOf<Uri?>(null) }
    var showConfirmLocalRestoreDialog by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (googleDriveAuthManager != null) {
            val res = googleDriveAuthManager.parseSignInResult(result.data)
            res.onSuccess { account ->
                val email = account.email ?: "Verbunden"
                googleAccountEmail = email
                profileRepository.updateGoogleAccountEmail(email)
                Toast.makeText(context, "Google Drive verbunden: $email", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "Verbindung fehlgeschlagen: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val exportLocalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null && backupManager != null) {
            coroutineScope.launch {
                isExportingLocal = true
                val tempZip = File(context.cacheDir, backupManager.generateBackupFileName())
                val res = backupManager.createBackupZip(tempZip)
                res.onSuccess { info ->
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempZip.inputStream().use { input -> input.copyTo(out) }
                        }
                        Toast.makeText(context, "Backup erfolgreich exportiert (${info.metadata.mealCount} Mahlzeiten, ${info.metadata.imageCount} Bilder)!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Fehler beim Schreiben des Backups: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        tempZip.delete()
                    }
                }.onFailure { err ->
                    Toast.makeText(context, "Backup-Erstellung fehlgeschlagen: ${err.message}", Toast.LENGTH_LONG).show()
                }
                isExportingLocal = false
            }
        }
    }

    val importLocalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingLocalImportUri = uri
            showConfirmLocalRestoreDialog = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        onRefreshHealthConnect()
    }

    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isTestingKey by remember { mutableStateOf(false) }
    var keyTestResult by remember { mutableStateOf<Result<Boolean>?>(null) }

    var activityDropdownExpanded by remember { mutableStateOf(false) }
    var goalDropdownExpanded by remember { mutableStateOf(false) }

    // Live computed preview
    val previewProfile = remember(
        ageText, gender, heightText, weightText, activityLevel, calorieGoal,
        geminiApiKey, geminiModel, syncHealthConnect,
        autoBackupEnabled, backupIntervalDays, backupWifiOnly, googleAccountEmail
    ) {
        val age = ageText.toIntOrNull() ?: 30
        val height = heightText.replace(',', '.').toDoubleOrNull() ?: 178.0
        val weight = weightText.replace(',', '.').toDoubleOrNull() ?: 75.0
        UserProfile(
            age = age,
            gender = gender,
            heightCm = height,
            weightKg = weight,
            activityLevel = activityLevel,
            calorieGoal = calorieGoal,
            geminiApiKey = geminiApiKey.trim(),
            geminiModel = geminiModel,
            syncHealthConnect = syncHealthConnect,
            autoBackupEnabled = autoBackupEnabled,
            backupIntervalDays = backupIntervalDays,
            backupWifiOnly = backupWifiOnly,
            lastBackupTimestamp = currentProfile.lastBackupTimestamp,
            googleAccountEmail = googleAccountEmail
        )
    }

    val previewBmr = remember(previewProfile) { CalorieCalculator.calculateBmr(previewProfile) }
    val previewTdee = remember(previewProfile) { CalorieCalculator.calculateTdee(previewProfile) }
    val previewTarget = remember(previewProfile) { CalorieCalculator.calculateDailyTarget(previewProfile) }

    val latestProfile by rememberUpdatedState(previewProfile)
    DisposableEffect(Unit) {
        onDispose {
            profileRepository.updateProfile(latestProfile)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Grundeinstellungen & Profil",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 1. Live Metabolism Calculation Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dein berechneter Energiebedarf",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Grundumsatz (BMR)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "$previewBmr kcal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Bedarf in völliger Ruhe",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    Column {
                        Text(
                            text = "Gesamtumsatz (TDEE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "$previewTdee kcal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Inkl. Aktivitätsfaktor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tägliches Kalorienziel:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$previewTarget kcal / Tag",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Personal Body Stats Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Körperdaten",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Gender Chips
                Text(
                    text = "Biologisches Geschlecht (für Formel-Berechnung)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = gender == Gender.MALE,
                        onClick = { gender = Gender.MALE },
                        label = { Text("Männlich") }
                    )
                    FilterChip(
                        selected = gender == Gender.FEMALE,
                        onClick = { gender = Gender.FEMALE },
                        label = { Text("Weiblich") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Age & Height
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Alter (Jahre)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Größe (cm)") },
                        leadingIcon = { Icon(Icons.Default.Height, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Weight
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Aktuelles Gewicht (kg)") },
                    leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Activity Level Dropdown
                Text(
                    text = "Allgemeine Tätigkeit / Aktivitätsgrad",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = activityDropdownExpanded,
                    onExpandedChange = { activityDropdownExpanded = !activityDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${activityLevel.displayName} (PAL ${activityLevel.factor})",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = activityDropdownExpanded,
                        onDismissRequest = { activityDropdownExpanded = false }
                    ) {
                        ActivityLevel.values().forEach { level ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = "${level.displayName} (×${level.factor})", fontWeight = FontWeight.Bold)
                                        Text(text = level.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    activityLevel = level
                                    activityDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Goal Dropdown
                Text(
                    text = "Kalorienziel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = goalDropdownExpanded,
                    onExpandedChange = { goalDropdownExpanded = !goalDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = calorieGoal.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = goalDropdownExpanded,
                        onDismissRequest = { goalDropdownExpanded = false }
                    ) {
                        CalorieGoal.values().forEach { goal ->
                            DropdownMenuItem(
                                text = { Text(text = goal.displayName) },
                                onClick = {
                                    calorieGoal = goal
                                    goalDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Health Connect / Google Fit Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Google Fit & Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Health Connect Synchronisierung",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = syncHealthConnect,
                        onCheckedChange = { checked ->
                            syncHealthConnect = checked
                            if (checked && healthConnectManager != null && !isHealthConnectAuthorized) {
                                permissionLauncher.launch(healthConnectManager.permissions)
                            }
                        }
                    )
                }

                Text(
                    text = "Liest deinen tatsächlichen täglichen Kalorienverbrauch (Grundumsatz & Aktivität) direkt aus Google Fit, Pixel Watch, Samsung Health, Garmin etc. aus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                )

                if (syncHealthConnect) {
                    val isAvailable = healthConnectManager?.isAvailable() == true
                    if (!isAvailable) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Health Connect ist auf diesem Gerät derzeit nicht verfügbar oder muss im Google Play Store installiert/aktualisiert werden.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else if (isHealthConnectAuthorized) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verbunden: Aktuelle Verbrauchsdaten werden im Dashboard live einberechnet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Berechtigungen zum Lesen der Kaloriendaten fehlen noch.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (healthConnectManager != null) {
                                            permissionLauncher.launch(healthConnectManager.permissions)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Berechtigungen erteilen")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Gemini API Key Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gemini KI-Konfiguration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Benötigt für die automatische Fotoerkennung und Nährwertanalyse von Speisen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                // Model Selection
                Text(
                    text = "Gemini Modell",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("gemini-3.8-flash").forEach { modelOption ->
                        FilterChip(
                            selected = geminiModel == modelOption,
                            onClick = {
                                geminiModel = modelOption
                                keyTestResult = null
                                profileRepository.updateProfile(previewProfile.copy(geminiModel = modelOption))
                            },
                            label = {
                                Text(
                                    text = "3.8 Flash",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = {
                        geminiApiKey = it
                        keyTestResult = null
                    },
                    label = { Text("Gemini API-Schlüssel") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Sichtbarkeit umschalten"
                            )
                        }
                    },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            isTestingKey = true
                            keyTestResult = null
                            coroutineScope.launch {
                                val res = geminiFoodService.testApiKey(geminiApiKey.trim(), geminiModel)
                                isTestingKey = false
                                keyTestResult = res
                                if (res.isSuccess) {
                                    profileRepository.updateProfile(
                                        previewProfile.copy(
                                            geminiApiKey = geminiApiKey.trim(),
                                            geminiModel = geminiModel
                                        )
                                    )
                                }
                            }
                        },
                        enabled = geminiApiKey.isNotBlank() && !isTestingKey,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTestingKey) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wird getestet...")
                        } else {
                            Text("Schlüssel testen")
                        }
                    }

                    // Test result status
                    keyTestResult?.let { result ->
                        if (result.isSuccess) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gültig ($geminiModel)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fehler", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // 5. Google Drive Datensicherung Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Google Drive Datensicherung",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mahlzeiten, Fotos & Einstellungen sichern",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { showGoogleCloudHelpDialog = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Setup-Informationen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Account Connection State
                val isGoogleConnected = googleAccountEmail.isNotBlank()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGoogleConnected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isGoogleConnected) Icons.Default.CloudDone else Icons.Default.Cloud,
                                contentDescription = null,
                                tint = if (isGoogleConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isGoogleConnected) "Verbundenes Google-Konto:" else "Kein Konto verknüpft",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isGoogleConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isGoogleConnected) googleAccountEmail else "Mit Google Drive verbinden",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isGoogleConnected) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (isGoogleConnected) {
                            TextButton(
                                onClick = {
                                    googleDriveAuthManager?.signOut {
                                        googleAccountEmail = ""
                                        profileRepository.updateGoogleAccountEmail("")
                                        Toast.makeText(context, "Google-Konto getrennt.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Trennen", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (googleDriveAuthManager != null) {
                                        googleSignInLauncher.launch(googleDriveAuthManager.getSignInIntent())
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Verbinden")
                            }
                        }
                    }
                }

                // Last backup timestamp display
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentProfile.lastBackupTimestamp > 0L) {
                            val df = SimpleDateFormat("dd.MM.yyyy, HH:mm 'Uhr'", Locale.getDefault())
                            "Zuletzt gesichert: ${df.format(Date(currentProfile.lastBackupTimestamp))}"
                        } else {
                            "Noch kein Backup auf Google Drive erstellt"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isGoogleConnected) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto Backup Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Periodisches automatisches Backup",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Regelmäßig im Hintergrund auf Google Drive sichern",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { autoBackupEnabled = it }
                        )
                    }

                    if (autoBackupEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sicherungs-Intervall",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = backupIntervalDays == 1,
                                onClick = { backupIntervalDays = 1 },
                                label = { Text("Täglich", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = backupIntervalDays == 7,
                                onClick = { backupIntervalDays = 7 },
                                label = { Text("Wöchentlich (alle 7 Tage)", style = MaterialTheme.typography.labelSmall) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Nur über WLAN sichern",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = backupWifiOnly,
                                onCheckedChange = { backupWifiOnly = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Cloud Action Buttons
                    Text(
                        text = "Cloud-Aktionen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val account = googleDriveAuthManager?.getSignedInAccount()
                                if (account == null) {
                                    Toast.makeText(context, "Bitte zuerst mit Google verbinden.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isCreatingCloudBackup = true
                                coroutineScope.launch {
                                    try {
                                        val tokenRes = googleDriveAuthManager.getAccessToken(account.account ?: throw IllegalStateException("Kein Account"))
                                        val token = tokenRes.getOrThrow()
                                        val tempZip = File(context.cacheDir, backupManager?.generateBackupFileName() ?: "swock_backup.zip")
                                        val createRes = backupManager?.createBackupZip(tempZip)?.getOrThrow()
                                        val folderId = googleDriveService?.getOrCreateBackupFolder(token)?.getOrThrow() ?: throw IllegalStateException("Ordner konnte nicht erstellt werden")
                                        val uploadRes = googleDriveService.uploadBackup(token, folderId, tempZip, tempZip.name).getOrThrow()
                                        googleDriveService.pruneOldBackups(token, folderId, keepCount = 5)
                                        profileRepository.updateLastBackupTimestamp(System.currentTimeMillis())
                                        tempZip.delete()
                                        val sizeMb = String.format(Locale.US, "%.1f MB", uploadRes.sizeBytes.toDouble() / (1024 * 1024))
                                        Toast.makeText(context, "Backup erfolgreich in Google Drive gespeichert ($sizeMb)!", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cloud-Backup fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isCreatingCloudBackup = false
                                    }
                                }
                            },
                            enabled = !isCreatingCloudBackup && !isRestoringCloudBackup,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isCreatingCloudBackup) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sichert...", style = MaterialTheme.typography.labelSmall)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Jetzt sichern", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val account = googleDriveAuthManager?.getSignedInAccount()
                                if (account == null) {
                                    Toast.makeText(context, "Bitte zuerst mit Google verbinden.", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                isFetchingBackups = true
                                showCloudBackupsDialog = true
                                coroutineScope.launch {
                                    try {
                                        val token = googleDriveAuthManager.getAccessToken(account.account ?: throw IllegalStateException("Kein Account")).getOrThrow()
                                        val folderId = googleDriveService?.getOrCreateBackupFolder(token)?.getOrThrow() ?: throw IllegalStateException("Ordner nicht gefunden")
                                        val backups = googleDriveService.listBackups(token, folderId).getOrThrow()
                                        cloudBackupsList = backups
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Fehler beim Laden der Backups: ${e.message}", Toast.LENGTH_LONG).show()
                                        showCloudBackupsDialog = false
                                    } finally {
                                        isFetchingBackups = false
                                    }
                                }
                            },
                            enabled = !isCreatingCloudBackup && !isRestoringCloudBackup,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wiederherstellen", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Local Offline Export / Import Section
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Lokales Backup (ZIP-Datei)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Komplette Datensicherung manuell exportieren oder von Datei importieren.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val defaultName = backupManager?.generateBackupFileName() ?: "swock_backup.zip"
                            exportLocalLauncher.launch(defaultName)
                        },
                        enabled = !isExportingLocal && !isImportingLocal,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isExportingLocal) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export...", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportieren", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            importLocalLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                        },
                        enabled = !isExportingLocal && !isImportingLocal,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isImportingLocal) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import...", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importieren", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Save Button
        Button(
            onClick = {
                profileRepository.updateProfile(previewProfile)
                Toast.makeText(context, "Einstellungen erfolgreich gespeichert!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Einstellungen speichern", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // 1. Google Cloud Setup Info Dialog
    if (showGoogleCloudHelpDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleCloudHelpDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Google Cloud Setup-Info") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Für die Anmeldung bei Google Drive benötigt Google die Registrierung der Android-App in der Google Cloud Console:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Paketname:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(text = context.packageName, style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "SHA-1 Fingerprint (Debug):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            val sha1 = "08:6A:2A:3D:18:28:A1:57:B1:2A:A5:A9:5F:29:2A:34:2A:86:75:D4"
                            Text(text = sha1, style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("SHA1", sha1))
                                    Toast.makeText(context, "SHA-1 in die Zwischenablage kopiert!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SHA-1 kopieren", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tipp: Falls bei der Google-Anmeldung Fehlercode 10 erscheint, erstelle in Google Cloud Console eine OAuth-Client-ID für Android mit obigen Werten und aktiviere die Google Drive API.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showGoogleCloudHelpDialog = false }) {
                    Text("Verstanden")
                }
            }
        )
    }

    // 2. Cloud Backups List Dialog
    if (showCloudBackupsDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isRestoringCloudBackup) {
                    showCloudBackupsDialog = false
                    cloudBackupsList = null
                }
            },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Google Drive Backups") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    if (isFetchingBackups) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Backups werden von Google Drive geladen...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (cloudBackupsList.isNullOrEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Keine Backups im Google Drive gefunden.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cloudBackupsList?.forEach { backup ->
                                val df = SimpleDateFormat("dd.MM.yyyy, HH:mm 'Uhr'", Locale.getDefault())
                                val dateStr = df.format(Date(backup.createdTime))
                                val sizeStr = if (backup.sizeBytes < 1024 * 1024) {
                                    "${backup.sizeBytes / 1024} KB"
                                } else {
                                    String.format(Locale.US, "%.1f MB", backup.sizeBytes.toDouble() / (1024 * 1024))
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    onClick = {
                                        selectedBackupForRestore = backup
                                        showConfirmRestoreDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = dateStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(text = backup.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = sizeStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCloudBackupsDialog = false
                        cloudBackupsList = null
                    }
                ) {
                    Text("Schließen")
                }
            }
        )
    }

    // 3. Confirm Cloud Restore Dialog
    if (showConfirmRestoreDialog && selectedBackupForRestore != null) {
        val backup = selectedBackupForRestore!!
        val df = SimpleDateFormat("dd.MM.yyyy, HH:mm 'Uhr'", Locale.getDefault())
        val dateStr = df.format(Date(backup.createdTime))

        AlertDialog(
            onDismissRequest = {
                if (!isRestoringCloudBackup) {
                    showConfirmRestoreDialog = false
                    selectedBackupForRestore = null
                }
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Backup wiederherstellen?") },
            text = {
                Column {
                    Text(
                        text = "Möchtest du das Backup vom $dateStr wiederherstellen?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Achtung: Sämtliche aktuellen Mahlzeiten und Fotos auf diesem Gerät werden durch den Stand des Backups ersetzt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (isRestoringCloudBackup) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Wird heruntergeladen und wiederhergestellt...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val account = googleDriveAuthManager?.getSignedInAccount()
                        if (account == null) {
                            Toast.makeText(context, "Nicht angemeldet.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isRestoringCloudBackup = true
                        coroutineScope.launch {
                            val tempZip = File(context.cacheDir, "download_${backup.name}")
                            try {
                                val token = googleDriveAuthManager.getAccessToken(account.account ?: throw IllegalStateException("Kein Account")).getOrThrow()
                                googleDriveService?.downloadBackup(token, backup.id, tempZip)?.getOrThrow()
                                val restoreRes = backupManager?.restoreFromBackupZip(tempZip)?.getOrThrow()
                                onDataRestored()
                                Toast.makeText(
                                    context,
                                    "Erfolgreich wiederhergestellt! ${restoreRes?.mealCount ?: 0} Mahlzeiten und ${restoreRes?.imageCount ?: 0} Fotos geladen.",
                                    Toast.LENGTH_LONG
                                ).show()
                                showCloudBackupsDialog = false
                                showConfirmRestoreDialog = false
                                selectedBackupForRestore = null
                            } catch (e: Exception) {
                                Toast.makeText(context, "Wiederherstellung fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                tempZip.delete()
                                isRestoringCloudBackup = false
                            }
                        }
                    },
                    enabled = !isRestoringCloudBackup,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Jetzt wiederherstellen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmRestoreDialog = false
                        selectedBackupForRestore = null
                    },
                    enabled = !isRestoringCloudBackup
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // 4. Confirm Local Restore Dialog
    if (showConfirmLocalRestoreDialog && pendingLocalImportUri != null) {
        val uri = pendingLocalImportUri!!
        AlertDialog(
            onDismissRequest = {
                if (!isImportingLocal) {
                    showConfirmLocalRestoreDialog = false
                    pendingLocalImportUri = null
                }
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Lokales Backup importieren?") },
            text = {
                Column {
                    Text(
                        text = "Möchtest du das ausgewählte Backup-Archiv einlesen?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Achtung: Sämtliche aktuellen Mahlzeiten und Fotos auf diesem Gerät werden durch die Datei überschrieben.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (isImportingLocal) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Backup wird entpackt und eingespielt...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isImportingLocal = true
                        coroutineScope.launch {
                            val tempZip = File(context.cacheDir, "local_import_${System.currentTimeMillis()}.zip")
                            try {
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    tempZip.outputStream().use { out -> input.copyTo(out) }
                                }
                                val restoreRes = backupManager?.restoreFromBackupZip(tempZip)?.getOrThrow()
                                onDataRestored()
                                Toast.makeText(
                                    context,
                                    "Import erfolgreich! ${restoreRes?.mealCount ?: 0} Mahlzeiten und ${restoreRes?.imageCount ?: 0} Fotos geladen.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Import fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                tempZip.delete()
                                isImportingLocal = false
                                showConfirmLocalRestoreDialog = false
                                pendingLocalImportUri = null
                            }
                        }
                    },
                    enabled = !isImportingLocal,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Jetzt importieren")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmLocalRestoreDialog = false
                        pendingLocalImportUri = null
                    },
                    enabled = !isImportingLocal
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

