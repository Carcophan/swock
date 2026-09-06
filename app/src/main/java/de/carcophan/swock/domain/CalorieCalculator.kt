package de.carcophan.swock.domain

import de.carcophan.swock.model.Gender
import de.carcophan.swock.model.UserProfile
import kotlin.math.roundToInt

object CalorieCalculator {

    /**
     * Calculates Basal Metabolic Rate (BMR / Grundumsatz) using the Mifflin-St Jeor formula:
     * Men: 10 * weight(kg) + 6.25 * height(cm) - 5 * age + 5
     * Women: 10 * weight(kg) + 6.25 * height(cm) - 5 * age - 161
     */
    fun calculateBmr(profile: UserProfile): Int {
        val base = (10.0 * profile.weightKg) + (6.25 * profile.heightCm) - (5.0 * profile.age)
        val bmr = when (profile.gender) {
            Gender.MALE -> base + 5.0
            Gender.FEMALE -> base - 161.0
        }
        return bmr.coerceAtLeast(500.0).roundToInt()
    }

    /**
     * Calculates Total Daily Energy Expenditure (TDEE / Gesamtumsatz) = BMR * PAL (Aktivitätsfaktor)
     */
    fun calculateTdee(profile: UserProfile): Int {
        val bmr = calculateBmr(profile)
        return (bmr * profile.activityLevel.factor).roundToInt()
    }

    /**
     * Target daily calories including the selected goal adjustment (deficit or surplus).
     */
    fun calculateDailyTarget(profile: UserProfile): Int {
        val tdee = calculateTdee(profile)
        val target = tdee + profile.calorieGoal.calorieAdjustment
        return target.coerceAtLeast(800)
    }

    /**
     * Calculates dynamic daily calorie target:
     * Base target + active calories burned from Health Connect (tracker/smartwatch).
     */
    fun calculateDynamicTarget(profile: UserProfile, activeBurnedCalories: Int): Int {
        val baseTarget = calculateDailyTarget(profile)
        return (baseTarget + activeBurnedCalories.coerceAtLeast(0)).coerceAtLeast(800)
    }

    data class CalorieStatus(
        val consumed: Int,
        val target: Int,
        val difference: Int, // target - consumed (positive = deficit/remaining, negative = surplus/over)
        val isOverBudget: Boolean,
        val percentage: Float
    )

    fun evaluateDay(consumedCalories: Int, targetCalories: Int): CalorieStatus {
        val target = targetCalories.coerceAtLeast(1)
        val diff = target - consumedCalories
        val isOver = diff < 0
        val pct = (consumedCalories.toFloat() / target.toFloat()).coerceIn(0f, 2f)
        return CalorieStatus(
            consumed = consumedCalories,
            target = target,
            difference = diff,
            isOverBudget = isOver,
            percentage = pct
        )
    }
}
