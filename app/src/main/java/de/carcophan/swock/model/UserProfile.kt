package de.carcophan.swock.model

enum class Gender(val displayName: String) {
    MALE("Männlich"),
    FEMALE("Weiblich")
}

enum class ActivityLevel(val displayName: String, val factor: Double, val description: String) {
    SEDENTARY("Sitzend", 1.2, "Bürojob / kaum Bewegung"),
    LIGHT("Leicht aktiv", 1.375, "Leichte Bewegung, 1–3x Sport/Woche"),
    MODERATE("Mäßig aktiv", 1.55, "Moderate Bewegung, 3–5x Sport/Woche"),
    VERY_ACTIVE("Sehr aktiv", 1.725, "Körperlich fordernd, 6–7x Sport/Woche"),
    EXTRA_ACTIVE("Extrem aktiv", 1.9, "Schwere körperliche Arbeit / Leistungssport")
}

enum class CalorieGoal(val displayName: String, val calorieAdjustment: Int) {
    MAINTAIN("Gewicht halten (100% Gesamtumsatz)", 0),
    DEFICIT_LIGHT("Leichtes Defizit (-300 kcal)", -300),
    DEFICIT_STANDARD("Standard Defizit (-500 kcal)", -500),
    SURPLUS_LIGHT("Leichter Überschuss (+300 kcal)", 300),
    SURPLUS_STRONG("Aufbau-Überschuss (+500 kcal)", 500)
}

data class UserProfile(
    val age: Int = 30,
    val gender: Gender = Gender.MALE,
    val heightCm: Double = 178.0,
    val weightKg: Double = 75.0,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
    val calorieGoal: CalorieGoal = CalorieGoal.MAINTAIN,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-3.6-flash",
    val syncHealthConnect: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val backupIntervalDays: Int = 1,
    val backupWifiOnly: Boolean = true,
    val lastBackupTimestamp: Long = 0L,
    val googleAccountEmail: String = ""
)
