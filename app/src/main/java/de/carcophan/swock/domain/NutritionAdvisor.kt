package de.carcophan.swock.domain

import de.carcophan.swock.model.AvoidanceItem
import de.carcophan.swock.model.AvoidanceSeverity
import de.carcophan.swock.model.DailyNutritionAdvice
import de.carcophan.swock.model.HealthRating
import de.carcophan.swock.model.MealEntry
import de.carcophan.swock.model.RecommendationCategory
import de.carcophan.swock.model.RecommendationItem
import de.carcophan.swock.model.UserProfile
import de.carcophan.swock.model.VitaminType
import kotlin.math.roundToInt

object NutritionAdvisor {

    fun generateLocalAdvice(
        meals: List<MealEntry>,
        profile: UserProfile,
        targetCalories: Int
    ): DailyNutritionAdvice {
        val totalConsumed = meals.sumOf { it.calories }
        val totalProtein = meals.sumOf { it.proteinGrams }
        val totalCarbs = meals.sumOf { it.carbsGrams }
        val totalFat = meals.sumOf { it.fatGrams }
        val remainingCalories = targetCalories - totalConsumed

        val targetProtein = (profile.weightKg * 1.6).coerceIn(60.0, 220.0)
        val targetFatMax = (profile.weightKg * 1.0).coerceIn(45.0, 110.0)

        // When no meals are logged yet
        if (meals.isEmpty()) {
            return DailyNutritionAdvice(
                balanceScore = "Tag noch unberührt",
                summaryText = "Du hast heute noch keine Mahlzeit erfasst. Starte mit einer ausgewogenen Basis in den Tag!",
                recommendations = listOf(
                    RecommendationItem(
                        title = "Morgendliche Hydration",
                        items = listOf("Großes Glas lauwarmes Wasser", "Wasser mit Zitrone", "Ungesüßter Kräuter- oder Grüntee"),
                        reason = "Bringt den Stoffwechsel in Schwung und gleicht nächtlichen Flüssigkeitsverlust aus.",
                        category = RecommendationCategory.DRINK,
                        nutrientFocus = "Flüssigkeit"
                    ),
                    RecommendationItem(
                        title = "Ausgewogenes Frühstück / erste Mahlzeit",
                        items = listOf("Haferflocken mit Beeren & Magerquark/Nüssen", "Vollkornbrot mit Ei & Tomaten", "Gemüse-Omelett"),
                        reason = "Liefert langanhaltende Energie, Ballaststoffe und hochwertige Proteine.",
                        category = RecommendationCategory.FOOD,
                        nutrientFocus = "Protein & Ballaststoffe"
                    )
                ),
                avoidances = listOf(
                    AvoidanceItem(
                        title = "Zuckerhaltige Getränke & süße Snacks auf nüchternen Magen",
                        items = listOf("Softdrinks", "Gezuckerte Fruchtsäfte", "Schoko-Croissants / Donuts"),
                        reason = "Führen zu extremen Blutzuckerspitzen und vorzeitigen Heißhungerattacken.",
                        severity = AvoidanceSeverity.MODERATE
                    )
                ),
                isAiGenerated = false
            )
        }

        val recList = mutableListOf<RecommendationItem>()
        val avoidList = mutableListOf<AvoidanceItem>()

        // 1. Protein Analysis
        val proteinDeficit = targetProtein - totalProtein
        if (proteinDeficit > 25.0) {
            recList.add(
                RecommendationItem(
                    title = "Proteinspeicher auffüllen",
                    items = listOf(
                        "Magerquark mit Beeren",
                        "Körniger Frischkäse mit Kräutern",
                        "Gegrilltes Hähnchenbrustfilet oder Tofu",
                        "Linsen- oder Kichererbsensalat"
                    ),
                    reason = "Bisher hast du ${totalProtein.roundToInt()}g von deinem Richtwert (~${targetProtein.roundToInt()}g) aufgenommen. Eiweiß schützt deine Muskulatur und sorgt für anhaltende Sättigung.",
                    category = RecommendationCategory.FOOD,
                    nutrientFocus = "Eiweiß"
                )
            )
        }

        // 2. Fat Analysis
        val fatCalories = totalFat * 9.0
        val fatPercentage = if (totalConsumed > 0) (fatCalories / totalConsumed * 100).roundToInt() else 0
        if (totalFat > targetFatMax || fatPercentage > 38) {
            avoidList.add(
                AvoidanceItem(
                    title = "Fett- und ölreiche Speisen meiden",
                    items = listOf(
                        "Frittiertes Fast Food (Pommes, Burger)",
                        "Fettreicher Käse & Saucen (Sahnesauce, Mayonnaise)",
                        "Fettiges Fleisch (Wurst, Speck)",
                        "Chips & Blätterteiggebäck"
                    ),
                    reason = "Dein Fettanteil liegt heute bereits bei ${totalFat.roundToInt()}g ($fatPercentage% deiner Kalorien). Um die Tagesbalance zu wahren, sollten verbleibende Speisen besonders fettarm sein.",
                    severity = AvoidanceSeverity.HIGH
                )
            )
        } else if (totalFat < profile.weightKg * 0.4 && remainingCalories > 300) {
            recList.add(
                RecommendationItem(
                    title = "Gesunde ungesättigte Fettsäuren",
                    items = listOf("Eine kleine Handvoll Walnüsse oder Mandeln", "Ein Spritzer Leinöl oder Olivenöl", "1/4 Avocado"),
                    reason = "Wertvolle Omega-3- und Omega-6-Fettsäuren unterstützen Herz, Gehirn und die Aufnahme fettlöslicher Vitamine.",
                    category = RecommendationCategory.FOOD,
                    nutrientFocus = "Gesunde Fette"
                )
            )
        }

        // 3. Simple Carbohydrates / Sugar / Fastfood check
        val poorMealsCount = meals.count { it.healthRating == HealthRating.E || it.healthRating == HealthRating.F }
        if (poorMealsCount > 0 || totalCarbs > (targetCalories * 0.65 / 4.0)) {
            avoidList.add(
                AvoidanceItem(
                    title = "Einfache Zucker & Weißmehl reduzieren",
                    items = listOf(
                        "Süßigkeiten, Schokolade & Riegel",
                        "Gebäck aus weißem Weizenmehl",
                        "Gesüßte Kaltgetränke & Frappes"
                    ),
                    reason = "Du hast heute bereits eine oder mehrere kalorien-/zuckerreichere Speisen genossen. Entlaste deinen Stoffwechsel und vermeide weitere schnelle Zucker.",
                    severity = AvoidanceSeverity.MODERATE
                )
            )
        }

        // 4. Calorie Budget Remaining Check
        if (remainingCalories <= 150) {
            avoidList.add(
                AvoidanceItem(
                    title = "Schwere & energiereiche Mahlzeiten meiden",
                    items = listOf("Große Hauptgerichte", "Nudel- und Reisberge", "Kalorienreiche Desserts"),
                    reason = "Dein Kalorienziel für heute ist bereits so gut wie erreicht ($totalConsumed / $targetCalories kcal).",
                    severity = AvoidanceSeverity.HIGH
                )
            )
            recList.add(
                RecommendationItem(
                    title = "Kalorienarme Füllstoffe & Volumen",
                    items = listOf(
                        "Gurken-, Sellerie- oder Karottensticks",
                        "Großer bunter Blattsalat mit Zitronen-Kräuter-Dressing",
                        "Warme Gemüsebrühe"
                    ),
                    reason = "Hohes Volumen bei minimalen Kalorien, um Sättigung ohne Kalorienüberschuss zu gewährleisten.",
                    category = RecommendationCategory.FOOD,
                    nutrientFocus = "Ballaststoffe & Volumen"
                )
            )
        } else if (remainingCalories >= 500) {
            recList.add(
                RecommendationItem(
                    title = "Ausgewogene Hauptmahlzeit einplanen",
                    items = listOf(
                        "Gemüsepfanne mit Naturreis und Putenbrust/Tofu",
                        "Gedünsteter Lachs mit Brokkoli und Salzkartoffeln",
                        "Quinoa-Bowl mit frischem Gemüse und Ei"
                    ),
                    reason = "Du hast noch $remainingCalories kcal Puffer für eine nährstoffreiche, vollwertige Speise.",
                    category = RecommendationCategory.FOOD,
                    nutrientFocus = "Vollwertige Nährstoffe"
                )
            )
        }

        // 5. Vitamin Deficiencies Check (from VitaminAggregator)
        val vitaminSummaries = VitaminAggregator.aggregateAll(meals)
        val missingVitamins = vitaminSummaries.filter { it.percentRda < 35 }

        val missingVitC = missingVitamins.any { it.type == VitaminType.VITAMIN_C }
        val missingVitA = missingVitamins.any { it.type == VitaminType.VITAMIN_A }
        val missingVitD = missingVitamins.any { it.type == VitaminType.VITAMIN_D }
        val missingVitE = missingVitamins.any { it.type == VitaminType.VITAMIN_E }

        if (missingVitC) {
            recList.add(
                RecommendationItem(
                    title = "Vitamin C Booster",
                    items = listOf("Rote Paprika (roh)", "Orangen, Mandarinen oder Grapefruit", "Kiwi oder Beeren", "Gedünsteter Brokkoli"),
                    reason = "Dein Vitamin C Bedarf ist heute noch gering abgedeckt. Vitamin C schützt Zellen und stärkt das Immunsystem.",
                    category = RecommendationCategory.FOOD,
                    nutrientFocus = "Vitamin C"
                )
            )
        } else if (missingVitA) {
            recList.add(
                RecommendationItem(
                    title = "Vitamin A / Beta-Carotin Lieferanten",
                    items = listOf("Knackige Karotten mit etwas Olivenöl", "Süßkartoffel", "Gedünsteter Spinat"),
                    reason = "Wichtig für Sehkraft, Schleimhäute und gesunde Hauterneuerung.",
                    category = RecommendationCategory.FOOD,
                    nutrientFocus = "Vitamin A"
                )
            )
        } else if (missingVitE) {
            recList.add(
                RecommendationItem(
                    title = "Vitamin E Zellschutz",
                    items = listOf("Mandeln oder Sonnenblumenkerne", "Haselnüsse", "Hochwertiges Pflanzenöl"),
                    reason = "Wirkt als kraftvolles Antioxidans zum Schutz der Körperzellen vor oxidativem Stress.",
                    category = RecommendationCategory.FOOD,
                    nutrientFocus = "Vitamin E"
                )
            )
        }

        // 6. Drinks & Hydration (Always essential)
        recList.add(
            RecommendationItem(
                title = "Flüssigkeitshaushalt auffüllen",
                items = listOf("Stilles Mineralwasser", "Wasser mit frischer Zitrone & Minze", "Ungesüßter Pfefferminz- oder Kamillentee"),
                reason = "Wasser transportiert Nährstoffe, verhindert Verwechslung von Hunger und Durst und entlastet die Nieren.",
                category = RecommendationCategory.DRINK,
                nutrientFocus = "Hydration"
            )
        )

        avoidList.add(
            AvoidanceItem(
                title = "Flüssige Kalorien & Alkohol",
                items = listOf("Zuckerhaltige Limonaden", "Bier, Wein & Cocktails", "Energy-Drinks", "Fertige Fruchtsaftgetränke"),
                reason = "Flüssige Kalorien machen nicht satt, belasten den Blutzuckerspiegel und die Leber und sabotieren deine Tagesbilanz.",
                severity = AvoidanceSeverity.MODERATE
            )
        )

        // Generate dynamic summary score
        val balanceScore = when {
            totalConsumed > targetCalories + 150 -> "Kalorienüberschuss"
            fatPercentage > 38 -> "Fettanteil dominant"
            proteinDeficit > 30 -> "Proteindefizit"
            remainingCalories in 0..300 -> "Fast im Zielbereich"
            else -> "Auf gutem Kurs"
        }

        val summaryText = when {
            totalConsumed > targetCalories -> "Du liegst heute bei $totalConsumed kcal (Ziel: $targetCalories kcal). Setze für den Rest des Tages auf kalorienarme, wasserreiche Nahrungsmittel."
            fatPercentage > 38 -> "Dein bisheriger Fettanteil ist mit $fatPercentage% hoch. Ergänze heute vor allem magere Proteine, frisches Gemüse und viel Wasser."
            proteinDeficit > 30 -> "Du hast noch rund $remainingCalories kcal Spielraum. Nutze diesen gezielt für proteinreiche Lebensmittel und Vitamine."
            else -> "Deine bisherige Ernährung ist solide. Mit ein paar gezielten Nährstoffen rundest du deinen Tag optimal ab."
        }

        return DailyNutritionAdvice(
            balanceScore = balanceScore,
            summaryText = summaryText,
            recommendations = recList.distinctBy { it.title }.take(4),
            avoidances = avoidList.distinctBy { it.title }.take(3),
            isAiGenerated = false
        )
    }
}
