package de.carcophan.swock.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import de.carcophan.swock.model.FoodAnalysisResult
import de.carcophan.swock.model.HealthRating
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

        // Try preferredModel (gemini-3.6-flash) first, fallback to gemini-2.5-flash / 1.5-flash
        val candidateModels = listOf("gemini-3.6-flash", "gemini-2.5-flash", "gemini-1.5-flash")
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

        val candidateModels = listOf("gemini-3.6-flash", "gemini-2.5-flash", "gemini-1.5-flash")
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
            
            Bewerte außerdem die gesundheitliche Qualität (Ernährungs-Ampel) nach ernährungswissenschaftlichen Kriterien (Nutri-Score / Nährstoffdichte / Verarbeitungsgrad / Zucker & gesättigte Fette):
            - "GREEN": Nährstoffreich, vollwertig, Gemüse, Obst, mageres Eiweiß, Wasser/ungesüßter Tee, wenig zugesetzter Zucker & schlechte Fette.
            - "YELLOW": Ausgewogen bis durchschnittlich verarbeitet, normale Kohlenhydrat-/Fettträger (z. B. Nudeln, Reis, Brot, Mischkost), in Maßen vollkommen in Ordnung.
            - "RED": Hohe Dichte an freiem Zucker, gesättigten Fettsäuren, Frittiertem, Fast Food, Softdrinks/Alkohol oder stark verarbeitet.
            
            Gib deine Antwort AUSSCHLIESSLICH im folgenden JSON-Format zurück:
            {
              "name": "Name der Speise oder des Getränks auf Deutsch",
              "calories": 450,
              "proteinGrams": 22.5,
              "carbsGrams": 55.0,
              "fatGrams": 14.0,
              "portionDesc": "1 Portion, ca. 350g",
              "notes": "Zutaten oder kurze Anmerkung",
              "healthRating": "GREEN",
              "healthReason": "Reich an Proteinen und Ballaststoffen, wenig Zucker",
              "category": "PASTA",
              "emoji": "🍝"
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
            
            Bewerte außerdem die gesundheitliche Qualität (Ernährungs-Ampel) nach ernährungswissenschaftlichen Kriterien (Nutri-Score / Nährstoffdichte / Verarbeitungsgrad / Zucker & gesättigte Fette):
            - "GREEN": Nährstoffreich, vollwertig, Gemüse, Obst, mageres Eiweiß, Wasser/ungesüßter Tee, wenig zugesetzter Zucker & schlechte Fette.
            - "YELLOW": Ausgewogen bis durchschnittlich verarbeitet, normale Kohlenhydrat-/Fettträger (z. B. Nudeln, Reis, Brot, Mischkost), in Maßen vollkommen in Ordnung.
            - "RED": Hohe Dichte an freiem Zucker, gesättigten Fettsäuren, Frittiertem, Fast Food, Softdrinks/Alkohol oder stark verarbeitet.
            
            Gib deine Antwort AUSSCHLIESSLICH im folgenden JSON-Format zurück:
            {
              "name": "Name der Speise oder des Getränks auf Deutsch",
              "calories": 450,
              "proteinGrams": 22.5,
              "carbsGrams": 55.0,
              "fatGrams": 14.0,
              "portionDesc": "1 Portion, ca. 350g",
              "notes": "Zutaten oder kurze Anmerkung",
              "healthRating": "GREEN",
              "healthReason": "Reich an Proteinen und Ballaststoffen, wenig Zucker"
            }
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
                calories > 750 && fatGrams > 35 -> HealthRating.RED
                proteinGrams >= 20 && calories <= 500 -> HealthRating.GREEN
                else -> HealthRating.YELLOW
            }
        }

        val category = foodJson.optString("category", "")
        val emoji = foodJson.optString("emoji", "")

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
            emoji = emoji
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
}
