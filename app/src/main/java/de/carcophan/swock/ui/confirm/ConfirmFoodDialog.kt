package de.carcophan.swock.ui.confirm

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Spa
import de.carcophan.swock.model.VitaminAmount
import de.carcophan.swock.model.VitaminType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import de.carcophan.swock.model.FoodAnalysisResult
import de.carcophan.swock.model.HealthRating
import de.carcophan.swock.model.MealEntry
import de.carcophan.swock.util.SymbolicFoodImageGenerator
import de.carcophan.swock.ui.common.HealthExplanationBox
import de.carcophan.swock.ui.common.HealthRatingBadge
import de.carcophan.swock.ui.common.HealthTrafficLightMiniPill
import de.carcophan.swock.ui.common.HealthTrafficLightSelector
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private fun formatDecimal(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
}

@Composable
fun ConfirmFoodDialog(
    imagePath: String,
    initialAnalysis: FoodAnalysisResult,
    existingMealId: Long = 0,
    title: String = "Mahlzeit bestätigen",
    subtitle: String = "Von Gemini ermittelt – passe die Werte bei Bedarf an:",
    confirmButtonText: String = "Speichern",
    initialTimestamp: Long = System.currentTimeMillis(),
    initialDateKey: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (MealEntry) -> Unit
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(initialAnalysis.name) }
    var quantity by rememberSaveable { mutableStateOf(1.0) }

    var baseCalories by rememberSaveable { mutableStateOf(initialAnalysis.calories.toDouble()) }
    var baseProtein by rememberSaveable { mutableStateOf(initialAnalysis.proteinGrams) }
    var baseCarbs by rememberSaveable { mutableStateOf(initialAnalysis.carbsGrams) }
    var baseFat by rememberSaveable { mutableStateOf(initialAnalysis.fatGrams) }

    var caloriesText by rememberSaveable { mutableStateOf(initialAnalysis.calories.toString()) }
    var proteinText by rememberSaveable { mutableStateOf(initialAnalysis.proteinGrams.toString()) }
    var carbsText by rememberSaveable { mutableStateOf(initialAnalysis.carbsGrams.toString()) }
    var fatText by rememberSaveable { mutableStateOf(initialAnalysis.fatGrams.toString()) }
    var portionDesc by rememberSaveable { mutableStateOf(initialAnalysis.portionDesc) }
    var isPortionCustomized by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf(initialAnalysis.notes) }
    var healthRating by rememberSaveable { mutableStateOf(initialAnalysis.healthRating) }
    var healthReason by rememberSaveable { mutableStateOf(initialAnalysis.healthReason) }

    val baseVitamins = remember { initialAnalysis.vitamins }
    var currentVitamins by remember { mutableStateOf(initialAnalysis.vitamins) }

    fun updateForQuantity(newQ: Double) {
        if (newQ <= 0.0) return
        val clamped = Math.round(newQ * 10.0) / 10.0
        quantity = clamped

        val newCal = (baseCalories * clamped).roundToInt()
        val newProtein = (baseProtein * clamped)
        val newCarbs = (baseCarbs * clamped)
        val newFat = (baseFat * clamped)

        caloriesText = newCal.toString()
        proteinText = formatDecimal(newProtein)
        carbsText = formatDecimal(newCarbs)
        fatText = formatDecimal(newFat)

        currentVitamins = baseVitamins.map { vit ->
            vit.copy(amount = Math.round(vit.amount * clamped * 100.0) / 100.0)
        }

        if (!isPortionCustomized) {
            val qStr = if (clamped % 1.0 == 0.0) clamped.toInt().toString() else String.format(Locale.GERMAN, "%.1f", clamped)
            portionDesc = if (initialAnalysis.portionDesc.isBlank()) {
                if (clamped == 1.0) "1 Portion" else "$qStr Portionen"
            } else {
                if (clamped == 1.0) initialAnalysis.portionDesc else "$qStr x ${initialAnalysis.portionDesc}"
            }
        }
    }

    val currentTimestamp = rememberSaveable { initialTimestamp }
    val timeFormat = remember { SimpleDateFormat("HH:mm 'Uhr'", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val dateKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val formattedTime = remember(currentTimestamp) { timeFormat.format(Date(currentTimestamp)) }
    val formattedDate = remember(currentTimestamp) { dateFormat.format(Date(currentTimestamp)) }
    val dateKey = remember(currentTimestamp) { initialDateKey ?: dateKeyFormat.format(Date(currentTimestamp)) }

    val bitmap = remember(imagePath) {
        val file = File(imagePath)
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Image Preview or Text-based Meal Banner
                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = if (imagePath.contains("symbolic")) "Symbolbild der Mahlzeit" else "Aufgenommenes Essen",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (imagePath.contains("symbolic")) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Symbolbild",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val theme = remember(name) {
                        SymbolicFoodImageGenerator.resolveTheme(name, initialAnalysis.category, initialAnalysis.emoji)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(theme.startColor), Color(theme.endColor))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        )
                        Text(
                            text = theme.defaultEmoji,
                            fontSize = 44.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.35f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = theme.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date & Time badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$formattedDate um $formattedTime",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name der Speise / Getränk") },
                    leadingIcon = {
                        Icon(Icons.Default.Restaurant, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quantity Stepper (+ / - buttons and presets)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Anzahl / Portionen",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Nährwerte passen sich automatisch an",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Stepper controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        val newQ = if (quantity > 1.0) {
                                            if (quantity % 1.0 != 0.0) quantity - 0.5 else quantity - 1.0
                                        } else if (quantity == 1.0) 0.5 else 0.5
                                        updateForQuantity(newQ)
                                    },
                                    enabled = quantity > 0.5,
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Anzahl verringern",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = if (quantity % 1.0 == 0.0) "${quantity.toInt()}x" else "${String.format(Locale.GERMAN, "%.1f", quantity)}x",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        val newQ = if (quantity < 1.0) 1.0 else if (quantity % 1.0 != 0.0) quantity + 0.5 else quantity + 1.0
                                        updateForQuantity(newQ)
                                    },
                                    enabled = quantity < 20.0,
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Anzahl erhöhen",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick selection chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(0.5, 1.0, 1.5, 2.0, 3.0)
                            presets.forEach { preset ->
                                val isSelected = Math.abs(quantity - preset) < 0.01
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { updateForQuantity(preset) },
                                    label = {
                                        Text(
                                            text = if (preset % 1.0 == 0.0) "${preset.toInt()}x" else "${String.format(Locale.GERMAN, "%.1f", preset)}x",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calories
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { input ->
                        val filtered = input.filter { ch -> ch.isDigit() }
                        caloriesText = filtered
                        val parsed = filtered.toDoubleOrNull()
                        if (parsed != null && quantity > 0) {
                            baseCalories = parsed / quantity
                        }
                    },
                    label = { Text("Kalorien (kcal)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Macros row: Protein, Carbs, Fat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { input ->
                            proteinText = input
                            val parsed = input.replace(',', '.').toDoubleOrNull()
                            if (parsed != null && quantity > 0) {
                                baseProtein = parsed / quantity
                            }
                        },
                        label = { Text("Eiweiß (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { input ->
                            carbsText = input
                            val parsed = input.replace(',', '.').toDoubleOrNull()
                            if (parsed != null && quantity > 0) {
                                baseCarbs = parsed / quantity
                            }
                        },
                        label = { Text("KH (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { input ->
                            fatText = input
                            val parsed = input.replace(',', '.').toDoubleOrNull()
                            if (parsed != null && quantity > 0) {
                                baseFat = parsed / quantity
                            }
                        },
                        label = { Text("Fett (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ernährungs-Ampel (Traffic Light Indicator)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HealthTrafficLightMiniPill(activeRating = healthRating)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lebensmittel-Ampel (A–F)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HealthRatingBadge(rating = healthRating)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        HealthTrafficLightSelector(
                            selectedRating = healthRating,
                            onRatingSelected = { healthRating = it }
                        )

                        if (healthReason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HealthExplanationBox(
                                rating = healthRating,
                                reason = healthReason
                            )
                        }
                    }
                }

                if (currentVitamins.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Spa,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Enthaltene Vitamine (${currentVitamins.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (vit in currentVitamins) {
                                    val amountStr = if (vit.amount % 1.0 == 0.0) vit.amount.toInt().toString() else String.format(Locale.GERMAN, "%.1f", vit.amount)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color(vit.type.tagColorHex), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${vit.type.shortName}: $amountStr ${vit.unit}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Portion
                OutlinedTextField(
                    value = portionDesc,
                    onValueChange = { input ->
                        portionDesc = input
                        isPortionCustomized = true
                    },
                    label = { Text("Portionsgröße (z. B. 1 Teller, ca. 350g)") },
                    leadingIcon = {
                        Icon(Icons.Default.Fastfood, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notizen / Zutaten (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abbrechen")
                    }

                    Button(
                        onClick = {
                            val cal = caloriesText.toIntOrNull() ?: 0
                            val protein = proteinText.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val carbs = carbsText.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val fat = fatText.replace(',', '.').toDoubleOrNull() ?: 0.0

                            val finalImagePath = if (imagePath.isBlank() || !File(imagePath).exists()) {
                                try {
                                    SymbolicFoodImageGenerator.generateSymbolicFoodImageSync(
                                        context = context,
                                        name = name.ifBlank { "Unbekannte Mahlzeit" },
                                        categoryHint = initialAnalysis.category,
                                        emojiHint = initialAnalysis.emoji,
                                        healthRating = healthRating
                                    )
                                } catch (e: Exception) {
                                    imagePath
                                }
                            } else {
                                imagePath
                            }

                            val entry = MealEntry(
                                id = existingMealId,
                                name = name.ifBlank { "Unbekannte Mahlzeit" },
                                calories = cal,
                                proteinGrams = protein,
                                carbsGrams = carbs,
                                fatGrams = fat,
                                portionDesc = portionDesc.ifBlank { "1 Portion" },
                                imagePath = finalImagePath,
                                timestamp = currentTimestamp,
                                dateKey = dateKey,
                                notes = notes,
                                healthRating = healthRating,
                                healthReason = healthReason,
                                vitaminsJson = VitaminAmount.listToJsonString(currentVitamins)
                            )
                            onConfirm(entry)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(confirmButtonText)
                    }
                }
            }
        }
    }
}
