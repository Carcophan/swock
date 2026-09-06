package de.carcophan.swock.model

enum class HealthRating(val grade: String, val label: String, val description: String) {
    A("A", "Sehr gesund", "Nährstoffreich & vollwertig"),
    B("B", "Gesund", "Gute Nährstoffqualität"),
    C("C", "Ausgewogen", "Durchschnittlich, in Maßen gut"),
    D("D", "Mäßig", "Erhöhter Fett-, Zucker- oder Salzgehalt"),
    E("E", "Ungesund", "Stark verarbeitet / kalorienreich"),
    F("F", "Sehr ungesund", "Sehr hoher Zucker-, Fett- oder Verarbeitungsgrad");

    companion object {
        fun fromString(value: String?): HealthRating {
            return when (value?.trim()?.uppercase()) {
                "A", "GREEN", "GRÜN", "SEHR_GESUND", "SEHR GESUND" -> A
                "B", "GUT" -> B
                "C", "YELLOW", "GELB", "MODERAT", "AUSGEWOGEN" -> C
                "D", "MÄSSIG", "MAESSIG" -> D
                "E", "UNGESUND" -> E
                "F", "RED", "ROT", "SEHR_UNGESUND", "SEHR UNGESUND", "SPARSAM" -> F
                else -> C
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
    val healthRating: HealthRating = HealthRating.C,
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
    val healthRating: HealthRating = HealthRating.C,
    val healthReason: String = "",
    val category: String = "",
    val emoji: String = ""
)
