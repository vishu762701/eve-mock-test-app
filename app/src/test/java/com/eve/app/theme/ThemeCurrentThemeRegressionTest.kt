package com.eve.app.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Permanent regression test for the SettingsActivity launch crash caused by
 * Theme.currentTheme remaining null after Theme.cut stripped applyTheme(...)
 * from Theme's static initializer, causing Theme.isCurrentThemeDark() -> NPE
 * inside TextCell.setColorfulIcon() (TextCell.java:578).
 */
class ThemeCurrentThemeRegressionTest {

    @Test
    fun themeStaticInitAndIsCurrentThemeDarkMustNotLeaveCurrentThemeNull() {
        val candidates = listOf(
            File("src/main/java/org/telegram/ui/ActionBar/Theme.java"),
            File("app/src/main/java/org/telegram/ui/ActionBar/Theme.java")
        )
        val themeFile = candidates.firstOrNull { it.isFile }
            ?: error("Theme.java not found in expected module paths")
        val content = themeFile.readText()

        assertTrue(
            "Theme.java static block must initialize currentTheme to defaultTheme",
            content.contains("currentTheme = currentDayTheme = defaultTheme = themeInfo")
        )
        assertTrue(
            "Theme.isCurrentThemeDark() must guard against null currentTheme",
            content.contains("currentTheme != null ? currentTheme.isDark()")
        )
        assertTrue(
            "Theme.syncFromEvePalette() must synchronize currentTheme with Eve palette",
            content.contains("if (currentTheme == null)")
        )
    }
}
