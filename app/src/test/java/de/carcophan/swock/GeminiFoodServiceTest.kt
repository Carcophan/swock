package de.carcophan.swock

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiFoodServiceTest {

    @Test
    fun testJsonParsing() {
        val sampleResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\n  \"name\": \"Lachsfilet mit Brokkoli\",\n  \"calories\": 520,\n  \"proteinGrams\": 42.0,\n  \"carbsGrams\": 12.5,\n  \"fatGrams\": 31.0,\n  \"portionDesc\": \"1 Portion, ca. 300g\",\n  \"notes\": \"Reich an Omega-3\"\n}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val root = JSONObject(sampleResponse)
        val candidates = root.getJSONArray("candidates")
        val candidate = candidates.getJSONObject(0)
        val content = candidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val text = parts.getJSONObject(0).getString("text")

        val foodJson = JSONObject(text.trim())
        assertEquals("Lachsfilet mit Brokkoli", foodJson.getString("name"))
        assertEquals(520, foodJson.getInt("calories"))
        assertEquals(42.0, foodJson.getDouble("proteinGrams"), 0.001)
    }
}
