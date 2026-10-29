package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Requirement R05 Verification:
 * Monochrome Category Filter Pills.
 *
 * Verifies:
 * 1. Night selected pill: EXACT background #FFFFFF, selected text/icons #000000.
 * 2. Day selected pill: EXACT background #000000, selected text/icons #FFFFFF.
 * 3. Unselected pills: quiet theme-neutral surfaces with WCAG AA compliant text (>= 4.5:1).
 * 4. Chip geometry: isCheckedIconVisible = false, no growth on check, singleSelection and selectionRequired.
 * 5. Single selection lifecycle: no-op on re-tapping selected, fallback to All on disappeared category.
 */
class HomeCategoryChipContractTest {

    private fun loadColors(relativePath: String): Map<String, String> {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("File not found: $relativePath")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("color")
        val map = mutableMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            map[element.getAttribute("name")] = element.textContent.trim().uppercase()
        }
        return map
    }

    private fun parseColor(hex: String): Int {
        val clean = hex.removePrefix("#")
        return when (clean.length) {
            6 -> (0xFF shl 24) or clean.toInt(16)
            8 -> clean.toLong(16).toInt()
            else -> throw IllegalArgumentException("Invalid color: $hex")
        }
    }

    private fun luminance(colorInt: Int): Double {
        fun channel(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        val r = channel((colorInt ushr 16) and 0xFF)
        val g = channel((colorInt ushr 8) and 0xFF)
        val b = channel(colorInt and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrast(c1: Int, c2: Int): Double {
        val l1 = luminance(c1)
        val l2 = luminance(c2)
        val high = maxOf(l1, l2)
        val low = minOf(l1, l2)
        return (high + 0.05) / (low + 0.05)
    }

    @Test
    fun testDayMonochromeTokens() {
        val colors = loadColors("src/main/res/values/telegram_colors.xml")
        val selectedBg = colors["category_pill_selected_bg"]
        val selectedFg = colors["category_pill_selected_fg"]
        val unselectedBg = colors["category_pill_unselected_bg"]
        val unselectedFg = colors["category_pill_unselected_fg"]

        assertEquals("DAY selected background must be EXACT #000000", "#FF000000", selectedBg)
        assertEquals("DAY selected foreground must be EXACT #FFFFFF", "#FFFFFFFF", selectedFg)

        val selectedContrast = contrast(parseColor(selectedBg!!), parseColor(selectedFg!!))
        assertEquals("DAY selected contrast must be 21:1", 21.0, selectedContrast, 0.01)

        val unselectedContrast = contrast(parseColor(unselectedBg!!), parseColor(unselectedFg!!))
        assertTrue("DAY unselected contrast must meet WCAG AA (>= 4.5): $unselectedContrast", unselectedContrast >= 4.5)
    }

    @Test
    fun testNightMonochromeTokens() {
        val colors = loadColors("src/main/res/values-night/telegram_colors.xml")
        val selectedBg = colors["category_pill_selected_bg"]
        val selectedFg = colors["category_pill_selected_fg"]
        val unselectedBg = colors["category_pill_unselected_bg"]
        val unselectedFg = colors["category_pill_unselected_fg"]

        assertEquals("NIGHT selected background must be EXACT #FFFFFF", "#FFFFFFFF", selectedBg)
        assertEquals("NIGHT selected foreground must be EXACT #000000", "#FF000000", selectedFg)

        val selectedContrast = contrast(parseColor(selectedBg!!), parseColor(selectedFg!!))
        assertEquals("NIGHT selected contrast must be 21:1", 21.0, selectedContrast, 0.01)

        val unselectedContrast = contrast(parseColor(unselectedBg!!), parseColor(unselectedFg!!))
        assertTrue("NIGHT unselected contrast must meet WCAG AA (>= 4.5): $unselectedContrast", unselectedContrast >= 4.5)
    }

    @Test
    fun testDesignSystemCategoryChipContract() {
        val candidates = listOf(
            File("app/src/main/java/com/eve/app/designsystem/EveDesignSystem.kt"),
            File("src/main/java/com/eve/app/designsystem/EveDesignSystem.kt")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("EveDesignSystem.kt not found")
        val content = file.readText()

        assertTrue(
            "EveDesignSystem must bind category_pill_selected_bg",
            content.contains("R.color.category_pill_selected_bg")
        )
        assertTrue(
            "EveDesignSystem must bind category_pill_unselected_bg",
            content.contains("R.color.category_pill_unselected_bg")
        )
        assertTrue(
            "EveDesignSystem must bind category_pill_selected_fg",
            content.contains("R.color.category_pill_selected_fg")
        )
        assertTrue(
            "EveDesignSystem must bind category_pill_unselected_fg",
            content.contains("R.color.category_pill_unselected_fg")
        )
        assertTrue(
            "EveDesignSystem must tint chip icons to match text foreground",
            content.contains("chipIconTint =")
        )
        assertTrue(
            "EveDesignSystem must disable checked icon to preserve width stability",
            content.contains("isCheckedIconVisible = false")
        )
    }

    @Test
    fun testChipGroupLayoutSingleSelectionAttributes() {
        val candidates = listOf(
            File("app/src/main/res/layout/activity_main.xml"),
            File("src/main/res/layout/activity_main.xml")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("activity_main.xml not found")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val chipGroup = doc.getElementById("chipGroupCategory")
            ?: (0 until doc.getElementsByTagName("com.eve.app.ui.home.HomeCategoryChipGroup").length)
                .map { doc.getElementsByTagName("com.eve.app.ui.home.HomeCategoryChipGroup").item(it) as Element }
                .firstOrNull { it.getAttribute("android:id").endsWith("chipGroupCategory") }

        org.junit.Assert.assertNotNull("chipGroupCategory must exist", chipGroup)
        assertEquals("true", chipGroup?.getAttribute("app:singleSelection"))
        assertEquals("true", chipGroup?.getAttribute("app:selectionRequired"))
        assertEquals("true", chipGroup?.getAttribute("app:singleLine"))
    }

    @Test
    fun testSingleSelectionSafetyContract() {
        val all = "All"
        val categories = listOf(all, "General Studies", "Math", "Hindi")

        // Re-tapping selected is a no-op
        val current = "Math"
        val same = HomeCategorySelection.select(current, "Math", categories)
        assertEquals("Re-tapping Math must return Math", "Math", same)

        // Switching All <-> Other category directly
        val toAll = HomeCategorySelection.select("Math", all, categories)
        assertEquals(all, toAll)
        val toHindi = HomeCategorySelection.select(all, "Hindi", categories)
        assertEquals("Hindi", toHindi)

        // Disappeared category falls back to All
        val reducedCategories = listOf(all, "General Studies", "Math")
        val fallback = HomeCategorySelection.resolve("Hindi", reducedCategories, all)
        assertEquals("Disappeared category must fall back to All", all, fallback)
    }
}
