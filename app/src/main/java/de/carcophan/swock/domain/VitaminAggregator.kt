package de.carcophan.swock.domain

import de.carcophan.swock.model.MealEntry
import de.carcophan.swock.model.VitaminAmount
import de.carcophan.swock.model.VitaminType

data class MealContribution(
    val mealName: String,
    val amount: Double,
    val unit: String
)

data class DailyVitaminSummary(
    val type: VitaminType,
    val totalAmount: Double,
    val unit: String,
    val percentRda: Int,
    val contributingMeals: List<MealContribution>
)

object VitaminAggregator {

    /**
     * Aggregates only the vitamins that have been consumed on this day (amount > 0).
     */
    fun aggregateConsumed(meals: List<MealEntry>): List<DailyVitaminSummary> {
        val fullMap = buildAggregationMap(meals)
        return fullMap.values
            .filter { it.totalAmount > 0.0 }
            .sortedByDescending { it.percentRda }
    }

    /**
     * Returns a complete list of all 13 essential vitamins, with consumed ones first.
     */
    fun aggregateAll(meals: List<MealEntry>): List<DailyVitaminSummary> {
        val fullMap = buildAggregationMap(meals)
        return fullMap.values
            .sortedWith(
                compareByDescending<DailyVitaminSummary> { it.totalAmount > 0.0 }
                    .thenByDescending { it.percentRda }
                    .thenBy { it.type.ordinal }
            )
    }

    private fun buildAggregationMap(meals: List<MealEntry>): Map<VitaminType, DailyVitaminSummary> {
        val summaryMap = mutableMapOf<VitaminType, MutableList<MealContribution>>()
        val totalsMap = mutableMapOf<VitaminType, Double>()

        // Initialize all 13 types
        for (vType in VitaminType.values()) {
            summaryMap[vType] = mutableListOf()
            totalsMap[vType] = 0.0
        }

        for (meal in meals) {
            val vitamins = meal.getVitamins().ifEmpty {
                estimateVitaminsFromMeal(meal)
            }

            for (vit in vitamins) {
                if (vit.amount <= 0.0) continue
                val curTotal = totalsMap[vit.type] ?: 0.0
                totalsMap[vit.type] = curTotal + vit.amount
                summaryMap[vit.type]?.add(
                    MealContribution(
                        mealName = meal.name.ifBlank { "Mahlzeit" },
                        amount = Math.round(vit.amount * 100.0) / 100.0,
                        unit = vit.unit
                    )
                )
            }
        }

        val result = mutableMapOf<VitaminType, DailyVitaminSummary>()
        for (vType in VitaminType.values()) {
            val total = Math.round((totalsMap[vType] ?: 0.0) * 100.0) / 100.0
            val percent = if (vType.standardRda > 0) ((total / vType.standardRda) * 100).toInt() else 0
            val contributions = summaryMap[vType] ?: emptyList()

            result[vType] = DailyVitaminSummary(
                type = vType,
                totalAmount = total,
                unit = vType.defaultUnit,
                percentRda = percent,
                contributingMeals = contributions
            )
        }

        return result
    }

    /**
     * Heuristic fallback to estimate prominent vitamins if a meal entry has no AI-derived vitamin data.
     */
    fun estimateVitaminsFromMeal(meal: MealEntry): List<VitaminAmount> {
        val text = "${meal.name} ${meal.notes}".lowercase()
        val results = mutableListOf<VitaminAmount>()

        fun addVit(type: VitaminType, amount: Double) {
            if (results.none { it.type == type }) {
                results.add(VitaminAmount(type, amount, type.defaultUnit))
            }
        }

        // Vitamin C (Zitrusfrüchte, Beeren, Paprika, Brokkoli, Tomate, Salat, Apfel, Obst)
        if (text.containsAny("orange", "zitrone", "paprika", "brokkoli", "beere", "erdbeer", "heidelbeer",
                "himbeer", "apfel", "kiwi", "obst", "salat", "grapefruit", "mandarine", "tomate")
        ) {
            addVit(VitaminType.VITAMIN_C, 35.0)
        }

        // Vitamin A (Karotte, Möhre, Süßkartoffel, Spinat, Kürbis, Grünkohl, Ei, Leber)
        if (text.containsAny("karotte", "moehre", "möhre", "suesskartoffel", "süßkartoffel", "spinat",
                "kuerbis", "kürbis", "gruenkohl", "grünkohl", "aprikose")
        ) {
            addVit(VitaminType.VITAMIN_A, 450.0)
        }

        // Vitamin D (Lachs, Forelle, Hering, Thunfisch, Pilze, Champignon, Ei)
        if (text.containsAny("lachs", "forelle", "hering", "thunfisch", "makrele", "pilz", "champignon")
        ) {
            addVit(VitaminType.VITAMIN_D, 6.0)
        }

        // Vitamin E (Nüsse, Mandeln, Walnuss, Sonnenblumen, Avocado, Pflanzenöl, Olivenöl)
        if (text.containsAny("nuss", "nüsse", "nuesse", "mandel", "walnuss", "avocado", "erdnuss", "leinsamen", "olivenoel")
        ) {
            addVit(VitaminType.VITAMIN_E, 5.0)
        }

        // Vitamin K (Spinat, Grünkohl, Brokkoli, Salat, Kohl, Gurke, Petersilie)
        if (text.containsAny("spinat", "gruenkohl", "grünkohl", "brokkoli", "salat", "kohl", "petersilie")
        ) {
            addVit(VitaminType.VITAMIN_K, 50.0)
        }

        // Vitamin B12 (Fleisch, Fisch, Ei, Milch, Joghurt, Quark, Käse)
        if (text.containsAny("fleisch", "rind", "huhn", "haehnchen", "hähnchen", "schwein", "lachs",
                "thunfisch", "fisch", "ei", "eier", "milch", "joghurt", "quark", "kaese", "käse")
        ) {
            addVit(VitaminType.VITAMIN_B12, 1.8)
        }

        // Vitamin B1 & B6 (Vollkorn, Haferflocken, Müsli, Brot, Banane, Reis, Hülsenfrüchte, Linsen, Bohnen)
        if (text.containsAny("hafer", "muesli", "müsli", "vollkorn", "brot", "banane", "linse", "bohne", "kichererbse")
        ) {
            addVit(VitaminType.VITAMIN_B1, 0.4)
            addVit(VitaminType.VITAMIN_B6, 0.5)
        }

        // Vitamin B9 Folsäure (Hülsenfrüchte, Spinat, Salat, Spargel, Vollkorn)
        if (text.containsAny("spinat", "salat", "linse", "bohne", "kichererbse", "spargel", "vollkorn")
        ) {
            addVit(VitaminType.VITAMIN_B9, 80.0)
        }

        // Vitamin B2 (Milch, Joghurt, Quark, Käse, Ei, Pilze)
        if (text.containsAny("milch", "joghurt", "quark", "kaese", "käse", "ei", "eier")
        ) {
            addVit(VitaminType.VITAMIN_B2, 0.4)
        }

        return results
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }
}
