package de.carcophan.swock.ui.dashboard

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.carcophan.swock.data.preferences.UserProfileRepository
import de.carcophan.swock.data.repository.MealRepository
import de.carcophan.swock.domain.CalorieCalculator
import de.carcophan.swock.model.FoodAnalysisResult
import de.carcophan.swock.model.MealEntry
import de.carcophan.swock.ui.confirm.ConfirmFoodDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    mealRepository: MealRepository,
    profileRepository: UserProfileRepository,
    onCapturePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onDescribeMeal: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selectedDateKey by mealRepository.selectedDateKey.collectAsState()
    val meals by mealRepository.mealsForSelectedDate.collectAsState()
    val profile by profileRepository.profile.collectAsState()

    val bmr = remember(profile) { CalorieCalculator.calculateBmr(profile) }
    val tdee = remember(profile) { CalorieCalculator.calculateTdee(profile) }
    val targetCalories = remember(profile) { CalorieCalculator.calculateDailyTarget(profile) }

    val totalConsumed = remember(meals) { meals.sumOf { it.calories } }
    val totalProtein = remember(meals) { meals.sumOf { it.proteinGrams } }
    val totalCarbs = remember(meals) { meals.sumOf { it.carbsGrams } }
    val totalFat = remember(meals) { meals.sumOf { it.fatGrams } }

    val calorieStatus = remember(totalConsumed, targetCalories) {
        CalorieCalculator.evaluateDay(totalConsumed, targetCalories)
    }

    val todayKey = remember { mealRepository.currentDateKey() }
    val isToday = selectedDateKey == todayKey

    val keyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("EEE, d. MMMM yyyy", Locale.GERMAN) }

    val formattedDisplayDate = remember(selectedDateKey) {
        try {
            val date = keyFormat.parse(selectedDateKey)
            if (date != null) {
                if (selectedDateKey == todayKey) {
                    "Heute, " + SimpleDateFormat("d. MMMM", Locale.GERMAN).format(date)
                } else {
                    displayFormat.format(date)
                }
            } else selectedDateKey
        } catch (e: Exception) {
            selectedDateKey
        }
    }

    var mealToDelete by remember { mutableStateOf<MealEntry?>(null) }
    var mealToEdit by remember { mutableStateOf<MealEntry?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showDescribeDialog by rememberSaveable { mutableStateOf(false) }

    fun changeDay(offset: Int) {
        try {
            val date = keyFormat.parse(selectedDateKey) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val newKey = keyFormat.format(cal.time)
            mealRepository.selectDate(newKey)
        } catch (e: Exception) {
            // fallback
        }
    }

    fun openDatePicker() {
        val cal = Calendar.getInstance()
        try {
            val parsed = keyFormat.parse(selectedDateKey)
            if (parsed != null) cal.time = parsed
        } catch (e: Exception) {}

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                val newKey = keyFormat.format(pickedCal.time)
                mealRepository.selectDate(newKey)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Date Navigation Bar
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { changeDay(-1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Vorheriger Tag")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { openDatePicker() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Datum wählen",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formattedDisplayDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isToday) {
                                TextButton(
                                    onClick = { mealRepository.selectDate(todayKey) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Heute", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            IconButton(onClick = { changeDay(1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Nächster Tag")
                            }
                        }
                    }
                }
            }

            // 2. Calorie Overview Card
            item {
                CalorieOverviewCard(
                    consumed = totalConsumed,
                    target = targetCalories,
                    bmr = bmr,
                    tdee = tdee,
                    status = calorieStatus,
                    protein = totalProtein,
                    carbs = totalCarbs,
                    fat = totalFat
                )
            }

            // 3. Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mahlzeiten (${meals.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 4. Meal Items or Empty State
            if (meals.isEmpty()) {
                item {
                    EmptyDayView(
                        onCapturePhoto = onCapturePhoto,
                        onPickImage = onPickImage,
                        onDescribeMeal = { showDescribeDialog = true }
                    )
                }
            } else {
                items(meals, key = { it.id }) { meal ->
                    MealItemCard(
                        meal = meal,
                        onClick = { mealToEdit = meal },
                        onDeleteClick = { mealToDelete = meal }
                    )
                }
            }
        }

        // Floating Action Buttons (Capture, Pick or Describe)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(visible = showFabMenu) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            showFabMenu = false
                            showDescribeDialog = true
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Text beschreiben")
                    }

                    FilledTonalButton(
                        onClick = {
                            showFabMenu = false
                            onPickImage()
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aus Galerie")
                    }

                    FilledTonalButton(
                        onClick = {
                            showFabMenu = false
                            onCapturePhoto()
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kamera öffnen")
                    }
                }
            }

            FloatingActionButton(
                onClick = { showFabMenu = !showFabMenu },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Essen erfassen",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    if (showDescribeDialog) {
        DescribeMealDialog(
            onDismiss = { showDescribeDialog = false },
            onAnalyze = { description ->
                showDescribeDialog = false
                onDescribeMeal(description)
            }
        )
    }

    // Delete Confirmation Dialog
    mealToDelete?.let { meal ->
        AlertDialog(
            onDismissRequest = { mealToDelete = null },
            title = { Text("Mahlzeit löschen") },
            text = { Text("Möchtest du '${meal.name}' wirklich entfernen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            mealRepository.deleteMeal(meal)
                        }
                        mealToDelete = null
                    }
                ) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mealToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Edit Meal Dialog with Quantity Stepper
    mealToEdit?.let { meal ->
        ConfirmFoodDialog(
            imagePath = meal.imagePath,
            initialAnalysis = FoodAnalysisResult(
                name = meal.name,
                calories = meal.calories,
                proteinGrams = meal.proteinGrams,
                carbsGrams = meal.carbsGrams,
                fatGrams = meal.fatGrams,
                portionDesc = meal.portionDesc,
                notes = meal.notes
            ),
            existingMealId = meal.id,
            title = "Mahlzeit bearbeiten",
            subtitle = "Passe die Anzahl oder Nährwerte nach Bedarf an:",
            confirmButtonText = "Aktualisieren",
            initialTimestamp = meal.timestamp,
            initialDateKey = meal.dateKey,
            onDismiss = { mealToEdit = null },
            onConfirm = { updatedMeal ->
                coroutineScope.launch {
                    mealRepository.updateMeal(updatedMeal)
                }
                mealToEdit = null
            }
        )
    }
}

