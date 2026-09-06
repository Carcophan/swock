package de.carcophan.swock

import de.carcophan.swock.model.FoodAnalysisResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import kotlin.math.roundToInt

class MealQuantityScalingTest {

    private fun scaleNutrients(
        baseAnalysis: FoodAnalysisResult,
        quantity: Double
    ): Quadruple<Int, Double, Double, Double> {
        val cal = (baseAnalysis.calories * quantity).roundToInt()
        val protein = Math.round(baseAnalysis.proteinGrams * quantity * 10.0) / 10.0
        val carbs = Math.round(baseAnalysis.carbsGrams * quantity * 10.0) / 10.0
        val fat = Math.round(baseAnalysis.fatGrams * quantity * 10.0) / 10.0
        return Quadruple(cal, protein, carbs, fat)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    @Test
    fun testNutrientScaling_doublePortion() {
        val base = FoodAnalysisResult(
            name = "Haferflocken mit Banane",
            calories = 350,
            proteinGrams = 12.0,
            carbsGrams = 58.5,
            fatGrams = 6.2,
            portionDesc = "1 Schüssel"
        )

        val (cal, protein, carbs, fat) = scaleNutrients(base, 2.0)

        assertEquals(700, cal)
        assertEquals(24.0, protein, 0.001)
        assertEquals(117.0, carbs, 0.001)
        assertEquals(12.4, fat, 0.001)
    }

    @Test
    fun testNutrientScaling_halfPortion() {
        val base = FoodAnalysisResult(
            name = "Pizza Margherita",
            calories = 800,
            proteinGrams = 32.0,
            carbsGrams = 94.0,
            fatGrams = 28.0,
            portionDesc = "1 Pizza"
        )

        val (cal, protein, carbs, fat) = scaleNutrients(base, 0.5)

        assertEquals(400, cal)
        assertEquals(16.0, protein, 0.001)
        assertEquals(47.0, carbs, 0.001)
        assertEquals(14.0, fat, 0.001)
    }

    @Test
    fun testNutrientScaling_oneAndHalfPortion() {
        val base = FoodAnalysisResult(
            name = "Rührei mit Brot",
            calories = 300,
            proteinGrams = 18.0,
            carbsGrams = 20.0,
            fatGrams = 14.0,
            portionDesc = "1 Teller"
        )

        val (cal, protein, carbs, fat) = scaleNutrients(base, 1.5)

        assertEquals(450, cal)
        assertEquals(27.0, protein, 0.001)
        assertEquals(30.0, carbs, 0.001)
        assertEquals(21.0, fat, 0.001)
    }
}
