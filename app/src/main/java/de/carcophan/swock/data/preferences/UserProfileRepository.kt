package de.carcophan.swock.data.preferences

import android.content.Context
import de.carcophan.swock.model.ActivityLevel
import de.carcophan.swock.model.CalorieGoal
import de.carcophan.swock.model.Gender
import de.carcophan.swock.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserProfileRepository(context: Context) {

    private val prefs = context.getSharedPreferences("swock_user_profile", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val age = prefs.getInt(KEY_AGE, 30)
        val genderStr = prefs.getString(KEY_GENDER, Gender.MALE.name) ?: Gender.MALE.name
        val gender = try {
            Gender.valueOf(genderStr)
        } catch (e: Exception) {
            Gender.MALE
        }

        val height = prefs.getFloat(KEY_HEIGHT, 178f).toDouble()
        val weight = prefs.getFloat(KEY_WEIGHT, 75f).toDouble()

        val activityStr = prefs.getString(KEY_ACTIVITY, ActivityLevel.LIGHT.name) ?: ActivityLevel.LIGHT.name
        val activity = try {
            ActivityLevel.valueOf(activityStr)
        } catch (e: Exception) {
            ActivityLevel.LIGHT
        }

        val goalStr = prefs.getString(KEY_GOAL, CalorieGoal.MAINTAIN.name) ?: CalorieGoal.MAINTAIN.name
        val goal = try {
            CalorieGoal.valueOf(goalStr)
        } catch (e: Exception) {
            CalorieGoal.MAINTAIN
        }

        val apiKey = prefs.getString(KEY_GEMINI_KEY, "") ?: ""
        val rawModel = prefs.getString(KEY_GEMINI_MODEL, "gemini-3.8-flash") ?: "gemini-3.8-flash"
        val model = if (rawModel != "gemini-3.8-flash") "gemini-3.8-flash" else rawModel
        val syncHealthConnect = prefs.getBoolean(KEY_HEALTH_CONNECT, false)

        val autoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
        val backupIntervalDays = prefs.getInt(KEY_BACKUP_INTERVAL_DAYS, 1)
        val backupWifiOnly = prefs.getBoolean(KEY_BACKUP_WIFI_ONLY, true)
        val lastBackupTimestamp = prefs.getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L)
        val googleAccountEmail = prefs.getString(KEY_GOOGLE_ACCOUNT_EMAIL, "") ?: ""

        val showVitaminOverview = prefs.getBoolean(KEY_SHOW_VITAMIN_OVERVIEW, true)
        val showNutritionBalance = prefs.getBoolean(KEY_SHOW_NUTRITION_BALANCE, true)
        val showRecommendations = prefs.getBoolean(KEY_SHOW_RECOMMENDATIONS, true)
        val showAvoidances = prefs.getBoolean(KEY_SHOW_AVOIDANCES, true)

        return UserProfile(
            age = age,
            gender = gender,
            heightCm = height,
            weightKg = weight,
            activityLevel = activity,
            calorieGoal = goal,
            geminiApiKey = apiKey,
            geminiModel = model,
            syncHealthConnect = syncHealthConnect,
            autoBackupEnabled = autoBackupEnabled,
            backupIntervalDays = backupIntervalDays,
            backupWifiOnly = backupWifiOnly,
            lastBackupTimestamp = lastBackupTimestamp,
            googleAccountEmail = googleAccountEmail,
            showVitaminOverview = showVitaminOverview,
            showNutritionBalance = showNutritionBalance,
            showRecommendations = showRecommendations,
            showAvoidances = showAvoidances
        )
    }

    fun updateProfile(newProfile: UserProfile) {
        prefs.edit()
            .putInt(KEY_AGE, newProfile.age)
            .putString(KEY_GENDER, newProfile.gender.name)
            .putFloat(KEY_HEIGHT, newProfile.heightCm.toFloat())
            .putFloat(KEY_WEIGHT, newProfile.weightKg.toFloat())
            .putString(KEY_ACTIVITY, newProfile.activityLevel.name)
            .putString(KEY_GOAL, newProfile.calorieGoal.name)
            .putString(KEY_GEMINI_KEY, newProfile.geminiApiKey)
            .putString(KEY_GEMINI_MODEL, newProfile.geminiModel)
            .putBoolean(KEY_HEALTH_CONNECT, newProfile.syncHealthConnect)
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, newProfile.autoBackupEnabled)
            .putInt(KEY_BACKUP_INTERVAL_DAYS, newProfile.backupIntervalDays)
            .putBoolean(KEY_BACKUP_WIFI_ONLY, newProfile.backupWifiOnly)
            .putLong(KEY_LAST_BACKUP_TIMESTAMP, newProfile.lastBackupTimestamp)
            .putString(KEY_GOOGLE_ACCOUNT_EMAIL, newProfile.googleAccountEmail)
            .putBoolean(KEY_SHOW_VITAMIN_OVERVIEW, newProfile.showVitaminOverview)
            .putBoolean(KEY_SHOW_NUTRITION_BALANCE, newProfile.showNutritionBalance)
            .putBoolean(KEY_SHOW_RECOMMENDATIONS, newProfile.showRecommendations)
            .putBoolean(KEY_SHOW_AVOIDANCES, newProfile.showAvoidances)
            .apply()

        _profile.value = newProfile
    }

    fun updateLastBackupTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP_TIMESTAMP, timestamp).apply()
        _profile.value = _profile.value.copy(lastBackupTimestamp = timestamp)
    }

    fun updateGoogleAccountEmail(email: String) {
        prefs.edit().putString(KEY_GOOGLE_ACCOUNT_EMAIL, email).apply()
        _profile.value = _profile.value.copy(googleAccountEmail = email)
    }

    companion object {
        private const val KEY_AGE = "pref_age"
        private const val KEY_GENDER = "pref_gender"
        private const val KEY_HEIGHT = "pref_height"
        private const val KEY_WEIGHT = "pref_weight"
        private const val KEY_ACTIVITY = "pref_activity"
        private const val KEY_GOAL = "pref_goal"
        private const val KEY_GEMINI_KEY = "pref_gemini_key"
        private const val KEY_GEMINI_MODEL = "pref_gemini_model"
        private const val KEY_HEALTH_CONNECT = "pref_health_connect"

        private const val KEY_AUTO_BACKUP_ENABLED = "pref_auto_backup_enabled"
        private const val KEY_BACKUP_INTERVAL_DAYS = "pref_backup_interval_days"
        private const val KEY_BACKUP_WIFI_ONLY = "pref_backup_wifi_only"
        private const val KEY_LAST_BACKUP_TIMESTAMP = "pref_last_backup_timestamp"
        private const val KEY_GOOGLE_ACCOUNT_EMAIL = "pref_google_account_email"

        private const val KEY_SHOW_VITAMIN_OVERVIEW = "pref_show_vitamin_overview"
        private const val KEY_SHOW_NUTRITION_BALANCE = "pref_show_nutrition_balance"
        private const val KEY_SHOW_RECOMMENDATIONS = "pref_show_recommendations"
        private const val KEY_SHOW_AVOIDANCES = "pref_show_avoidances"
    }
}