@Composable
fun CalorieOverviewCard(
    consumed: Int,
    target: Int,
    bmr: Int,
    tdee: Int,
    status: CalorieCalculator.CalorieStatus,
    protein: Double,
    carbs: Double,
    fat: Double
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Kalorienaufnahme",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$consumed",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " / $target kcal",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                    }
                }

                // Over / Under Badge
                val isOver = status.isOverBudget
                val badgeColor = if (isOver) Color(0xFFE53935) else Color(0xFF2E7D32)
                val badgeBg = if (isOver) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeBg,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOver) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOver) {
                                "+${abs(status.difference)} kcal über Ziel"
                            } else {
                                "${status.difference} kcal übrig"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            val progress = (status.percentage).coerceIn(0f, 1f)
            val progressColor = if (status.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Grundumsatz: $bmr kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Gesamtbedarf: $tdee kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Macronutrient Breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MacroStatItem(name = "Eiweiß", grams = protein, color = Color(0xFF1E88E5))
                MacroStatItem(name = "Kohlenhydrate", grams = carbs, color = Color(0xFFFB8C00))
                MacroStatItem(name = "Fett", grams = fat, color = Color(0xFFE53935))
            }
        }
    }
}

@Composable
fun MacroStatItem(name: String, grams: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${String.format(Locale.GERMAN, "%.1f", grams)} g",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MealItemCard(
    meal: MealEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm 'Uhr'", Locale.getDefault()) }
    val formattedTime = remember(meal.timestamp) { timeFormat.format(Date(meal.timestamp)) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food Thumbnail
            val imageFile = remember(meal.imagePath) { File(meal.imagePath) }
            if (imageFile.exists()) {
                AsyncImage(
                    model = imageFile,
                    contentDescription = meal.name,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fastfood,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Meal Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (meal.portionDesc.isNotBlank()) {
                    Text(
                        text = meal.portionDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${meal.calories} kcal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "E: ${meal.proteinGrams.toInt()}g • K: ${meal.carbsGrams.toInt()}g • F: ${meal.fatGrams.toInt()}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action buttons (Edit & Delete)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Mahlzeit bearbeiten / Menge anpassen",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Mahlzeit löschen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDayView(
    onCapturePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onDescribeMeal: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.RestaurantMenu,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Keine Mahlzeiten erfasst",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Fotografiere dein Essen oder beschreibe es im Text, damit Gemini es automatisch analysiert.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDescribeMeal,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Text", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onPickImage,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Galerie", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onCapturePhoto,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kamera", maxLines = 1)
                }
            }
        }
    }
}
