package com.eve.app.ui.home

import com.eve.app.theme.TelegramColorRoles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Verifies the original Telegram selector properties independent of EVE's Chip host. */
class TelegramFilterSelectorSourceTest {
    @Test fun exactSourceDefaultTabColorsArePreserved() {
        assertEquals(0xFF298ACF.toInt(), TelegramColorRoles.defaults["actionBarTabLine"])
        assertEquals(0xFF298ACF.toInt(), TelegramColorRoles.defaults["actionBarTabActiveText"])
        assertEquals(0xFF777C7F.toInt(), TelegramColorRoles.defaults["actionBarTabUnactiveText"])
        assertEquals(0x121A1D21, TelegramColorRoles.defaults["actionBarTabSelector"])
    }

    @Test fun originalRawDayTabTextRequiresAaAccommodationOnWhite() {
        fun luminance(color: Int): Double {
            fun channel(value: Int): Double {
                val c = value / 255.0
                return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * channel((color ushr 16) and 255) +
                0.7152 * channel((color ushr 8) and 255) +
                0.0722 * channel(color and 255)
        }
        val white = luminance(0xFFFFFFFF.toInt())
        val active = luminance(TelegramColorRoles.defaults.getValue("actionBarTabActiveText"))
        val inactive = luminance(TelegramColorRoles.defaults.getValue("actionBarTabUnactiveText"))
        assertTrue("Original Telegram Day active blue needs text AA correction",
            (white + 0.05) / (active + 0.05) < 4.5)
        assertTrue("Original Telegram Day inactive gray needs text AA correction",
            (white + 0.05) / (inactive + 0.05) < 4.5)
    }

    @Test fun exactUpstreamQuintInterpolatorAnd320msFrameContractArePreserved() {
        val source = File("src/main/java/org/telegram/ui/Components/CubicBezierInterpolator.java").readBytes()
        val digest = java.security.MessageDigest.getInstance("SHA-1")
        digest.update("blob ${source.size}\\u0000".replace("\\u0000", "\u0000").toByteArray(Charsets.UTF_8))
        digest.update(source)
        val sha = digest.digest().joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        assertEquals("3a9cae100e17a03abacf2f4df71db2b82757b968", sha)
        val group = File("src/main/java/com/eve/app/ui/home/HomeCategoryChipGroup.kt").readText()
        assertTrue(group.contains("CubicBezierInterpolator.EASE_OUT_QUINT"))
        assertTrue(group.contains("SystemClock.elapsedRealtime()"))
        assertTrue(group.contains("coerceIn(0L, 17L)"))
        assertTrue(group.contains("animationTime += dt / 320.0f"))
        assertTrue(group.contains("postOnAnimation(indicatorAnimation)"))
        assertTrue(group.contains("removeCallbacks(indicatorAnimation)"))
        assertTrue(group.contains("!UiArcMotion.isMotionEnabled(context)"))
    }

    @Test fun originalSelectorGeometryAndDayNightRoleResolutionAreWired() {
        val group = File("src/main/java/com/eve/app/ui/home/HomeCategoryChipGroup.kt").readText()
        val design = File("src/main/java/com/eve/app/designsystem/EveDesignSystem.kt").readText()
        val theme = File("src/main/java/com/eve/app/theme/TelegramThemeEngine.kt").readText()
        assertTrue(group.contains("val radius = dp(14f)"))
        assertTrue(group.contains("val y = height / 2f - dp(14f)"))
        assertTrue(group.contains("val padding = dp(12.5f)"))
        assertTrue(group.contains("return RectF(x - padding, y, x + indicatorWidth + padding, y + dp(28f))"))
        assertTrue(group.contains("selectorDrawable.setBounds("))
        assertTrue(group.contains("shown.left.toInt()") && group.contains("shown.bottom.toInt()"))
        assertTrue(group.contains("selectorDrawable.alpha = 31"))
        assertTrue(group.contains("Layout.getDesiredWidth(selected.text, selected.paint)"))
        assertTrue(group.contains("setOnCheckedStateChangeListener { _, _ -> selectedCategoryChanged() }"))
        assertTrue(design.contains("R.color.category_pill_selected_bg"))
        assertTrue(design.contains("R.color.category_pill_unselected_bg"))
        assertTrue(design.contains("R.color.category_pill_selected_fg"))
        assertTrue(design.contains("R.color.category_pill_unselected_fg"))
        assertTrue(theme.contains("val rawFilterTabLine = role(\"actionBarTabLine\")"))
        assertTrue(theme.contains("filterTabLine = rawFilterTabLine"))
        assertTrue(theme.contains("filterTabActiveText = accessibleFilterTabActiveText"))
        assertTrue(theme.contains("filterTabUnactiveText = accessibleFilterTabUnactiveText"))
        assertTrue(theme.contains("ColorUtils.blendARGB(surface, rawFilterTabLine, 31f / 255f)"))
        assertTrue(theme.contains("TelegramColorRoles.accessibleForeground("))
        assertTrue(theme.contains("role(\"actionBarTabActiveText\")"))
        assertTrue(theme.contains("role(\"actionBarTabUnactiveText\")"))
    }
}
