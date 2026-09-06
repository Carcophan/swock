package de.carcophan.swock.model

enum class RecommendationCategory(val displayName: String, val emoji: String) {
    FOOD("Essen", "🥗"),
    DRINK("Trinken", "💧"),
    GENERAL("Allgemein", "✨")
}

enum class AvoidanceSeverity(val label: String, val levelColorHex: Long) {
    HIGH("Dringend meiden", 0xFFE53935),    // Red
    MODERATE("Einschränken", 0xFFFB8C00),    // Orange
    INFO("Im Auge behalten", 0xFFFDD835)     // Yellow
}

data class RecommendationItem(
    val title: String,
    val items: List<String>,
    val reason: String,
    val category: RecommendationCategory = RecommendationCategory.FOOD,
    val nutrientFocus: String = ""
)

data class AvoidanceItem(
    val title: String,
    val items: List<String>,
    val reason: String,
    val severity: AvoidanceSeverity = AvoidanceSeverity.MODERATE
)

data class DailyNutritionAdvice(
    val balanceScore: String, // z. B. "Ausgewogen", "Leichtes Defizit", "Hoher Fettanteil"
    val summaryText: String,  // Kurze Gesamteinschätzung des Tages
    val recommendations: List<RecommendationItem>,
    val avoidances: List<AvoidanceItem>,
    val isAiGenerated: Boolean = false,
    val generatedTimestamp: Long = System.currentTimeMillis()
)
