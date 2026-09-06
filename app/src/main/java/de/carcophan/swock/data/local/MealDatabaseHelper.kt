package de.carcophan.swock.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import de.carcophan.swock.model.MealEntry

class MealDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "swock_meals.db"
        const val DATABASE_VERSION = 1

        const val TABLE_MEALS = "meals"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_CALORIES = "calories"
        const val COL_PROTEIN = "protein_grams"
        const val COL_CARBS = "carbs_grams"
        const val COL_FAT = "fat_grams"
        const val COL_PORTION = "portion_desc"
        const val COL_IMAGE_PATH = "image_path"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_DATE_KEY = "date_key"
        const val COL_NOTES = "notes"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createQuery = """
            CREATE TABLE $TABLE_MEALS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_CALORIES INTEGER NOT NULL,
                $COL_PROTEIN REAL NOT NULL,
                $COL_CARBS REAL NOT NULL,
                $COL_FAT REAL NOT NULL,
                $COL_PORTION TEXT NOT NULL,
                $COL_IMAGE_PATH TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_DATE_KEY TEXT NOT NULL,
                $COL_NOTES TEXT
            )
        """.trimIndent()
        db.execSQL(createQuery)
        db.execSQL("CREATE INDEX idx_meals_date ON $TABLE_MEALS ($COL_DATE_KEY)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration logic if schema changes
    }

    fun insertMeal(meal: MealEntry): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, meal.name)
            put(COL_CALORIES, meal.calories)
            put(COL_PROTEIN, meal.proteinGrams)
            put(COL_CARBS, meal.carbsGrams)
            put(COL_FAT, meal.fatGrams)
            put(COL_PORTION, meal.portionDesc)
            put(COL_IMAGE_PATH, meal.imagePath)
            put(COL_TIMESTAMP, meal.timestamp)
            put(COL_DATE_KEY, meal.dateKey)
            put(COL_NOTES, meal.notes)
        }
        return db.insert(TABLE_MEALS, null, values)
    }

    fun deleteMeal(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_MEALS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun updateMeal(meal: MealEntry): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, meal.name)
            put(COL_CALORIES, meal.calories)
            put(COL_PROTEIN, meal.proteinGrams)
            put(COL_CARBS, meal.carbsGrams)
            put(COL_FAT, meal.fatGrams)
            put(COL_PORTION, meal.portionDesc)
            put(COL_IMAGE_PATH, meal.imagePath)
            put(COL_TIMESTAMP, meal.timestamp)
            put(COL_DATE_KEY, meal.dateKey)
            put(COL_NOTES, meal.notes)
        }
        return db.update(TABLE_MEALS, values, "$COL_ID = ?", arrayOf(meal.id.toString()))
    }

    fun getMealsForDate(dateKey: String): List<MealEntry> {
        val list = mutableListOf<MealEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEALS,
            null,
            "$COL_DATE_KEY = ?",
            arrayOf(dateKey),
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )
        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(COL_ID)
            val nameIndex = c.getColumnIndexOrThrow(COL_NAME)
            val caloriesIndex = c.getColumnIndexOrThrow(COL_CALORIES)
            val proteinIndex = c.getColumnIndexOrThrow(COL_PROTEIN)
            val carbsIndex = c.getColumnIndexOrThrow(COL_CARBS)
            val fatIndex = c.getColumnIndexOrThrow(COL_FAT)
            val portionIndex = c.getColumnIndexOrThrow(COL_PORTION)
            val imagePathIndex = c.getColumnIndexOrThrow(COL_IMAGE_PATH)
            val timestampIndex = c.getColumnIndexOrThrow(COL_TIMESTAMP)
            val dateKeyIndex = c.getColumnIndexOrThrow(COL_DATE_KEY)
            val notesIndex = c.getColumnIndexOrThrow(COL_NOTES)

            while (c.moveToNext()) {
                list.add(
                    MealEntry(
                        id = c.getLong(idIndex),
                        name = c.getString(nameIndex),
                        calories = c.getInt(caloriesIndex),
                        proteinGrams = c.getDouble(proteinIndex),
                        carbsGrams = c.getDouble(carbsIndex),
                        fatGrams = c.getDouble(fatIndex),
                        portionDesc = c.getString(portionIndex),
                        imagePath = c.getString(imagePathIndex),
                        timestamp = c.getLong(timestampIndex),
                        dateKey = c.getString(dateKeyIndex),
                        notes = c.getString(notesIndex) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun getAllDatesWithMeals(): List<String> {
        val dates = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT DISTINCT $COL_DATE_KEY FROM $TABLE_MEALS ORDER BY $COL_DATE_KEY DESC",
            null
        )
        cursor.use { c ->
            val colIdx = c.getColumnIndexOrThrow(COL_DATE_KEY)
            while (c.moveToNext()) {
                dates.add(c.getString(colIdx))
            }
        }
        return dates
    }

    fun getAllMeals(): List<MealEntry> {
        val list = mutableListOf<MealEntry>()
        val db = readableDatabase
        val cursor = db.query(TABLE_MEALS, null, null, null, null, null, "$COL_TIMESTAMP DESC")
        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(COL_ID)
            val nameIndex = c.getColumnIndexOrThrow(COL_NAME)
            val caloriesIndex = c.getColumnIndexOrThrow(COL_CALORIES)
            val proteinIndex = c.getColumnIndexOrThrow(COL_PROTEIN)
            val carbsIndex = c.getColumnIndexOrThrow(COL_CARBS)
            val fatIndex = c.getColumnIndexOrThrow(COL_FAT)
            val portionIndex = c.getColumnIndexOrThrow(COL_PORTION)
            val imagePathIndex = c.getColumnIndexOrThrow(COL_IMAGE_PATH)
            val timestampIndex = c.getColumnIndexOrThrow(COL_TIMESTAMP)
            val dateKeyIndex = c.getColumnIndexOrThrow(COL_DATE_KEY)
            val notesIndex = c.getColumnIndexOrThrow(COL_NOTES)

            while (c.moveToNext()) {
                list.add(
                    MealEntry(
                        id = c.getLong(idIndex),
                        name = c.getString(nameIndex),
                        calories = c.getInt(caloriesIndex),
                        proteinGrams = c.getDouble(proteinIndex),
                        carbsGrams = c.getDouble(carbsIndex),
                        fatGrams = c.getDouble(fatIndex),
                        portionDesc = c.getString(portionIndex),
                        imagePath = c.getString(imagePathIndex),
                        timestamp = c.getLong(timestampIndex),
                        dateKey = c.getString(dateKeyIndex),
                        notes = c.getString(notesIndex) ?: ""
                    )
                )
            }
        }
        return list
    }
}
