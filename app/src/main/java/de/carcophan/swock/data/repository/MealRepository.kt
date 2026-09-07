package de.carcophan.swock.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import de.carcophan.swock.data.local.MealDatabaseHelper
import de.carcophan.swock.model.MealEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MealRepository(private val context: Context) {

    private val dbHelper = MealDatabaseHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _selectedDateKey = MutableStateFlow(currentDateKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _mealsForSelectedDate = MutableStateFlow<List<MealEntry>>(emptyList())
    val mealsForSelectedDate: StateFlow<List<MealEntry>> = _mealsForSelectedDate.asStateFlow()

    private val _availableDates = MutableStateFlow<List<String>>(emptyList())
    val availableDates: StateFlow<List<String>> = _availableDates.asStateFlow()

    init {
        refreshData()
        scope.launch {
            cleanOrphanedImages()
        }
    }

    fun currentDateKey(): String {
        return dateFormat.format(Date())
    }

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
        refreshData()
    }

    fun refreshData() {
        scope.launch {
            val dateKey = _selectedDateKey.value
            val meals = dbHelper.getMealsForDate(dateKey)
            val dates = dbHelper.getAllDatesWithMeals()
            withContext(Dispatchers.Main) {
                _mealsForSelectedDate.value = meals
                _availableDates.value = dates
            }
        }
    }

    suspend fun saveMeal(meal: MealEntry): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insertMeal(meal)
        refreshData()
        id
    }

    suspend fun deleteMeal(meal: MealEntry) = withContext(Dispatchers.IO) {
        dbHelper.deleteMeal(meal.id)
        deleteImageFilesForMeal(meal.imagePath)
        cleanOrphanedImages()
        refreshData()
    }

    suspend fun deleteMealById(mealId: Long) = withContext(Dispatchers.IO) {
        val meal = dbHelper.getAllMeals().firstOrNull { it.id == mealId }
        if (meal != null) {
            deleteMeal(meal)
        } else {
            dbHelper.deleteMeal(mealId)
            cleanOrphanedImages()
            refreshData()
        }
    }

    suspend fun updateMeal(meal: MealEntry) = withContext(Dispatchers.IO) {
        val existing = dbHelper.getAllMeals().firstOrNull { it.id == meal.id }
        if (existing != null && existing.imagePath.isNotBlank() && existing.imagePath != meal.imagePath) {
            deleteImageFilesForMeal(existing.imagePath)
        }
        dbHelper.updateMeal(meal)
        refreshData()
    }

    /**
     * Thoroughly deletes image file(s) for a given meal image path.
     * Handles absolute paths, file:// prefixes, content:// URIs, filenames in food_images,
     * matching temporary camera images, and evicts Coil cache entries.
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun deleteImageFilesForMeal(imagePath: String) {
        if (imagePath.isBlank()) return
        try {
            val cleanPath = if (imagePath.startsWith("file://")) {
                imagePath.removePrefix("file://")
            } else {
                imagePath
            }

            if (cleanPath.startsWith("content://")) {
                try {
                    context.contentResolver.delete(Uri.parse(cleanPath), null, null)
                } catch (e: Exception) {
                    // Ignore ContentResolver deletion issues
                }
            } else {
                // 1. Delete direct file if exists
                val directFile = File(cleanPath)
                if (directFile.exists()) {
                    directFile.delete()
                }

                // 2. Also check and delete by filename inside food_images directory
                val imagesDir = File(context.filesDir, "food_images")
                val fileName = directFile.name
                if (fileName.isNotBlank()) {
                    val fileInFoodImages = File(imagesDir, fileName)
                    if (fileInFoodImages.exists()) {
                        fileInFoodImages.delete()
                    }
                }

                // 3. Also check if a corresponding camera photo exists in cacheDir/images
                val cacheImagesDir = File(context.cacheDir, "images")
                if (fileName.isNotBlank()) {
                    val fileInCache = File(cacheImagesDir, fileName)
                    if (fileInCache.exists()) {
                        fileInCache.delete()
                    }
                }
            }

            // 4. Invalidate Coil memory and disk cache
            try {
                val imageLoader = coil.Coil.imageLoader(context)
                imageLoader.diskCache?.remove(cleanPath)
                imageLoader.memoryCache?.remove(coil.memory.MemoryCache.Key(cleanPath))
                val directFile = File(cleanPath)
                if (directFile.isAbsolute) {
                    imageLoader.diskCache?.remove(directFile.absolutePath)
                    imageLoader.memoryCache?.remove(coil.memory.MemoryCache.Key(directFile.absolutePath))
                }
            } catch (t: Throwable) {
                // Ignore Coil eviction errors
            }
        } catch (e: Exception) {
            // Non-critical image deletion error
        }
    }

    /**
     * Removes any image file in internal food_images storage that is not referenced
     * by any existing meal in the database.
     */
    suspend fun cleanOrphanedImages(excludePaths: Set<String> = emptySet()) = withContext(Dispatchers.IO) {
        try {
            val allMeals = dbHelper.getAllMeals()
            val referencedPaths = allMeals.mapNotNull { it.imagePath.ifBlank { null } }.toSet()
            val referencedFileNames = referencedPaths.map { File(it).name }.toSet()

            val imagesDir = File(context.filesDir, "food_images")
            if (imagesDir.exists() && imagesDir.isDirectory) {
                imagesDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val isReferenced = referencedFileNames.contains(file.name) ||
                                referencedPaths.contains(file.absolutePath) ||
                                excludePaths.contains(file.absolutePath) ||
                                excludePaths.any { it.endsWith(file.name) }
                        if (!isReferenced) {
                            file.delete()
                        }
                    }
                }
            }

            // Also clean up stale camera photos in cacheDir/images older than 10 minutes
            val cacheImagesDir = File(context.cacheDir, "images")
            if (cacheImagesDir.exists() && cacheImagesDir.isDirectory) {
                val tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000L
                cacheImagesDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith("camera_photo_") && file.lastModified() < tenMinutesAgo) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore non-fatal cleanup errors
        }
    }

    suspend fun getMealsForDateDirect(dateKey: String): List<MealEntry> = withContext(Dispatchers.IO) {
        dbHelper.getMealsForDate(dateKey)
    }

    /**
     * Copies and compresses image to internal app storage, returning its absolute path.
     */
    suspend fun saveImageFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, "food_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        val destination = File(imagesDir, "food_${System.currentTimeMillis()}.jpg")

        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            val original = BitmapFactory.decodeStream(input)
            if (original != null) {
                // Downscale if image is too large to save space and memory
                val maxDim = 1280
                val ratio = Math.min(
                    maxDim.toFloat() / original.width,
                    maxDim.toFloat() / original.height
                )
                val targetW = if (ratio < 1f) (original.width * ratio).toInt() else original.width
                val targetH = if (ratio < 1f) (original.height * ratio).toInt() else original.height
                val scaled = if (ratio < 1f) {
                    Bitmap.createScaledBitmap(original, targetW, targetH, true)
                } else {
                    original
                }

                // Correct EXIF orientation so portrait photos stay upright
                val rotated = if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    }
                    Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
                } else {
                    scaled
                }

                FileOutputStream(destination).use { out ->
                    rotated.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                if (rotated != scaled && rotated != original) {
                    rotated.recycle()
                }
                if (scaled != original) {
                    scaled.recycle()
                }
                original.recycle()
            }
        }

        destination.absolutePath
    }

    fun getDbHelper(): MealDatabaseHelper = dbHelper

    fun closeDatabase() {
        dbHelper.close()
    }
}
