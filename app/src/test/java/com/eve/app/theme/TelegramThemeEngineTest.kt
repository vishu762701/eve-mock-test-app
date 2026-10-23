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
}
