package de.carcophan.swock.data.backup

import android.content.Context
import de.carcophan.swock.data.local.MealDatabaseHelper
import de.carcophan.swock.data.preferences.UserProfileRepository
import de.carcophan.swock.data.repository.MealRepository
import de.carcophan.swock.model.ActivityLevel
import de.carcophan.swock.model.CalorieGoal
import de.carcophan.swock.model.Gender
import de.carcophan.swock.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupMetadata(
    val version: Int = 1,
    val appVersion: String = "1.0",
    val createdAt: Long = System.currentTimeMillis(),
    val mealCount: Int = 0,
    val imageCount: Int = 0,
    val appId: String = "de.carcophan.swock"
)

data class BackupInfo(
    val file: File,
    val metadata: BackupMetadata,
    val sizeBytes: Long
)

data class RestoreSummary(
    val mealCount: Int,
    val imageCount: Int,
    val createdAt: Long
)

class BackupManager(
    private val context: Context,
    private val mealRepository: MealRepository,
    private val profileRepository: UserProfileRepository
) {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun generateBackupFileName(): String {
        return "swock_backup_${dateFormat.format(Date())}.zip"
    }

    /**
     * Packages the complete app data (SQLite DB, all food photos, user profile preferences)
     * into a single ZIP archive.
     */
    suspend fun createBackupZip(outputZipFile: File): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            // 1. Force SQLite WAL checkpoint so DB file is up to date
            mealRepository.getDbHelper().checkpoint()

            // 2. Clean orphaned images so backup only includes images of active meals
            mealRepository.cleanOrphanedImages()

            val dbFile = context.getDatabasePath(MealDatabaseHelper.DATABASE_NAME)
            val imagesDir = File(context.filesDir, "food_images")
            val imageFiles = if (imagesDir.exists()) {
                imagesDir.listFiles { f -> f.isFile } ?: emptyArray()
            } else {
                emptyArray()
            }

            val allMeals = mealRepository.getDbHelper().getAllMeals()
            val profile = profileRepository.profile.value

            val metadata = BackupMetadata(
                version = 1,
                appVersion = "1.0",
                createdAt = System.currentTimeMillis(),
                mealCount = allMeals.size,
                imageCount = imageFiles.size,
                appId = context.packageName
            )

            if (outputZipFile.parentFile?.exists() == false) {
                outputZipFile.parentFile?.mkdirs()
            }

            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
                // Write metadata.json
                val metaJson = JSONObject().apply {
                    put("version", metadata.version)
                    put("appVersion", metadata.appVersion)
                    put("createdAt", metadata.createdAt)
                    put("mealCount", metadata.mealCount)
                    put("imageCount", metadata.imageCount)
                    put("appId", metadata.appId)
                }.toString(2)
                writeZipEntry(zos, "metadata.json", metaJson.toByteArray(Charsets.UTF_8))

                // Write user_profile.json
                val profileJson = JSONObject().apply {
                    put("age", profile.age)
                    put("gender", profile.gender.name)
                    put("heightCm", profile.heightCm)
                    put("weightKg", profile.weightKg)
                    put("activityLevel", profile.activityLevel.name)
                    put("calorieGoal", profile.calorieGoal.name)
                    put("geminiApiKey", profile.geminiApiKey)
                    put("geminiModel", profile.geminiModel)
                    put("syncHealthConnect", profile.syncHealthConnect)
                    put("showVitaminOverview", profile.showVitaminOverview)
                    put("showNutritionBalance", profile.showNutritionBalance)
                    put("showRecommendations", profile.showRecommendations)
                    put("showAvoidances", profile.showAvoidances)
                }.toString(2)
                writeZipEntry(zos, "preferences/user_profile.json", profileJson.toByteArray(Charsets.UTF_8))

                // Write SQLite Database
                if (dbFile.exists()) {
                    writeFileToZip(zos, "database/swock_meals.db", dbFile)
                }

                // Write all food images
                for (img in imageFiles) {
                    writeFileToZip(zos, "food_images/${img.name}", img)
                }
            }

            Result.success(BackupInfo(outputZipFile, metadata, outputZipFile.length()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores database, photos, and preferences from a backup ZIP archive.
     */
    suspend fun restoreFromBackupZip(zipFile: File): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        val tempExtractDir = File(context.cacheDir, "swock_restore_${System.currentTimeMillis()}")
        try {
            if (!zipFile.exists() || zipFile.length() == 0L) {
                return@withContext Result.failure(IllegalArgumentException("Backup-Datei existiert nicht oder ist leer."))
            }

            if (!tempExtractDir.exists()) {
                tempExtractDir.mkdirs()
            }

            // 1. Unpack ZIP
            unzipFile(zipFile, tempExtractDir)

            // 2. Validate extracted contents
            val extractedDb = File(tempExtractDir, "database/swock_meals.db")
            if (!extractedDb.exists()) {
                return@withContext Result.failure(IllegalStateException("Ungültiges Backup-Archiv: Datenbank nicht gefunden."))
            }

            // 3. Read metadata if present
            val metaFile = File(tempExtractDir, "metadata.json")
            var mealCount = 0
            var imageCount = 0
            var createdAt = System.currentTimeMillis()
            if (metaFile.exists()) {
                try {
                    val metaObj = JSONObject(metaFile.readText(Charsets.UTF_8))
                    mealCount = metaObj.optInt("mealCount", 0)
                    imageCount = metaObj.optInt("imageCount", 0)
                    createdAt = metaObj.optLong("createdAt", System.currentTimeMillis())
                } catch (e: Exception) {
                    // Non-critical metadata read error
                }
            }

            // 4. Safely close existing database connection
            mealRepository.closeDatabase()

            // 5. Replace SQLite DB file
            val currentDb = context.getDatabasePath(MealDatabaseHelper.DATABASE_NAME)
            if (currentDb.parentFile?.exists() == false) {
                currentDb.parentFile?.mkdirs()
            }

            // Delete stale WAL / SHM files if any
            File(currentDb.parentFile, "${MealDatabaseHelper.DATABASE_NAME}-wal").delete()
            File(currentDb.parentFile, "${MealDatabaseHelper.DATABASE_NAME}-shm").delete()
            File(currentDb.parentFile, "${MealDatabaseHelper.DATABASE_NAME}-journal").delete()

            extractedDb.copyTo(currentDb, overwrite = true)

            // 6. Restore images into internal storage
            val targetImagesDir = File(context.filesDir, "food_images")
            if (!targetImagesDir.exists()) {
                targetImagesDir.mkdirs()
            }

            val extractedImagesDir = File(tempExtractDir, "food_images")
            if (extractedImagesDir.exists()) {
                extractedImagesDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.copyTo(File(targetImagesDir, file.name), overwrite = true)
                    }
                }
            }

            // 7. Remap image paths in SQLite so all meals point to targetImagesDir
            mealRepository.getDbHelper().remapImagePaths(targetImagesDir)

            // 8. Restore user profile if present
            val profileFile = File(tempExtractDir, "preferences/user_profile.json")
            if (profileFile.exists()) {
                try {
                    val pJson = JSONObject(profileFile.readText(Charsets.UTF_8))
                    val currentProf = profileRepository.profile.value
                    val restoredProfile = currentProf.copy(
                        age = pJson.optInt("age", currentProf.age),
                        gender = try { Gender.valueOf(pJson.optString("gender")) } catch (e: Exception) { currentProf.gender },
                        heightCm = pJson.optDouble("heightCm", currentProf.heightCm),
                        weightKg = pJson.optDouble("weightKg", currentProf.weightKg),
                        activityLevel = try { ActivityLevel.valueOf(pJson.optString("activityLevel")) } catch (e: Exception) { currentProf.activityLevel },
                        calorieGoal = try { CalorieGoal.valueOf(pJson.optString("calorieGoal")) } catch (e: Exception) { currentProf.calorieGoal },
                        geminiApiKey = pJson.optString("geminiApiKey", currentProf.geminiApiKey),
                        geminiModel = pJson.optString("geminiModel", currentProf.geminiModel),
                        syncHealthConnect = pJson.optBoolean("syncHealthConnect", currentProf.syncHealthConnect),
                        showVitaminOverview = pJson.optBoolean("showVitaminOverview", currentProf.showVitaminOverview),
                        showNutritionBalance = pJson.optBoolean("showNutritionBalance", currentProf.showNutritionBalance),
                        showRecommendations = pJson.optBoolean("showRecommendations", currentProf.showRecommendations),
                        showAvoidances = pJson.optBoolean("showAvoidances", currentProf.showAvoidances)
                    )
                    profileRepository.updateProfile(restoredProfile)
                } catch (e: Exception) {
                    // Profile restore failure is non-fatal
                }
            }

            // 9. Refresh repository and UI
            mealRepository.refreshData()

            // Fetch actual restored count
            val finalMeals = mealRepository.getDbHelper().getAllMeals()
            val actualMealCount = if (finalMeals.isNotEmpty()) finalMeals.size else mealCount
            val actualImageCount = targetImagesDir.listFiles()?.size ?: imageCount

            Result.success(RestoreSummary(actualMealCount, actualImageCount, createdAt))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempExtractDir.deleteRecursively()
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, data: ByteArray) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        zos.write(data)
        zos.closeEntry()
    }

    private fun writeFileToZip(zos: ZipOutputStream, entryName: String, file: File) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        BufferedInputStream(FileInputStream(file)).use { input ->
            input.copyTo(zos)
        }
        zos.closeEntry()
    }

    private fun unzipFile(zipFile: File, outputDir: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val newFile = File(outputDir, entry.name)
                // Security check against zip slip
                if (!newFile.canonicalPath.startsWith(outputDir.canonicalPath)) {
                    throw SecurityException("Zip Slip Vulnerability erkannt: ${entry.name}")
                }
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(newFile)).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
