package de.carcophan.swock.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import de.carcophan.swock.model.HealthRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

data class FoodCategoryTheme(
    val categoryId: String,
    val label: String,
    val defaultEmoji: String,
    val startColor: Int,
    val endColor: Int,
    val accentColor: Int
)

object SymbolicFoodImageGenerator {

    val THEMES = mapOf(
        "PASTA" to FoodCategoryTheme(
            categoryId = "PASTA",
            label = "Pasta & Nudeln",
            defaultEmoji = "🍝",
            startColor = 0xFFFF7043.toInt(), // Coral
            endColor = 0xFFD84315.toInt(),   // Deep Rust Orange
            accentColor = 0xFFFFCCBC.toInt()
        ),
        "PIZZA" to FoodCategoryTheme(
            categoryId = "PIZZA",
            label = "Pizza",
            defaultEmoji = "🍕",
            startColor = 0xFFFFA726.toInt(), // Amber
            endColor = 0xFFE64A19.toInt(),   // Burnt Orange
            accentColor = 0xFFFFE082.toInt()
        ),
        "BURGER" to FoodCategoryTheme(
            categoryId = "BURGER",
            label = "Burger & Snacks",
            defaultEmoji = "🍔",
            startColor = 0xFFFFB300.toInt(), // Golden Amber
            endColor = 0xFFBF360C.toInt(),   // Deep Terra
            accentColor = 0xFFFFD54F.toInt()
        ),
        "SALAD" to FoodCategoryTheme(
            categoryId = "SALAD",
            label = "Salat & Gemüse",
            defaultEmoji = "🥗",
            startColor = 0xFF66BB6A.toInt(), // Fresh Emerald
            endColor = 0xFF1B5E20.toInt(),   // Deep Forest Green
            accentColor = 0xFFA5D6A7.toInt()
        ),
        "FRUIT" to FoodCategoryTheme(
            categoryId = "FRUIT",
            label = "Obst & Früchte",
            defaultEmoji = "🍎",
            startColor = 0xFF81C784.toInt(), // Lime Green
            endColor = 0xFFC2185B.toInt(),   // Berry Pink
            accentColor = 0xFFF48FB1.toInt()
        ),
        "MEAT" to FoodCategoryTheme(
            categoryId = "MEAT",
            label = "Fleischgericht",
            defaultEmoji = "🥩",
            startColor = 0xFFEF5350.toInt(), // Warm Coral Red
            endColor = 0xFF880E4F.toInt(),   // Deep Mahogany
            accentColor = 0xFFFFCDD2.toInt()
        ),
        "FISH" to FoodCategoryTheme(
            categoryId = "FISH",
            label = "Fisch & Meeresfrüchte",
            defaultEmoji = "🍣",
            startColor = 0xFF26A69A.toInt(), // Teal
            endColor = 0xFF004D40.toInt(),   // Deep Ocean
            accentColor = 0xFF80CBC4.toInt()
        ),
        "BREAKFAST" to FoodCategoryTheme(
            categoryId = "BREAKFAST",
            label = "Frühstück & Müsli",
            defaultEmoji = "🥣",
            startColor = 0xFFFFCA28.toInt(), // Warm Sun
            endColor = 0xFFE65100.toInt(),   // Sunrise Orange
            accentColor = 0xFFFFECB3.toInt()
        ),
        "EGG" to FoodCategoryTheme(
            categoryId = "EGG",
            label = "Eiergericht",
            defaultEmoji = "🍳",
            startColor = 0xFFFDD835.toInt(), // Bright Yellow
            endColor = 0xFFFB8C00.toInt(),   // Warm Amber
            accentColor = 0xFFFFF59D.toInt()
        ),
        "BREAD" to FoodCategoryTheme(
            categoryId = "BREAD",
            label = "Brot & Backwaren",
            defaultEmoji = "🥪",
            startColor = 0xFFDCE775.toInt(), // Wheat Gold
            endColor = 0xFF6D4C41.toInt(),   // Warm Roast Brown
            accentColor = 0xFFD7CCC8.toInt()
        ),
        "SOUP" to FoodCategoryTheme(
            categoryId = "SOUP",
            label = "Suppe & Eintopf",
            defaultEmoji = "🍲",
            startColor = 0xFFFFA726.toInt(), // Warm Amber
            endColor = 0xFFB71C1C.toInt(),   // Deep Crimson
            accentColor = 0xFFFFCC80.toInt()
        ),
        "ASIAN_RICE" to FoodCategoryTheme(
            categoryId = "ASIAN_RICE",
            label = "Reis & Asiatisch",
            defaultEmoji = "🍛",
            startColor = 0xFFAB47BC.toInt(), // Lotus Purple
            endColor = 0xFF303F9F.toInt(),   // Royal Indigo
            accentColor = 0xFFCE93D8.toInt()
        ),
        "DESSERT" to FoodCategoryTheme(
            categoryId = "DESSERT",
            label = "Dessert & Süßes",
            defaultEmoji = "🍰",
            startColor = 0xFFEC407A.toInt(), // Rose Pink
            endColor = 0xFF6A1B9A.toInt(),   // Deep Violet
            accentColor = 0xFFF8BBD0.toInt()
        ),
        "ICE_CREAM" to FoodCategoryTheme(
            categoryId = "ICE_CREAM",
            label = "Eis & Sorbet",
            defaultEmoji = "🍦",
            startColor = 0xFF80CBC4.toInt(), // Soft Mint
            endColor = 0xFFD81B60.toInt(),   // Berry Pink
            accentColor = 0xFFB2DFDB.toInt()
        ),
        "COFFEE" to FoodCategoryTheme(
            categoryId = "COFFEE",
            label = "Kaffee & Heißgetränk",
            defaultEmoji = "☕",
            startColor = 0xFF8D6E63.toInt(), // Coffee Roast
            endColor = 0xFF3E2723.toInt(),   // Dark Espresso
            accentColor = 0xFFD7CCC8.toInt()
        ),
        "DRINK" to FoodCategoryTheme(
            categoryId = "DRINK",
            label = "Getränk & Erfrischung",
            defaultEmoji = "🥤",
            startColor = 0xFF29B6F6.toInt(), // Sky Cyan
            endColor = 0xFF283593.toInt(),   // Deep Indigo
            accentColor = 0xFFB3E5FC.toInt()
        ),
        "DEFAULT" to FoodCategoryTheme(
            categoryId = "DEFAULT",
            label = "Mahlzeit",
            defaultEmoji = "🍽️",
            startColor = 0xFF5C6BC0.toInt(), // Slate Indigo
            endColor = 0xFF1A237E.toInt(),   // Midnight Navy
            accentColor = 0xFFC5CAE9.toInt()
        )
    )

