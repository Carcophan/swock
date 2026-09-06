package de.carcophan.swock

import de.carcophan.swock.domain.CalorieCalculator
import de.carcophan.swock.model.ActivityLevel
import de.carcophan.swock.model.CalorieGoal
import de.carcophan.swock.model.Gender
import de.carcophan.swock.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieCalculatorTest {

    @Test
    fun testBmrCalculation_male() {
        // Man: 30 years old, 75 kg, 180 cm
        // BMR = 10 * 75 + 6.25 * 180 - 5 * 30 + 5 = 750 + 1125 - 150 + 5 = 1730
        val profile = UserProfile(
            age = 30,
            gender = Gender.MALE,
            heightCm = 180.0,
            weightKg = 75.0,
            activityLevel = ActivityLevel.SEDENTARY,
            calorieGoal = CalorieGoal.MAINTAIN
        )

        val bmr = CalorieCalculator.calculateBmr(profile)
        assertEquals(1730, bmr)
    }

    @Test
    fun testBmrCalculation_female() {
        // Woman: 28 years old, 60 kg, 165 cm
        // BMR = 10 * 60 + 6.25 * 165 - 5 * 28 - 161 = 600 + 1031.25 - 140 - 161 = 1330.25 -> 1330
        val profile = UserProfile(
            age = 28,
            gender = Gender.FEMALE,
            heightCm = 165.0,
            weightKg = 60.0,
            activityLevel = ActivityLevel.SEDENTARY,
            calorieGoal = CalorieGoal.MAINTAIN
        )

        val bmr = CalorieCalculator.calculateBmr(profile)
        assertEquals(1330, bmr)
    }

    @Test
    fun testTdeeAndTarget_withActivityAndDeficit() {
        // Male, BMR 1730, Activity LIGHT (1.375) -> TDEE = 1730 * 1.375 = 2378.75 -> 2379
        // Calorie Goal DEFICIT_STANDARD (-500) -> 2379 - 500 = 1879
        val profile = UserProfile(
            age = 30,
            gender = Gender.MALE,
            heightCm = 180.0,
            weightKg = 75.0,
            activityLevel = ActivityLevel.LIGHT,
            calorieGoal = CalorieGoal.DEFICIT_STANDARD
        )

        val tdee = CalorieCalculator.calculateTdee(profile)
        assertEquals(2379, tdee)

        val target = CalorieCalculator.calculateDailyTarget(profile)
        assertEquals(1879, target)
    }

    @Test
    fun testEvaluateDay_underBudget() {
        val target = 2000
        val consumed = 1650

        val status = CalorieCalculator.evaluateDay(consumed, target)
        assertFalse(status.isOverBudget)
        assertEquals(350, status.difference)
        assertTrue(status.percentage < 1.0f)
    }

    @Test
    fun testEvaluateDay_overBudget() {
        val target = 2000
        val consumed = 2350

        val status = CalorieCalculator.evaluateDay(consumed, target)
        assertTrue(status.isOverBudget)
        assertEquals(-350, status.difference)
        assertTrue(status.percentage > 1.0f)
    }
}
