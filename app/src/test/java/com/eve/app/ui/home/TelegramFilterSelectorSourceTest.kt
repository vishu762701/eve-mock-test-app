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

    @Test fun originalSelectorGeometryAndDayNightRoleResolutionAreWired() {
        val group = File("src/main/java/com/eve/app/ui/home/HomeCategoryChipGroup.kt").readText()
        val design = File("src/main/java/com/eve/app/designsystem/EveDesignSystem.kt").readText()
        val theme = File("src/main/java/com/eve/app/theme/TelegramThemeEngine.kt").readText()
        assertTrue(group.contains("val radius = dp(14f)"))
        assertTrue(group.contains("val y = height / 2f - dp(14f)"))
        assertTrue(group.contains("val internalPadding = dp(12.5f)"))
        assertTrue(group.contains("(y + dp(28f)).toInt()"))
        assertTrue(group.contains("selectorDrawable.alpha = 31"))
        assertTrue(group.contains("Layout.getDesiredWidth(selected.text, selected.paint)"))
        assertTrue(group.contains("setOnCheckedStateChangeListener { _, _ -> invalidate() }"))
        assertTrue(design.contains("chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)"))
        assertTrue(design.contains("palette.filterTabActiveText"))
        assertTrue(design.contains("palette.filterTabUnactiveText"))
        assertTrue(theme.contains("filterTabLine = role(\"actionBarTabLine\")"))
        assertTrue(theme.contains("filterTabActiveText = role(\"actionBarTabActiveText\")"))
        assertTrue(theme.contains("filterTabUnactiveText = role(\"actionBarTabUnactiveText\")"))
    }
}
