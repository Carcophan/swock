package de.carcophan.swock

import de.carcophan.swock.util.SymbolicFoodImageGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SymbolicFoodImageGeneratorTest {

    @Test
    fun testCategoryHintResolution() {
        val theme = SymbolicFoodImageGenerator.resolveTheme("Unbekannt", categoryHint = "PASTA")
        assertEquals("PASTA", theme.categoryId)
        assertEquals("🍝", theme.defaultEmoji)
        assertEquals("Pasta & Nudeln", theme.label)
    }

    @Test
    fun testCategoryHintWithCustomEmoji() {
        val theme = SymbolicFoodImageGenerator.resolveTheme("Gemischter Salat", categoryHint = "SALAD", emojiHint = "🥑")
        assertEquals("SALAD", theme.categoryId)
        assertEquals("🥑", theme.defaultEmoji)
    }

    @Test
    fun testNameKeywordMatching() {
        val pasta = SymbolicFoodImageGenerator.resolveTheme("1 Portion Spaghetti Bolognese")
        assertEquals("PASTA", pasta.categoryId)
        assertEquals("🍝", pasta.defaultEmoji)

        val pizza = SymbolicFoodImageGenerator.resolveTheme("Pizza Margherita")
        assertEquals("PIZZA", pizza.categoryId)
        assertEquals("🍕", pizza.defaultEmoji)

        val burger = SymbolicFoodImageGenerator.resolveTheme("Doppelter Cheeseburger mit Pommes")
        assertEquals("BURGER", burger.categoryId)
        assertEquals("🍔", burger.defaultEmoji)

        val salad = SymbolicFoodImageGenerator.resolveTheme("Großer grüner Salat mit Dressing")
        assertEquals("SALAD", salad.categoryId)
        assertEquals("🥗", salad.defaultEmoji)

        val coffee = SymbolicFoodImageGenerator.resolveTheme("Großer Cappuccino mit Hafermilch")
        assertEquals("COFFEE", coffee.categoryId)
        assertEquals("☕", coffee.defaultEmoji)

        val egg = SymbolicFoodImageGenerator.resolveTheme("3 Rühreier mit Schnittlauch")
        assertEquals("EGG", egg.categoryId)
        assertEquals("🍳", egg.defaultEmoji)

        val fish = SymbolicFoodImageGenerator.resolveTheme("Gebratenes Lachsfilet")
        assertEquals("FISH", fish.categoryId)
        assertEquals("🍣", fish.defaultEmoji)

        val dessert = SymbolicFoodImageGenerator.resolveTheme("Ein Stück Schokoladenkuchen")
        assertEquals("DESSERT", dessert.categoryId)
        assertEquals("🍰", dessert.defaultEmoji)

        val defaultTheme = SymbolicFoodImageGenerator.resolveTheme("XYZ123 Speise")
        assertEquals("DEFAULT", defaultTheme.categoryId)
        assertEquals("🍽️", defaultTheme.defaultEmoji)
    }

    @Test
    fun testThemeColorsAreValid() {
        SymbolicFoodImageGenerator.THEMES.values.forEach { theme ->
            assertNotNull(theme.label)
            assertNotNull(theme.defaultEmoji)
            assertNotEquals(0, theme.startColor)
            assertNotEquals(0, theme.endColor)
            assertNotEquals(0, theme.accentColor)
        }
    }
}