    fun resolveTheme(name: String, categoryHint: String = "", emojiHint: String = ""): FoodCategoryTheme {
        val upperCat = categoryHint.trim().uppercase(Locale.ROOT)
        if (upperCat.isNotBlank()) {
            THEMES[upperCat]?.let {
                return if (emojiHint.isNotBlank()) it.copy(defaultEmoji = emojiHint.trim()) else it
            }
        }

        val lowerName = name.lowercase(Locale.GERMAN)
        val matchedTheme = when {
            lowerName.contains("pizza") || lowerName.contains("flammkuchen") || lowerName.contains("calzone") ->
                THEMES["PIZZA"]
            lowerName.contains("spaghetti") || lowerName.contains("pasta") || lowerName.contains("nudel") ||
                    lowerName.contains("lasagne") || lowerName.contains("penne") || lowerName.contains("tortellini") ||
                    lowerName.contains("macaroni") ->
                THEMES["PASTA"]
            lowerName.contains("burger") || lowerName.contains("pommes") || lowerName.contains("fries") ||
                    lowerName.contains("fastfood") || lowerName.contains("nugget") || lowerName.contains("hotdog") ->
                THEMES["BURGER"]
            lowerName.contains("salat") || lowerName.contains("salad") || lowerName.contains("gemüse") ||
                    lowerName.contains("brokkoli") || lowerName.contains("spinat") || lowerName.contains("gurke") ||
                    lowerName.contains("tomate") || lowerName.contains("vegan") || lowerName.contains("vegetar") ->
                THEMES["SALAD"]
            lowerName.contains("apfel") || lowerName.contains("banane") || lowerName.contains("orange") ||
                    lowerName.contains("beere") || lowerName.contains("erdbeer") || lowerName.contains("obst") ||
                    lowerName.contains("frucht") || lowerName.contains("trauben") || lowerName.contains("mango") ||
                    lowerName.contains("ananas") || lowerName.contains("avocado") ->
                THEMES["FRUIT"]
            lowerName.contains("fisch") || lowerName.contains("lachs") || lowerName.contains("thunfisch") ||
                    lowerName.contains("forelle") || lowerName.contains("garnele") || lowerName.contains("shrimp") ||
                    lowerName.contains("sushi") || lowerName.contains("kabeljau") || lowerName.contains("seafood") ->
                THEMES["FISH"]
            lowerName.contains("steak") || lowerName.contains("schnitzel") || lowerName.contains("fleisch") ||
                    lowerName.contains("rind") || lowerName.contains("hähnchen") || lowerName.contains("huhn") ||
                    lowerName.contains("chicken") || lowerName.contains("pute") || lowerName.contains("wurst") ||
                    lowerName.contains("schwein") || lowerName.contains("bacon") || lowerName.contains("gulasch") ->
                THEMES["MEAT"]
            lowerName.contains("kaffee") || lowerName.contains("cappuccino") || lowerName.contains("espresso") ||
                    lowerName.contains("latte") || lowerName.contains("tee") || lowerName.contains("tea") ||
                    lowerName.contains("matcha") || lowerName.contains("kakao") ->
                THEMES["COFFEE"]
            lowerName.contains("wasser") || lowerName.contains("saft") || lowerName.contains("cola") ||
                    lowerName.contains("smoothie") || lowerName.contains("shake") || lowerName.contains("limonade") ||
                    lowerName.contains("bier") || lowerName.contains("wein") || lowerName.contains("drink") ||
                    lowerName.contains("cocktail") ->
                THEMES["DRINK"]
            lowerName.contains("müsli") || lowerName.contains("haferflocken") || lowerName.contains("oats") ||
                    lowerName.contains("porridge") || lowerName.contains("granola") ->
                THEMES["BREAKFAST"]
            lowerName.contains("spiegelei") || lowerName.contains("rührei") || lowerName.contains("omelett") ||
                    lowerName.contains("gekochtes ei") || lowerName.contains("eierspeise") ||
                    Regex("\\b(ei|eier)\\b").containsMatchIn(lowerName) ->
                THEMES["EGG"]
            lowerName.contains("brot") || lowerName.contains("brötchen") || lowerName.contains("sandwich") ||
                    lowerName.contains("toast") || lowerName.contains("bagel") || lowerName.contains("croissant") ||
                    lowerName.contains("semmel") || lowerName.contains("stulle") || lowerName.contains("brezel") ->
                THEMES["BREAD"]
            lowerName.contains("suppe") || lowerName.contains("eintopf") || lowerName.contains("ramen") ||
                    lowerName.contains("brühe") || lowerName.contains("pho") || lowerName.contains("chili con carne") ->
                THEMES["SOUP"]
            lowerName.contains("eiscreme") || lowerName.contains("ice cream") ||
                    lowerName.contains("sorbet") || lowerName.contains("gelato") ||
                    Regex("\\beis\\b").containsMatchIn(lowerName) ->
                THEMES["ICE_CREAM"]
            lowerName.contains("kuchen") || lowerName.contains("torte") || lowerName.contains("dessert") ||
                    lowerName.contains("schokolade") || lowerName.contains("muffin") || lowerName.contains("donut") ||
                    lowerName.contains("keks") || lowerName.contains("cookie") || lowerName.contains("waffel") ||
                    lowerName.contains("pancake") || lowerName.contains("pfannkuchen") ->
                THEMES["DESSERT"]
            lowerName.contains("reis") || lowerName.contains("döner") || lowerName.contains("kebab") ||
                    lowerName.contains("curry") || lowerName.contains("wok") || lowerName.contains("asiatisch") ||
                    lowerName.contains("nudelpfanne") || lowerName.contains("biryani") || lowerName.contains("paella") ->
                THEMES["ASIAN_RICE"]
            else -> THEMES["DEFAULT"]
        } ?: THEMES["DEFAULT"]!!

        return if (emojiHint.isNotBlank()) {
            matchedTheme.copy(defaultEmoji = emojiHint.trim())
        } else {
            matchedTheme
        }
    }

