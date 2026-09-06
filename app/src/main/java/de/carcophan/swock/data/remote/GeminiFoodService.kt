package de.carcophan.swock.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import de.carcophan.swock.model.AvoidanceItem
import de.carcophan.swock.model.AvoidanceSeverity
import de.carcophan.swock.model.DailyNutritionAdvice
import de.carcophan.swock.model.FoodAnalysisResult
import de.carcophan.swock.model.HealthRating
import de.carcophan.swock.model.MealEntry
import de.carcophan.swock.model.RecommendationCategory
import de.carcophan.swock.model.RecommendationItem
import de.carcophan.swock.model.UserProfile
import de.carcophan.swock.model.VitaminAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiFoodService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyzeFoodImage(
        imagePath: String,
        apiKey: String,
        preferredModel: String = "gemini-3.6-flash"
    ): Result<FoodAnalysisResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Bitte hinterlege zuerst deinen Gemini API-Schlüssel in den Einstellungen.")
            )
        }

        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            return@withContext Result.failure(
                IllegalArgumentException("Bilddatei nicht gefunden: $imagePath")
            )
        }

        val base64Image = encodeImageToBase64(imageFile)
            ?: return@withContext Result.failure(
                IllegalStateException("Konnte das Bild nicht für die Analyse vorbereiten.")
            )

        // Use preferredModel (gemini-3.6-flash)
        val candidateModels = listOf("gemini-3.6-flash")
        val modelsToTry = (listOf(preferredModel) + candidateModels).distinct()
        var lastError: Exception? = null

        for (modelName in modelsToTry) {
            try {
                val result = callGeminiApi(modelName, base64Image, apiKey)
                return@withContext Result.success(result)
            } catch (e: Exception) {
                lastError = e
                // If it's an authentication error (400/403 with API_KEY_INVALID), no point retrying another model
                if (e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                    e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
                ) {
                    return@withContext Result.failure(e)
                }
            }
        }

        Result.failure(lastError ?: RuntimeException("Unbekannter Fehler bei der Gemini-Analyse."))
    }

    suspend fun analyzeFoodDescription(
        description: String,
        apiKey: String,
        preferredModel: String = "gemini-3.6-flash"
    ): Result<FoodAnalysisResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Bitte hinterlege zuerst deinen Gemini API-Schlüssel in den Einstellungen.")
            )
        }

        if (description.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Bitte gib eine Beschreibung der Mahlzeit ein.")
            )
        }

        val candidateModels = listOf("gemini-3.6-flash")
        val modelsToTry = (listOf(preferredModel) + candidateModels).distinct()
        var lastError: Exception? = null

        for (modelName in modelsToTry) {
            try {
                val result = callGeminiApiForText(modelName, description, apiKey)
                return@withContext Result.success(result)
            } catch (e: Exception) {
                lastError = e
                if (e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                    e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
                ) {
                    return@withContext Result.failure(e)
                }
            }
        }

        Result.failure(lastError ?: RuntimeException("Unbekannter Fehler bei der Gemini-Textanalyse."))
    }

    private fun callGeminiApiForText(modelName: String, description: String, apiKey: String): FoodAnalysisResult {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val promptText = """
            Du bist ein professioneller Ernährungsberater und Experte für Nahrungsmittelanalyse.
            Analysiere die folgende Beschreibung einer Mahlzeit oder eines Nahrungsmittels/Getränks sorgfältig:
            "$description"

            Identifiziere das Nahrungsmittel oder Getränk, schätze oder berechne die Portionsgröße und ermittle möglichst genau die Kalorien (kcal) und die Makronährstoffe (Eiweiß, Kohlenhydrate, Fett).
            Falls mehrere Nahrungsmittel oder Getränke/Beilagen genannt werden, erfasse das gesamte Gericht bzw. die gesamte Mahlzeit kombiniert.
            
            Bewerte außerdem die gesundheitliche Qualität (6-stufige Lebensmittel-Ampel von A bis F):
            - "A": Sehr gesund (Grün) – Hohe Nährstoffdichte, vollwertig, unverarbeitet, frisches Gemüse, Obst, mageres Protein, Hülsenfrüchte, Wasser/ungesüßter Tee.
            - "B": Gesund (Hellgrün) – Nährstoffreich mit wertvollen Fetten/Kohlenhydraten (z. B. Haferflocken, Nüsse, Vollkorn, Fisch, fettarme Milchprodukte).
            - "C": Ausgewogen (Gelb) – Solide durchschnittliche Mischkost (z. B. Reis, Nudeln, Brot, Kartoffeln, normale Fleischgerichte), in Maßen gut.
            - "D": Mäßig (Hellorange) – Höherer Kalorien-, Fett-, Salz- oder einfacher Kohlenhydratgehalt (z. B. Weißbrot, fetteres Fleisch, Fertigsaucen).
            - "E": Ungesund (Orange) – Stark verarbeitet, frittiert, hoher Anteil an gesättigten Fetten oder zugesetztem Zucker (z. B. Burger, Pommes, Pizza, Kuchen).
            - "F": Sehr ungesund (Rot) – Sehr hoher Gehalt an Industriezucker, Transfetten, Süßigkeiten, Chips, Softdrinks, Energy-Drinks, Alkohol oder hochgradig verarbeitetes Fast Food.
            
            Ermittle außerdem die enthaltenen Vitaminarten (z. B. Vitamin C, Vitamin A, Vitamin D, Vitamin E, Vitamin K, Vitamin B1, B2, B6, B12 etc.) mit geschätzter Menge und Einheit (mg oder µg).
            
            Gib deine Antwort AUSSCHLIESSLICH im folgenden JSON-Format zurück:
            {
              "name": "Name der Speise oder des Getränks auf Deutsch",
              "calories": 450,
              "proteinGrams": 22.5,
              "carbsGrams": 55.0,
              "fatGrams": 14.0,
              "portionDesc": "1 Portion, ca. 350g",
              "notes": "Zutaten oder kurze Anmerkung",
              "healthRating": "A",
              "healthReason": "Reich an Proteinen und Ballaststoffen, wenig Zucker",
              "category": "PASTA",
              "emoji": "🍝",
              "vitamins": [
                {"name": "Vitamin C", "amount": 25.0, "unit": "mg"},
                {"name": "Vitamin A", "amount": 150.0, "unit": "µg"}
              ]
            }
            Wähle für "category" eine der folgenden Kategorien: PASTA, PIZZA, BURGER, SALAD, FRUIT, MEAT, FISH, BREAKFAST, EGG, BREAD, SOUP, ASIAN_RICE, DESSERT, ICE_CREAM, COFFEE, DRINK, DEFAULT.
            Wähle für "emoji" genau ein passendes Food-Emoji (z. B. 🍝, 🍕, 🥗, ☕).
        """.trimIndent()

        val rootJson = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject().apply {
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply {
                    put("text", promptText)
                })
                put("parts", partsArray)
            }
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.2)
            }
            put("generationConfig", genConfig)
        }

        val request = Request.Builder()
            .url(url)
            .post(rootJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errObj = JSONObject(responseBody).optJSONObject("error")
                errObj?.optString("message") ?: "HTTP ${response.code}: $responseBody"
            } catch (e: Exception) {
                "HTTP ${response.code}: $responseBody"
            }
            throw RuntimeException(errorMsg)
        }

        return parseGeminiResponse(responseBody)
    }

    private fun callGeminiApi(modelName: String, base64Image: String, apiKey: String): FoodAnalysisResult {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val promptText = """
            Du bist ein professioneller Ernährungsberater und Experte für Nahrungsmittelanalyse.
            Analysiere das beigefügte Bild sorgfältig. 
            Identifiziere das Nahrungsmittel oder Getränk, schätze die Portionsgröße und ermittle möglichst genau die Kalorien (kcal) und die Makronährstoffe (Eiweiß, Kohlenhydrate, Fett).
            Falls mehrere Nahrungsmittel auf dem Bild sind, erfasse das gesamte Gericht/die gesamte Mahlzeit.
            
            Bewerte außerdem die gesundheitliche Qualität (6-stufige Lebensmittel-Ampel von A bis F):
            - "A": Sehr gesund (Grün) – Hohe Nährstoffdichte, vollwertig, unverarbeitet, frisches Gemüse, Obst, mageres Protein, Hülsenfrüchte, Wasser/ungesüßter Tee.
            - "B": Gesund (Hellgrün) – Nährstoffreich mit wertvollen Fetten/Kohlenhydraten (z. B. Haferflocken, Nüsse, Vollkorn, Fisch, fettarme Milchprodukte).
            - "C": Ausgewogen (Gelb) – Solide durchschnittliche Mischkost (z. B. Reis, Nudeln, Brot, Kartoffeln, normale Fleischgerichte), in Maßen gut.
            - "D": Mäßig (Hellorange) – Höherer Kalorien-, Fett-, Salz- oder einfacher Kohlenhydratgehalt (z. B. Weißbrot, fetteres Fleisch, Fertigsaucen).
            - "E": Ungesund (Orange) – Stark verarbeitet, frittiert, hoher Anteil an gesättigten Fetten oder zugesetztem Zucker (z. B. Burger, Pommes, Pizza, Kuchen).
            - "F": Sehr ungesund (Rot) – Sehr hoher Gehalt an Industriezucker, Transfetten, Süßigkeiten, Chips, Softdrinks, Energy-Drinks, Alkohol oder hochgradig verarbeitetes Fast Food.
            
            Ermittle außerdem die enthaltenen Vitaminarten (z. B. Vitamin C, Vitamin A, Vitamin D, Vitamin E, Vitamin K, Vitamin B1, B2, B6, B12 etc.) mit geschätzter Menge und Einheit (mg oder µg).
            
            Gib deine Antwort AUSSCHLIESSLICH im folgenden JSON-Format zurück:
            {
              "name": "Name der Speise oder des Getränks auf Deutsch",
              "calories": 450,
              "proteinGrams": 22.5,
              "carbsGrams": 55.0,
              "fatGrams": 14.0,
              "portionDesc": "1 Portion, ca. 350g",
              "notes": "Zutaten oder kurze Anmerkung",
              "healthRating": "A",
              "healthReason": "Reich an Proteinen und Ballaststoffen, wenig Zucker",
              "category": "PASTA",
              "emoji": "🍝",
              "vitamins": [
                {"name": "Vitamin C", "amount": 25.0, "unit": "mg"},
                {"name": "Vitamin A", "amount": 150.0, "unit": "µg"}
              ]
            }
            Wähle für "category" eine der folgenden Kategorien: PASTA, PIZZA, BURGER, SALAD, FRUIT, MEAT, FISH, BREAKFAST, EGG, BREAD, SOUP, ASIAN_RICE, DESSERT, ICE_CREAM, COFFEE, DRINK, DEFAULT.
            Wähle für "emoji" genau ein passendes Food-Emoji (z. B. 🍝, 🍕, 🥗, ☕).
        """.trimIndent()

        val rootJson = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject().apply {
                val partsArray = JSONArray()

                // Text part
                partsArray.put(JSONObject().apply {
                    put("text", promptText)
                })

                // Image part
                partsArray.put(JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mime_type", "image/jpeg")
                        put("data", base64Image)
                    }
                    put("inline_data", inlineData)
                })

                put("parts", partsArray)
            }
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.2)
            }
            put("generationConfig", genConfig)
        }

        val request = Request.Builder()
            .url(url)
            .post(rootJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errObj = JSONObject(responseBody).optJSONObject("error")
                errObj?.optString("message") ?: "HTTP ${response.code}: $responseBody"
            } catch (e: Exception) {
                "HTTP ${response.code}: $responseBody"
            }
            throw RuntimeException(errorMsg)
        }

        return parseGeminiResponse(responseBody)
    }

    fun parseGeminiResponse(responseBody: String): FoodAnalysisResult {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates")
            ?: throw RuntimeException("Keine Antwort von Gemini erhalten.")
        if (candidates.length() == 0) {
            throw RuntimeException("Gemini hat keine Kandidaten zurückgeliefert.")
        }

        val candidate = candidates.getJSONObject(0)
        val content = candidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val text = parts.getJSONObject(0).getString("text")

        // Clean any stray markdown formatting if model wrapped it in ```json ... ```
        val cleanedJson = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val foodJson = JSONObject(cleanedJson)
        val name = foodJson.optString("name", "Unbekannte Speise")
        val calories = foodJson.optInt("calories", 0)
        val proteinGrams = foodJson.optDouble("proteinGrams", 0.0)
        val carbsGrams = foodJson.optDouble("carbsGrams", 0.0)
        val fatGrams = foodJson.optDouble("fatGrams", 0.0)
        val portionDesc = foodJson.optString("portionDesc", "1 Portion")
        val notes = foodJson.optString("notes", "")

        val healthRatingRaw = foodJson.optString("healthRating", "")
        val healthReason = foodJson.optString("healthReason", "")
        val healthRating = if (healthRatingRaw.isNotBlank()) {
            HealthRating.fromString(healthRatingRaw)
        } else {
            // Heuristic fallback if not provided
            when {
                calories > 800 && fatGrams > 40 -> HealthRating.F
                calories > 650 && fatGrams > 30 -> HealthRating.E
                calories > 550 || fatGrams > 22 -> HealthRating.D
                proteinGrams >= 25 && calories <= 450 -> HealthRating.A
                proteinGrams >= 15 && calories <= 550 -> HealthRating.B
                else -> HealthRating.C
            }
        }

        val category = foodJson.optString("category", "")
        val emoji = foodJson.optString("emoji", "")

        val vitaminsList = mutableListOf<VitaminAmount>()
        val vitaminsArray = foodJson.optJSONArray("vitamins")
        if (vitaminsArray != null) {
            for (i in 0 until vitaminsArray.length()) {
                val vitObj = vitaminsArray.optJSONObject(i) ?: continue
                val amount = VitaminAmount.fromJsonObject(vitObj)
                if (amount != null && amount.amount > 0.0) {
                    vitaminsList.add(amount)
                }
            }
        }

        return FoodAnalysisResult(
            name = name,
            calories = calories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams,
            portionDesc = portionDesc,
            notes = notes,
            healthRating = healthRating,
            healthReason = healthReason,
            category = category,
            emoji = emoji,
            vitamins = vitaminsList
        )
    }

    suspend fun testApiKey(apiKey: String, model: String = "gemini-3.6-flash"): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Der API-Schlüssel ist leer."))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val testJson = JSONObject().apply {
                val contents = JSONArray()
                val item = JSONObject().apply {
                    val parts = JSONArray()
                    parts.put(JSONObject().apply { put("text", "Antworte mit 'OK'") })
                    put("parts", parts)
                }
                contents.put(item)
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(testJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val body = response.body?.string() ?: ""
                val msg = try {
                    JSONObject(body).optJSONObject("error")?.optString("message") ?: body
                } catch (e: Exception) {
                    body
                }
                Result.failure(RuntimeException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun encodeImageToBase64(file: File): String? {
        return try {
            val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val maxDim = 1024
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

            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()

            if (scaled != original) {
                scaled.recycle()
            }
            original.recycle()

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generateAiFoodImage(
        dishName: String,
        apiKey: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-002:predict?key=$apiKey"
            val prompt = "Stylized minimalist 3d icon of $dishName, isolated on clean pastel gradient background, vibrant appetizing colors, soft lighting, modern digital art style"
            val rootJson = JSONObject().apply {
                val instances = JSONArray().apply {
                    put(JSONObject().apply {
                        put("prompt", prompt)
                    })
                }
                put("instances", instances)
                put("parameters", JSONObject().apply {
                    put("sampleCount", 1)
                    put("aspectRatio", "1:1")
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(rootJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val predictions = root.optJSONArray("predictions") ?: return@withContext null
            if (predictions.length() == 0) return@withContext null
            val b64 = predictions.getJSONObject(0).optString("bytesBase64Encoded", "")
            if (b64.isBlank()) return@withContext null
            Base64.decode(b64, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generateDailyNutritionAdvice(
        meals: List<MealEntry>,
        profile: UserProfile,
        targetCalories: Int,
        activeCaloriesBurned: Int,
        apiKey: String,
        preferredModel: String = "gemini-3.6-flash"
    ): Result<DailyNutritionAdvice> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Bitte hinterlege zuerst deinen Gemini API-Schlüssel in den Profileinstellungen.")
            )
        }

        val totalCalories = meals.sumOf { it.calories }
        val totalProtein = meals.sumOf { it.proteinGrams }
        val totalCarbs = meals.sumOf { it.carbsGrams }
        val totalFat = meals.sumOf { it.fatGrams }
        val remainingCalories = targetCalories - totalCalories

        val mealsSummary = if (meals.isEmpty()) {
            "Bisher wurden heute noch keine Mahlzeiten oder Getränke verzehrt."
        } else {
            meals.joinToString("\n") { meal ->
                "- ${meal.name}: ${meal.calories} kcal, ${meal.proteinGrams}g Protein, ${meal.carbsGrams}g KH, ${meal.fatGrams}g Fett (Portion: ${meal.portionDesc}, Ampel: ${meal.healthRating.grade} - ${meal.healthRating.label})"
            }
        }

        val promptText = """
            Du bist ein professioneller Ernährungscoach und Diätologe.
            Analysiere den heutigen Ernährungstag einer Person und gib fundierte, alltagstaugliche Empfehlungen:
            
            Benutzerprofil:
            - Alter: ${profile.age} Jahre, Geschlecht: ${profile.gender.displayName}
            - Größe: ${profile.heightCm} cm, Gewicht: ${profile.weightKg} kg
            - Aktivitätslevel: ${profile.activityLevel.displayName} (${profile.activityLevel.description})
            - Kalorienziel: ${profile.calorieGoal.displayName}
            - Berechnetes Tagesziel: $targetCalories kcal (inkl. $activeCaloriesBurned kcal verbrannter Aktivität)
            
            Bisher verzehrt am heutigen Tag:
            - Kalorien: $totalCalories kcal (Rest: $remainingCalories kcal)
            - Eiweiß: ${String.format("%.1f", totalProtein)}g
            - Kohlenhydrate: ${String.format("%.1f", totalCarbs)}g
            - Fett: ${String.format("%.1f", totalFat)}g
            
            Bisherige Mahlzeiten:
            $mealsSummary
            
            Deine Aufgabe:
            1. Gib konkrete Vorschläge zu Speisen und/oder Getränken ("recommendations"), die die Person heute noch ZUSÄTZLICH essen oder trinken sollte, um die Nahrungsaufnahme optimal auszubalancieren (z.B. fehlende Proteine auffüllen, gesunde Fette, ballaststoffreiches Gemüse, Obst mit spezifischen Vitaminen, oder gezielte Flüssigkeitszufuhr/Hydration).
            2. Gib konkrete Ratschläge ("avoidances"), was die Person für den aktuellen Tag VERMEIDEN sollte, weil sie evtl. schon genug oder zu viel davon hatte (z.B. überschüssige Fette, einfache Zucker, verarbeitete Speisen, Alkohol/flüssige Kalorien, oder weil das Kalorienbudget fast erschöpft ist).
            
            Gib deine Antwort AUSSCHLIESSLICH im folgenden JSON-Format zurück:
            {
              "balanceScore": "Kurzer prägnanter Status z.B. 'Leichtes Proteindefizit' oder 'Ausgewogen'",
              "summaryText": "1-2 Sätze motivierende und ehrliche Gesamteinschätzung des bisherigen Tages.",
              "recommendations": [
                {
                  "title": "Titel des Vorschlags",
                  "items": ["Konkretes Beispiel 1", "Konkretes Beispiel 2"],
                  "reason": "Genau Begründung, warum das heute noch fehlt",
                  "category": "FOOD", // oder "DRINK"
                  "nutrientFocus": "z.B. Eiweiß & Zink"
                }
              ],
              "avoidances": [
                {
                  "title": "Titel der Warnung / Vermeidung",
                  "items": ["Konkretes No-Go 1", "Konkretes No-Go 2"],
                  "reason": "Genaue Begründung, warum man davon heute schon genug/zu viel hatte",
                  "severity": "HIGH" // oder "MODERATE" oder "INFO"
                }
              ]
            }
        """.trimIndent()

        val candidateModels = listOf("gemini-3.6-flash")
        val modelsToTry = (listOf(preferredModel) + candidateModels).distinct()
        var lastError: Exception? = null

        for (modelName in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val rootJson = JSONObject().apply {
                    val contentsArray = JSONArray()
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray()
                        partsArray.put(JSONObject().apply {
                            put("text", promptText)
                        })
                        put("parts", partsArray)
                    }
                    contentsArray.put(contentObj)
                    put("contents", contentsArray)

                    val genConfig = JSONObject().apply {
                        put("response_mime_type", "application/json")
                        put("temperature", 0.3)
                    }
                    put("generationConfig", genConfig)
                }

                val request = Request.Builder()
                    .url(url)
                    .post(rootJson.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(responseBody).optJSONObject("error")
                        errObj?.optString("message") ?: "HTTP ${response.code}: $responseBody"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $responseBody"
                    }
                    throw RuntimeException(errorMsg)
                }

                val advice = parseAdviceResponse(responseBody)
                return@withContext Result.success(advice)
            } catch (e: Exception) {
                lastError = e
                if (e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                    e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
                ) {
                    return@withContext Result.failure(e)
                }
            }
        }

        Result.failure(lastError ?: RuntimeException("Unbekannter Fehler bei der Generierung der Ernährungsempfehlungen."))
    }

    private fun parseAdviceResponse(responseBody: String): DailyNutritionAdvice {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates")
            ?: throw RuntimeException("Keine Antwort von Gemini erhalten.")
        if (candidates.length() == 0) {
            throw RuntimeException("Gemini hat keine Kandidaten zurückgeliefert.")
        }

        val candidate = candidates.getJSONObject(0)
        val content = candidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val text = parts.getJSONObject(0).getString("text")

        val cleanedJson = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(cleanedJson)
        val balanceScore = json.optString("balanceScore", "Tages-Empfehlungen")
        val summaryText = json.optString("summaryText", "")

        val recList = mutableListOf<RecommendationItem>()
        val recArray = json.optJSONArray("recommendations")
        if (recArray != null) {
            for (i in 0 until recArray.length()) {
                val itemObj = recArray.optJSONObject(i) ?: continue
                val title = itemObj.optString("title", "")
                val reason = itemObj.optString("reason", "")
                val nutrientFocus = itemObj.optString("nutrientFocus", "")
                val categoryStr = itemObj.optString("category", "FOOD")
                val category = if (categoryStr.equals("DRINK", ignoreCase = true)) {
                    RecommendationCategory.DRINK
                } else RecommendationCategory.FOOD

                val items = mutableListOf<String>()
                val itemsArr = itemObj.optJSONArray("items")
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        items.add(itemsArr.optString(j))
                    }
                }

                if (title.isNotBlank()) {
                    recList.add(
                        RecommendationItem(
                            title = title,
                            items = items,
                            reason = reason,
                            category = category,
                            nutrientFocus = nutrientFocus
                        )
                    )
                }
            }
        }

        val avoidList = mutableListOf<AvoidanceItem>()
        val avoidArray = json.optJSONArray("avoidances")
        if (avoidArray != null) {
            for (i in 0 until avoidArray.length()) {
                val itemObj = avoidArray.optJSONObject(i) ?: continue
                val title = itemObj.optString("title", "")
                val reason = itemObj.optString("reason", "")
                val severityStr = itemObj.optString("severity", "MODERATE")
                val severity = when (severityStr.uppercase()) {
                    "HIGH" -> AvoidanceSeverity.HIGH
                    "INFO" -> AvoidanceSeverity.INFO
                    else -> AvoidanceSeverity.MODERATE
                }

                val items = mutableListOf<String>()
                val itemsArr = itemObj.optJSONArray("items")
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        items.add(itemsArr.optString(j))
                    }
                }

                if (title.isNotBlank()) {
                    avoidList.add(
                        AvoidanceItem(
                            title = title,
                            items = items,
                            reason = reason,
                            severity = severity
                        )
                    )
                }
            }
        }

        return DailyNutritionAdvice(
            balanceScore = balanceScore,
            summaryText = summaryText,
            recommendations = recList,
            avoidances = avoidList,
            isAiGenerated = true
        )
    }
}

