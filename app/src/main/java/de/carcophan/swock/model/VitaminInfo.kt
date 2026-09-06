package de.carcophan.swock.model

import org.json.JSONArray
import org.json.JSONObject

enum class VitaminType(
    val code: String,
    val displayName: String,
    val shortName: String,
    val defaultUnit: String,
    val standardRda: Double, // Empfohlene Tagesdosis (DGE Referenzwert für Erwachsene)
    val benefit: String,
    val tagColorHex: Long
) {
    VITAMIN_A("VIT_A", "Vitamin A", "Vit A", "µg", 900.0, "Sehkraft, Haut & Schleimhäute", 0xFFFF9800),
    VITAMIN_B1("VIT_B1", "Vitamin B1 (Thiamin)", "Vit B1", "mg", 1.2, "Energiestoffwechsel & Nerven", 0xFF4CAF50),
    VITAMIN_B2("VIT_B2", "Vitamin B2 (Riboflavin)", "Vit B2", "mg", 1.4, "Zellenergie & Sehkraft", 0xFF8BC34A),
    VITAMIN_B3("VIT_B3", "Vitamin B3 (Niacin)", "Vit B3", "mg", 15.0, "Regeneration & Haut", 0xFF009688),
    VITAMIN_B5("VIT_B5", "Vitamin B5 (Pantothensäure)", "Vit B5", "mg", 6.0, "Hormone & Neurotransmitter", 0xFF00BCD4),
    VITAMIN_B6("VIT_B6", "Vitamin B6 (Pyridoxin)", "Vit B6", "mg", 1.5, "Eiweißstoffwechsel & Abwehr", 0xFF03A9F4),
    VITAMIN_B7("VIT_B7", "Vitamin B7 (Biotin)", "Vit B7", "µg", 40.0, "Haare, Haut & Nägel", 0xFF3F51B5),
    VITAMIN_B9("VIT_B9", "Vitamin B9 (Folsäure)", "Vit B9", "µg", 300.0, "Zellteilung & Blutbildung", 0xFF673AB7),
    VITAMIN_B12("VIT_B12", "Vitamin B12 (Cobalamin)", "Vit B12", "µg", 4.0, "Nervensystem & Blutbildung", 0xFF9C27B0),
    VITAMIN_C("VIT_C", "Vitamin C", "Vit C", "mg", 100.0, "Immunsystem, Kollagen & Zellschutz", 0xFFFF5722),
    VITAMIN_D("VIT_D", "Vitamin D", "Vit D", "µg", 20.0, "Knochen, Zähne & Immunsystem", 0xFFFFC107),
    VITAMIN_E("VIT_E", "Vitamin E", "Vit E", "mg", 13.0, "Zellschutz & Radikalfänger", 0xFFE91E63),
    VITAMIN_K("VIT_K", "Vitamin K", "Vit K", "µg", 70.0, "Blutgerinnung & Knochenstoffwechsel", 0xFF2E7D32);

    companion object {
        fun match(rawName: String?): VitaminType? {
            if (rawName.isNullOrBlank()) return null
            val clean = rawName.trim().lowercase()
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("-", " ")
                .replace("_", " ")

            return when {
                clean.contains("vitamin a") || clean == "vit a" || clean.contains("retinol") || clean.contains("carotin") -> VITAMIN_A
                clean.contains("vitamin b12") || clean == "vit b12" || clean.contains("b12") || clean.contains("cobalamin") -> VITAMIN_B12
                clean.contains("vitamin b1") || clean == "vit b1" || clean.contains("thiamin") -> VITAMIN_B1
                clean.contains("vitamin b2") || clean == "vit b2" || clean.contains("riboflavin") -> VITAMIN_B2
                clean.contains("vitamin b3") || clean == "vit b3" || clean.contains("niacin") -> VITAMIN_B3
                clean.contains("vitamin b5") || clean == "vit b5" || clean.contains("pantothen") -> VITAMIN_B5
                clean.contains("vitamin b6") || clean == "vit b6" || clean.contains("pyridoxin") -> VITAMIN_B6
                clean.contains("vitamin b7") || clean == "vit b7" || clean.contains("biotin") -> VITAMIN_B7
                clean.contains("vitamin b9") || clean == "vit b9" || clean.contains("folsaeure") || clean.contains("folat") || clean.contains("folic") -> VITAMIN_B9
                clean.contains("vitamin c") || clean == "vit c" || clean.contains("ascorbin") -> VITAMIN_C
                clean.contains("vitamin d") || clean == "vit d" || clean.contains("cholecalciferol") -> VITAMIN_D
                clean.contains("vitamin e") || clean == "vit e" || clean.contains("tocopherol") -> VITAMIN_E
                clean.contains("vitamin k") || clean == "vit k" || clean.contains("phyllochinon") || clean.contains("menachinon") -> VITAMIN_K
                else -> null
            }
        }
    }
}

data class VitaminAmount(
    val type: VitaminType,
    val amount: Double,
    val unit: String = type.defaultUnit
) {
    val percentRda: Int
        get() = if (type.standardRda > 0) ((amount / type.standardRda) * 100).toInt() else 0

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("code", type.code)
            put("name", type.displayName)
            put("amount", amount)
            put("unit", unit)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): VitaminAmount? {
            val code = json.optString("code", "")
            val name = json.optString("name", "")
            val type = VitaminType.values().find { it.code.equals(code, ignoreCase = true) }
                ?: VitaminType.match(name)
                ?: return null

            val rawAmount = json.optDouble("amount", 0.0)
            val unit = json.optString("unit", type.defaultUnit)

            return VitaminAmount(
                type = type,
                amount = Math.round(rawAmount * 100.0) / 100.0,
                unit = if (unit.isNotBlank()) unit else type.defaultUnit
            )
        }

        fun listToJsonString(list: List<VitaminAmount>): String {
            val array = JSONArray()
            for (item in list) {
                array.put(item.toJsonObject())
            }
            return array.toString()
        }

        fun listFromJsonString(jsonStr: String?): List<VitaminAmount> {
            if (jsonStr.isNullOrBlank() || jsonStr.trim() == "[]") return emptyList()
            val list = mutableListOf<VitaminAmount>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val amount = fromJsonObject(obj)
                    if (amount != null && amount.amount > 0.0) {
                        list.add(amount)
                    }
                }
            } catch (e: Exception) {
                // Return empty list on parse failure
            }
            return list
        }
    }
}