    /**
     * Generates a high-quality 512x512 symbolic image, saves it to internal app storage,
     * and returns its absolute file path.
     */
    suspend fun generateSymbolicFoodImage(
        context: Context,
        name: String,
        categoryHint: String = "",
        emojiHint: String = "",
        healthRating: HealthRating = HealthRating.YELLOW
    ): String = withContext(Dispatchers.IO) {
        generateSymbolicFoodImageSync(context, name, categoryHint, emojiHint, healthRating)
    }

    fun generateSymbolicFoodImageSync(
        context: Context,
        name: String,
        categoryHint: String = "",
        emojiHint: String = "",
        healthRating: HealthRating = HealthRating.YELLOW
    ): String {
        val theme = resolveTheme(name, categoryHint, emojiHint)
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background Gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                theme.startColor, theme.endColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // 2. Subtle Radial Light Accent (soft center spotlight)
        val radialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                size / 2f, size * 0.44f, size * 0.45f,
                0x35FFFFFF.toInt(), 0x00FFFFFF,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), radialPaint)

        // 3. Ambient Decorative Circles
        val decoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0x12FFFFFF
        }
        canvas.drawCircle(size * 0.85f, size * 0.15f, 130f, decoPaint)
        decoPaint.color = 0x0E000000
        canvas.drawCircle(size * 0.12f, size * 0.88f, 160f, decoPaint)

        // 4. Subtle Drop Shadow under Emoji
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x24000000
        }
        val shadowRect = RectF(
            size / 2f - 100f, size * 0.54f,
            size / 2f + 100f, size * 0.62f
        )
        canvas.drawOval(shadowRect, shadowPaint)

        // 5. Large Centered Emoji
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 175f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
        // Center emoji vertically around y = 240
        val emojiY = 240f - (emojiPaint.descent() + emojiPaint.ascent()) / 2f
        canvas.drawText(theme.defaultEmoji, size / 2f, emojiY, emojiPaint)

        // 6. Translucent Pill Badge with Category Name at bottom
        val labelText = theme.label
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = 0xFFFFFFFF.toInt()
            setShadowLayer(4f, 0f, 2f, 0x50000000)
        }

        val textWidth = textPaint.measureText(labelText)
        val pillPaddingX = 36f
        val pillWidth = (textWidth + pillPaddingX * 2).coerceIn(140f, size * 0.85f)
        val pillHeight = 52f
        val pillTop = size * 0.76f
        val pillBottom = pillTop + pillHeight
        val pillLeft = (size - pillWidth) / 2f
        val pillRight = pillLeft + pillWidth
        val pillRect = RectF(pillLeft, pillTop, pillRight, pillBottom)

        // Pill background
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x36000000
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, pillBgPaint)

        // Pill outline border
        val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x30FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, pillBorderPaint)

        // Pill text
        val pillTextY = pillTop + (pillHeight / 2f) - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(labelText, size / 2f, pillTextY, textPaint)

        // 7. Save to internal app storage
        val imagesDir = File(context.filesDir, "food_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        val imageFile = File(imagesDir, "symbolic_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        bitmap.recycle()

        return imageFile.absolutePath
    }
}
