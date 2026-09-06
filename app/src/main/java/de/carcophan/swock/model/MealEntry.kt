package de.carcophan.swock.model

data class MealEntry(
    val id: Long = 0,
    val name: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val portionDesc: String,
    val imagePath: String,
    val timestamp: Long,
    val dateKey: String, // Format: yyyy-MM-dd
    val notes: String = ""
)

data class FoodAnalysisResult(
    val name: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val portionDesc: String,
    val notes: String = ""
)
