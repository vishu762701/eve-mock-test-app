package com.eve.app.theme

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelegramThemeEngineTest {

    @Test
    fun importedThemeIsRegisteredWithStableIdAndResolvablePalette() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val file = java.io.File(context.filesDir, "custom_${System.nanoTime().toString().replace("-", "")}.attheme")
        try {
            file.writeText(
                "windowBackgroundGray=-1\n" +
                    "windowBackgroundWhite=-1\n" +
                    "windowBackgroundWhiteBlackText=#FF112233\n" +
                    "chat_messagePanelSend=-14509328\n"
            )
            val registered = TelegramThemeEngine.registerCustomTheme(context, file)
            assertNotNull("Imported file must be registered", registered)
            val theme = registered!!
            assertEquals(file.name.removeSuffix(".attheme"), theme.id)
            assertEquals(theme, TelegramThemeEngine.getTheme(theme.id))
            assertTrue(TelegramThemeEngine.getAllThemes().any { it.id == theme.id })
            val palette = TelegramThemeEngine.resolvePalette(context, theme, 0)
            assertEquals(0xFF112233.toInt(), palette.textPrimary)
        } finally {
            file.delete()
        }
    }

    @Test
    fun allBuiltInAccentForegroundsMeetNormalTextContrast() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        for (theme in TelegramThemeEngine.BUILT_IN_THEMES) {
            for (accent in theme.accentOptions) {
                val palette = TelegramThemeEngine.resolvePalette(context, theme, accent.id)
                val ratio = androidx.core.graphics.ColorUtils.calculateContrast(palette.onAccent, palette.accent)
                assertTrue("${theme.id}/${accent.name} contrast ratio: $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun testAtthemeStreamParser() {
        val sample = """
            // Comment line
            windowBackgroundGray=-16777216
            windowBackgroundWhite=-15198183
            windowBackgroundWhiteBlackText=-1
            chat_messagePanelSend=-14509328
            glass_targetMainTabs=-14474460
            hexColor=#FF229AF0
        """.trimIndent()

        val map = mutableMapOf<String, Int>()
        TelegramThemeEngine.parseAtthemeStream(
            ByteArrayInputStream(sample.toByteArray(StandardCharsets.UTF_8)),
            map
        )

        assertEquals(-16777216, map["windowBackgroundGray"])
        assertEquals(-15198183, map["windowBackgroundWhite"])
        assertEquals(-1, map["windowBackgroundWhiteBlackText"])
        assertEquals(-14509328, map["chat_messagePanelSend"])
        assertEquals(-14474460, map["glass_targetMainTabs"])
        assertEquals(0xFF229AF0.toInt(), map["hexColor"])
    }

    @Test
    fun testDefaultHousePresetsMatchTelegramSource() {
        // Day theme: House preset uses Day with accent 9 (0xFF56A2C9)
        val dayTheme = TelegramThemeEngine.BUILT_IN_THEMES.first { it.id == "day" }
        assertEquals(9, dayTheme.defaultAccentId)
        val dayAccent9 = dayTheme.accentOptions.first { it.id == 9 }
        assertEquals(0xFF56A2C9.toInt(), dayAccent9.color)

        // Night theme: House preset uses Night with accent 0 (0xFF3E88F7)
        val nightTheme = TelegramThemeEngine.BUILT_IN_THEMES.first { it.id == "night" }
        assertEquals(0, nightTheme.defaultAccentId)
        val nightAccent0 = nightTheme.accentOptions.first { it.id == 0 }
        assertEquals(0xFF3E88F7.toInt(), nightAccent0.color)
    }

    @Test
    fun testSolveSrcColor() {
        // Zero alpha returns 0
        assertEquals(0, TelegramThemeEngine.solveSrcColor(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0f))

        // 1.0 alpha returns outColor with full alpha
        val full = TelegramThemeEngine.solveSrcColor(0xFF000000.toInt(), 0xFF123456.toInt(), 1f)
        assertEquals(0xFF123456.toInt(), full)

        // Mid alpha computation
        val res = TelegramThemeEngine.solveSrcColor(0xFFFFFFFF.toInt(), 0xFF181819.toInt(), 0.76f)
        assertTrue(Color.alpha(res) > 0)
    }

    @Test
    fun testMultAlpha() {
        val color = 0xFF804020.toInt()
        val half = TelegramThemeEngine.multAlpha(color, 0.5f)
        assertEquals(128, Color.alpha(half))
        assertEquals(Color.red(color), Color.red(half))
        assertEquals(Color.green(color), Color.green(half))
        assertEquals(Color.blue(color), Color.blue(half))
    }

    @Test
    fun testChangeColorAccentFallbackWhenHueDiffTooLarge() {
        val baseHsv = floatArrayOf(200f, 0.6f, 0.8f)
        val accentHsv = floatArrayOf(210f, 0.7f, 0.9f)
        val distantColor = 0xFFFF0000.toInt() // Red (hue 0), diff from 200 is > 30
        val fallback = 0xFF123456.toInt()

        val result = TelegramThemeEngine.changeColorAccent(baseHsv, accentHsv, distantColor, false, fallback)
        assertEquals(fallback, result)
    }

    @Test
    fun testChangeColorAccentTransformsMatchingHue() {
        val baseHsv = floatArrayOf(200f, 0.6f, 0.8f)
        val accentHsv = floatArrayOf(210f, 0.7f, 0.9f)
        val nearColor = Color.HSVToColor(floatArrayOf(205f, 0.5f, 0.8f))
        val fallback = 0xFF123456.toInt()

        val result = TelegramThemeEngine.changeColorAccent(baseHsv, accentHsv, nearColor, false, fallback)
        assertTrue("Expected transformed color, not fallback", result != fallback)
    }

    @Test
    fun resetPreviewCanBeCancelledWithoutPersistingDefaults() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        TelegramThemeEngine.init(context)
        val previousDay = TelegramThemeEngine.getDayThemeId()
        val previousDayAccent = TelegramThemeEngine.getDayAccentId()
        val previousNight = TelegramThemeEngine.getNightThemeId()
        val previousNightAccent = TelegramThemeEngine.getNightAccentId()
        val previousMode = TelegramThemeEngine.getThemeMode()
        try {
            TelegramThemeEngine.beginPreview()
            TelegramThemeEngine.setPreviewDayTheme("arctic", 0, context)
            TelegramThemeEngine.applyChanges(context)
            assertEquals("arctic", TelegramThemeEngine.getDayThemeId())

            TelegramThemeEngine.beginPreview()
            TelegramThemeEngine.resetToDefaults(context)
            assertEquals("day", TelegramThemeEngine.getDayThemeId())
            TelegramThemeEngine.cancelPreview(context)

            assertEquals("arctic", TelegramThemeEngine.getDayThemeId())
            val prefs = context.getSharedPreferences(TelegramThemeEngine.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            assertEquals("arctic", prefs.getString("day_theme_id", null))
        } finally {
            TelegramThemeEngine.beginPreview()
            TelegramThemeEngine.setPreviewDayTheme(previousDay, previousDayAccent, context)
            TelegramThemeEngine.setPreviewNightTheme(previousNight, previousNightAccent, context)
            TelegramThemeEngine.setPreviewMode(previousMode, context)
            TelegramThemeEngine.applyChanges(context)
        }
    }

    @Test
    fun testFallbackKeyResolution() {
        val colors = mapOf(
            "windowBackgroundWhite" to 0xFFEEDDCC.toInt()
        )
        // With only windowBackgroundWhite present:
        // dialogBackground falls back to windowBackgroundWhite
        // glass_targetMainTabs falls back to dialogBackground -> windowBackgroundWhite
        val targetColor = TelegramThemeEngine.FALLBACK_KEYS["glass_targetMainTabs"]
        assertEquals("dialogBackground", targetColor)
        val dialogBgFallback = TelegramThemeEngine.FALLBACK_KEYS["dialogBackground"]
        assertEquals("windowBackgroundWhite", dialogBgFallback)
    }

    @Test
    fun testParseColorValueValidation() {
        // Valid 32-bit hex
        assertEquals(0xFF112233.toInt(), TelegramThemeEngine.parseColorValue("#FF112233"))
        // Valid signed decimal
        assertEquals(-1, TelegramThemeEngine.parseColorValue("-1"))
        // Out of range (greater than 0xFFFFFFFF)
        assertEquals(null, TelegramThemeEngine.parseColorValue("4294967296"))
        // Invalid string
        assertEquals(null, TelegramThemeEngine.parseColorValue("not_a_color"))
    }

    @Test
    fun testRegisterCustomThemeRejectsUnrecognizedKeys() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val file = java.io.File(context.filesDir, "custom_${System.nanoTime().toString().replace("-", "")}.attheme")
        try {
            file.writeText("unknown_arbitrary_key=12345\nother_bogus_key=67890\n")
            val registered = TelegramThemeEngine.registerCustomTheme(context, file)
            assertEquals("Theme without recognized keys must be rejected", null, registered)
        } finally {
            file.delete()
        }
    }

    @Test
    fun testTransactionalPreviewLifecycle() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        TelegramThemeEngine.init(context)

        // Capture starting committed state
        val initialDayTheme = TelegramThemeEngine.getDayThemeId()
        val initialMode = TelegramThemeEngine.getThemeMode()

        // Begin preview
        TelegramThemeEngine.beginPreview()
        TelegramThemeEngine.setPreviewDayTheme("arctic", 1, context)
        TelegramThemeEngine.setPreviewMode(TelegramThemeEngine.ThemeMode.NIGHT, context)

        assertEquals("arctic", TelegramThemeEngine.getDayThemeId())
        assertEquals(TelegramThemeEngine.ThemeMode.NIGHT, TelegramThemeEngine.getThemeMode())

        // Cancel preview restores initial committed values
        TelegramThemeEngine.cancelPreview(context)
        assertEquals(initialDayTheme, TelegramThemeEngine.getDayThemeId())
        assertEquals(initialMode, TelegramThemeEngine.getThemeMode())

        // Begin preview and resetPreviewToDefaults
        TelegramThemeEngine.beginPreview()
        TelegramThemeEngine.resetPreviewToDefaults(context)
        assertEquals("day", TelegramThemeEngine.getDayThemeId())
        assertEquals(9, TelegramThemeEngine.getDayAccentId())
        assertEquals("night", TelegramThemeEngine.getNightThemeId())
        assertEquals(0, TelegramThemeEngine.getNightAccentId())

        // Cancel preview restores initial committed values even after resetPreviewToDefaults
        TelegramThemeEngine.cancelPreview(context)
        assertEquals(initialDayTheme, TelegramThemeEngine.getDayThemeId())
        assertEquals(initialMode, TelegramThemeEngine.getThemeMode())
    }

    @Test
    fun testSingleModeAuthoritySync() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        TelegramThemeEngine.init(context)

        TelegramThemeEngine.setCommittedMode(TelegramThemeEngine.ThemeMode.NIGHT, context)
        assertEquals(TelegramThemeEngine.ThemeMode.NIGHT, TelegramThemeEngine.getThemeMode())
        assertEquals(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES,
            androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode()
        )

        val tmPrefs = context.getSharedPreferences(com.eve.app.util.ThemeManager.PREFS, android.content.Context.MODE_PRIVATE)
        assertTrue(tmPrefs.getBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, false))

        // Reset to system
        TelegramThemeEngine.setCommittedMode(TelegramThemeEngine.ThemeMode.SYSTEM, context)
        assertEquals(TelegramThemeEngine.ThemeMode.SYSTEM, TelegramThemeEngine.getThemeMode())
        assertEquals(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode()
        )
    }
}
