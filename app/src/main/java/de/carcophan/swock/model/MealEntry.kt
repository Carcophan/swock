package de.carcophan.swock.model

enum class HealthRating(val label: String, val description: String) {
    GREEN("Gesund", "Nährstoffreich & ausgewogen"),
    YELLOW("Moderat", "In Maßen genießen"),
    RED("Sparsam", "Hohe Kalorien- oder Zuckerdichte");

    companion object {
        fun fromString(value: String?): HealthRating {
            return when (value?.trim()?.uppercase()) {
                "GREEN", "GRÜN", "GESUND" -> GREEN
                "YELLOW", "GELB", "MODERAT", "AUSGEWOGEN" -> YELLOW
                "RED", "ROT", "UNGESUND", "SPARSAM" -> RED
                else -> YELLOW
            }
        }
    }
}

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
    val notes: String = "",
    val healthRating: HealthRating = HealthRating.YELLOW,
    val healthReason: String = ""
)

data class FoodAnalysisResult(
    val name: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val portionDesc: String,
    val notes: String = "",
    val healthRating: HealthRating = HealthRating.YELLOW,
    val healthReason: String = ""
)
