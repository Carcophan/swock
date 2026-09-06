package de.carcophan.swock

import de.carcophan.swock.domain.VitaminAggregator
import de.carcophan.swock.model.MealEntry
import de.carcophan.swock.model.VitaminAmount
import de.carcophan.swock.model.VitaminType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VitaminAggregationTest {

    @Test
    fun testVitaminTypeMatching() {
        assertEquals(VitaminType.VITAMIN_C, VitaminType.match("Vitamin C"))
        assertEquals(VitaminType.VITAMIN_C, VitaminType.match("Ascorbinsäure"))
        assertEquals(VitaminType.VITAMIN_A, VitaminType.match("Retinol"))
        assertEquals(VitaminType.VITAMIN_A, VitaminType.match("Beta-Carotin"))
        assertEquals(VitaminType.VITAMIN_B12, VitaminType.match("Vitamin B12"))
        assertEquals(VitaminType.VITAMIN_B12, VitaminType.match("Cobalamin"))
        assertEquals(VitaminType.VITAMIN_B9, VitaminType.match("Folsäure"))
        assertEquals(VitaminType.VITAMIN_B9, VitaminType.match("Folat"))
        assertEquals(VitaminType.VITAMIN_D, VitaminType.match("Vitamin D"))
        assertEquals(VitaminType.VITAMIN_E, VitaminType.match("Tocopherol"))
        assertEquals(VitaminType.VITAMIN_K, VitaminType.match("Phyllochinon"))
    }

    @Test
    fun testSerializationAndDeserialization() {
        val list = listOf(
            VitaminAmount(VitaminType.VITAMIN_C, 65.5, "mg"),
            VitaminAmount(VitaminType.VITAMIN_D, 10.0, "µg"),
            VitaminAmount(VitaminType.VITAMIN_B12, 2.5, "µg")
        )

        val jsonString = VitaminAmount.listToJsonString(list)
        val restored = VitaminAmount.listFromJsonString(jsonString)

        assertEquals(3, restored.size)
        assertEquals(VitaminType.VITAMIN_C, restored[0].type)
        assertEquals(65.5, restored[0].amount, 0.001)
        assertEquals("mg", restored[0].unit)
        assertEquals(VitaminType.VITAMIN_D, restored[1].type)
        assertEquals(10.0, restored[1].amount, 0.001)
        assertEquals(VitaminType.VITAMIN_B12, restored[2].type)
        assertEquals(2.5, restored[2].amount, 0.001)
    }

    @Test
    fun testAggregationAcrossMultipleMeals() {
        val meal1Vitamins = listOf(
            VitaminAmount(VitaminType.VITAMIN_C, 40.0, "mg"),
            VitaminAmount(VitaminType.VITAMIN_A, 300.0, "µg")
        )
        val meal2Vitamins = listOf(
            VitaminAmount(VitaminType.VITAMIN_C, 60.0, "mg"),
            VitaminAmount(VitaminType.VITAMIN_D, 15.0, "µg")
        )

        val meal1 = MealEntry(
            id = 1,
            name = "Orangen-Smoothie",
            calories = 180,
            proteinGrams = 2.0,
            carbsGrams = 40.0,
            fatGrams = 0.5,
            portionDesc = "1 Glas",
            imagePath = "",
            timestamp = 1000L,
            dateKey = "2026-09-06",
            vitaminsJson = VitaminAmount.listToJsonString(meal1Vitamins)
        )

        val meal2 = MealEntry(
            id = 2,
            name = "Lachs mit Brokkoli",
            calories = 520,
            proteinGrams = 42.0,
            carbsGrams = 12.0,
            fatGrams = 32.0,
            portionDesc = "1 Teller",
            imagePath = "",
            timestamp = 2000L,
            dateKey = "2026-09-06",
            vitaminsJson = VitaminAmount.listToJsonString(meal2Vitamins)
        )

        val summaries = VitaminAggregator.aggregateConsumed(listOf(meal1, meal2))

        // Vitamin C should be 40 + 60 = 100 mg (100% RDA of 100mg)
        val vitC = summaries.find { it.type == VitaminType.VITAMIN_C }
        assertNotNull(vitC)
        assertEquals(100.0, vitC!!.totalAmount, 0.001)
        assertEquals(100, vitC.percentRda)
        assertEquals(2, vitC.contributingMeals.size)
        assertEquals("Orangen-Smoothie", vitC.contributingMeals[0].mealName)
        assertEquals(40.0, vitC.contributingMeals[0].amount, 0.001)
        assertEquals("Lachs mit Brokkoli", vitC.contributingMeals[1].mealName)
        assertEquals(60.0, vitC.contributingMeals[1].amount, 0.001)

        // Vitamin A should be 300 µg (33% RDA of 900µg)
        val vitA = summaries.find { it.type == VitaminType.VITAMIN_A }
        assertNotNull(vitA)
        assertEquals(300.0, vitA!!.totalAmount, 0.001)
        assertEquals(33, vitA.percentRda)

        // Vitamin D should be 15 µg (75% RDA of 20µg)
        val vitD = summaries.find { it.type == VitaminType.VITAMIN_D }
        assertNotNull(vitD)
        assertEquals(15.0, vitD!!.totalAmount, 0.001)
        assertEquals(75, vitD.percentRda)
    }

    @Test
    fun testAggregateAllReturns13Vitamins() {
        val meals = emptyList<MealEntry>()
        val allVits = VitaminAggregator.aggregateAll(meals)
        assertEquals(13, allVits.size)
        assertTrue(allVits.all { it.totalAmount == 0.0 })
    }

    @Test
    fun testHeuristicFallbackForMealsWithoutVitamins() {
        val oldMeal = MealEntry(
            id = 10,
            name = "Karottensalat mit Nüssen",
            calories = 250,
            proteinGrams = 5.0,
            carbsGrams = 18.0,
            fatGrams = 16.0,
            portionDesc = "1 Schale",
            imagePath = "",
            timestamp = 1000L,
            dateKey = "2026-09-06",
            vitaminsJson = "[]" // Empty/legacy
        )

        val estimated = VitaminAggregator.estimateVitaminsFromMeal(oldMeal)
        assertTrue("Sollte Vitamin A enthalten", estimated.any { it.type == VitaminType.VITAMIN_A })
        assertTrue("Sollte Vitamin E enthalten", estimated.any { it.type == VitaminType.VITAMIN_E })
    }

    @Test
    fun testVitaminScalingWithQuantity() {
        val baseVitamins = listOf(
            VitaminAmount(VitaminType.VITAMIN_C, 30.0, "mg"),
            VitaminAmount(VitaminType.VITAMIN_B12, 1.2, "µg")
        )

        val scaledDouble = baseVitamins.map { it.copy(amount = Math.round(it.amount * 2.0 * 100.0) / 100.0) }
        assertEquals(60.0, scaledDouble[0].amount, 0.001)
        assertEquals(2.4, scaledDouble[1].amount, 0.001)

        val scaledHalf = baseVitamins.map { it.copy(amount = Math.round(it.amount * 0.5 * 100.0) / 100.0) }
        assertEquals(15.0, scaledHalf[0].amount, 0.001)
        assertEquals(0.6, scaledHalf[1].amount, 0.001)
    }

    @Test
    fun testGeminiResponseParsingWithVitamins() {
        val jsonText = """
            {
              "name": "Frische Erdbeeren mit Quark",
              "calories": 220,
              "proteinGrams": 24.0,
              "carbsGrams": 18.0,
              "fatGrams": 4.0,
              "portionDesc": "250g",
              "notes": "Reich an Protein und Vitamin C",
              "healthRating": "A",
              "healthReason": "Hoher Proteingehalt und frische Früchte",
              "vitamins": [
                {"name": "Vitamin C", "amount": 65.0, "unit": "mg"},
                {"name": "Vitamin B12", "amount": 1.1, "unit": "µg"},
                {"name": "Vitamin B2", "amount": 0.45, "unit": "mg"}
              ]
            }
        """.trimIndent()

        val foodJson = JSONObject(jsonText)
        val vitaminsArray = foodJson.getJSONArray("vitamins")
        val parsedVitamins = mutableListOf<VitaminAmount>()
        for (i in 0 until vitaminsArray.length()) {
            val item = VitaminAmount.fromJsonObject(vitaminsArray.getJSONObject(i))
            if (item != null) parsedVitamins.add(item)
        }

        assertEquals(3, parsedVitamins.size)
        assertEquals(VitaminType.VITAMIN_C, parsedVitamins[0].type)
        assertEquals(65.0, parsedVitamins[0].amount, 0.001)
        assertEquals(VitaminType.VITAMIN_B12, parsedVitamins[1].type)
        assertEquals(1.1, parsedVitamins[1].amount, 0.001)
        assertEquals(VitaminType.VITAMIN_B2, parsedVitamins[2].type)
        assertEquals(0.45, parsedVitamins[2].amount, 0.001)
    }
}
