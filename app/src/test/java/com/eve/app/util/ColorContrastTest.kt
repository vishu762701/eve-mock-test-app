package com.eve.app.util

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG 2.1 Contrast Compliance Verification Suite:
 * - Normal text requires a minimum contrast ratio of 4.5:1 (Level AA)
 * - Large text and UI components/icons require a minimum contrast ratio of 3.0:1 (Level AA)
 *
 * Verifies all color tokens across both Light and Dark themes.
 */
class ColorContrastTest {

    private fun parseHex(hex: String): Int {
        val clean = hex.removePrefix("#")
        val parsed = clean.toLong(16).toInt()
        return if (clean.length <= 6) {
            (0xFF shl 24) or (parsed and 0xFFFFFF)
        } else {
            parsed
        }
    }

    private fun red(c: Int): Int = (c shr 16) and 0xFF
    private fun green(c: Int): Int = (c shr 8) and 0xFF
    private fun blue(c: Int): Int = c and 0xFF

    private fun channelLuminance(value8Bit: Int): Double {
        val s = value8Bit / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun relativeLuminance(colorHex: String): Double {
        val c = parseHex(colorHex)
        val r = channelLuminance(red(c))
        val g = channelLuminance(green(c))
        val b = channelLuminance(blue(c))
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(fgHex: String, bgHex: String): Double {
        val l1 = relativeLuminance(fgHex)
        val l2 = relativeLuminance(bgHex)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    // Top Bar Contrast Tests
    @Test
    fun testTimerNormal_againstTopBar_lightAndDarkThemes() {
        val lightTopBarBg = "#111111"
        val lightTimerNormal = "#FFFFFF"
        val lightRatio = contrastRatio(lightTimerNormal, lightTopBarBg)
        assertTrue("Light timer_normal must exceed 4.5:1 on top bar (actual: $lightRatio)", lightRatio >= 4.5)

        val darkTopBarBg = "#FFFFFF"
        val darkTimerNormal = "#111111"
        val darkRatio = contrastRatio(darkTimerNormal, darkTopBarBg)
        assertTrue("Dark timer_normal must exceed 4.5:1 on top bar (actual: $darkRatio)", darkRatio >= 4.5)
    }

    @Test
    fun testTimerWarning_againstTopBar_lightAndDarkThemes() {
        val lightTopBarBg = "#111111"
        val lightTimerWarning = "#FF5252"
        val lightRatio = contrastRatio(lightTimerWarning, lightTopBarBg)
        assertTrue("Light timer_warning must exceed 4.5:1 on top bar (actual: $lightRatio)", lightRatio >= 4.5)

        val darkTopBarBg = "#FFFFFF"
        val darkTimerWarning = "#C62828"
        val darkRatio = contrastRatio(darkTimerWarning, darkTopBarBg)
        assertTrue("Dark timer_warning must exceed 4.5:1 on top bar (actual: $darkRatio)", darkRatio >= 4.5)
    }

    @Test
    fun testLanguageAndSubmitButtons_againstTopBar_lightAndDarkThemes() {
        // HI text button
        val lightTopBarBg = "#111111"
        val lightHiText = "#FFFFFF"
        assertTrue(contrastRatio(lightHiText, lightTopBarBg) >= 4.5)

        val darkTopBarBg = "#FFFFFF"
        val darkHiText = "#000000"
        assertTrue(contrastRatio(darkHiText, darkTopBarBg) >= 4.5)

        // Submit Button: on_primary bg with primary text
        val lightSubmitBg = "#FFFFFF"
        val lightSubmitText = "#111111"
        assertTrue(contrastRatio(lightSubmitText, lightSubmitBg) >= 4.5)

        val darkSubmitBg = "#000000"
        val darkSubmitText = "#FFFFFF"
        assertTrue(contrastRatio(darkSubmitText, darkSubmitBg) >= 4.5)
    }

    // Feedback Token Contrast Tests
    @Test
    fun testSuccessToken_againstSurfaces() {
        val lightBg = "#FFFFFF"
        val lightSuccess = "#1B5E20"
        val lightRatio = contrastRatio(lightSuccess, lightBg)
        assertTrue("Light success token must be WCAG AA compliant (actual: $lightRatio)", lightRatio >= 4.5)

        val darkSurface = "#111111"
        val darkSuccess = "#81C784"
        val darkRatio = contrastRatio(darkSuccess, darkSurface)
        assertTrue("Dark success token must be WCAG AA compliant (actual: $darkRatio)", darkRatio >= 4.5)
    }

    @Test
    fun testErrorToken_againstSurfaces() {
        val lightBg = "#FFFFFF"
        val lightError = "#B71C1C"
        val lightRatio = contrastRatio(lightError, lightBg)
        assertTrue("Light error token must be WCAG AA compliant (actual: $lightRatio)", lightRatio >= 4.5)

        val darkSurface = "#111111"
        val darkError = "#FF8A80"
        val darkRatio = contrastRatio(darkError, darkSurface)
        assertTrue("Dark error token must be WCAG AA compliant (actual: $darkRatio)", darkRatio >= 4.5)
    }

    @Test
    fun testWarningToken_againstSurfaces() {
        val lightBg = "#FFFFFF"
        val lightWarning = "#B45309"
        val lightRatio = contrastRatio(lightWarning, lightBg)
        assertTrue("Light warning token must be WCAG AA compliant (actual: $lightRatio)", lightRatio >= 4.5)

        val darkSurface = "#111111"
        val darkWarning = "#FBBF24"
        val darkRatio = contrastRatio(darkWarning, darkSurface)
        assertTrue("Dark warning token must be WCAG AA compliant (actual: $darkRatio)", darkRatio >= 4.5)
    }

    @Test
    fun testCautionToken_againstSurfaces() {
        val lightBg = "#FFFFFF"
        val lightCaution = "#C2410C"
        val lightRatio = contrastRatio(lightCaution, lightBg)
        assertTrue("Light caution token must be WCAG AA compliant (actual: $lightRatio)", lightRatio >= 4.5)

        val darkSurface = "#111111"
        val darkCaution = "#FB923C"
        val darkRatio = contrastRatio(darkCaution, darkSurface)
        assertTrue("Dark caution token must be WCAG AA compliant (actual: $darkRatio)", darkRatio >= 4.5)
    }

    // Icon Contrast Tests (WCAG 3.0:1 requirement)
    @Test
    fun testStarAndFlagIcons_againstSurfaces() {
        val lightSurface = "#FFFFFF"
        val lightIconTint = "#111111"
        val lightRatio = contrastRatio(lightIconTint, lightSurface)
        assertTrue("Star/flag icon in light mode must exceed 3:1 (actual: $lightRatio)", lightRatio >= 3.0)

        val darkSurface = "#111111"
        val darkIconTint = "#F5F5F5"
        val darkRatio = contrastRatio(darkIconTint, darkSurface)
        assertTrue("Star/flag icon in dark mode must exceed 3:1 (actual: $darkRatio)", darkRatio >= 3.0)
    }
}
