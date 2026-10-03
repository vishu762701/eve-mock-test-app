package com.eve.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class HomePanelWashDrawableTest {

    private fun channelLuminance(value8Bit: Int): Double {
        val s = value8Bit / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun relativeLuminance(colorInt: Int): Double {
        val r = channelLuminance((colorInt shr 16) and 0xFF)
        val g = channelLuminance((colorInt shr 8) and 0xFF)
        val b = channelLuminance(colorInt and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(colorA: Int, colorB: Int): Double {
        val l1 = relativeLuminance(colorA)
        val l2 = relativeLuminance(colorB)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testCornerSamples_matchGridExact() {
        val width = 100
        val height = 70
        val pixels = HomePanelWashDrawable.interpolateGridPixels(
            HomePanelWashDrawable.GRID_COLORS,
            width,
            height
        )

        val topLeft = pixels[0]
        val topRight = pixels[width - 1]
        val bottomLeft = pixels[(height - 1) * width]
        val bottomRight = pixels[(height - 1) * width + width - 1]

        val expectedTopLeft = HomePanelWashDrawable.GRID_COLORS[0][0]
        val expectedTopRight = HomePanelWashDrawable.GRID_COLORS[0][4]
        val expectedBottomLeft = HomePanelWashDrawable.GRID_COLORS[3][0]
        val expectedBottomRight = HomePanelWashDrawable.GRID_COLORS[3][4]

        assertEquals("Top-left sample mismatch", expectedTopLeft, topLeft)
        assertEquals("Top-right sample mismatch", expectedTopRight, topRight)
        assertEquals("Bottom-left sample mismatch", expectedBottomLeft, bottomLeft)
        assertEquals("Bottom-right sample mismatch", expectedBottomRight, bottomRight)
    }

    @Test
    fun testCenterSample_withinTolerance() {
        val width = 100
        val height = 70
        val pixels = HomePanelWashDrawable.interpolateGridPixels(
            HomePanelWashDrawable.GRID_COLORS,
            width,
            height
        )

        val centerX = width / 2
        val centerY = height / 2
        val centerPixel = pixels[centerY * width + centerX]

        // Control points around center:
        // gy = 0.5 * 3 = 1.5 (midpoint between row 1 and 2, col 2)
        // c[1][2] = #F4E0FD, c[2][2] = #EEE4FD
        val r = (centerPixel shr 16) and 0xFF
        val g = (centerPixel shr 8) and 0xFF
        val b = centerPixel and 0xFF

        // Expected midtone: R ~ 0xF1 (241), G ~ 0xE2 (226), B ~ 0xFD (253)
        assertTrue("Center red in expected range (was $r)", abs(r - 241) <= 6)
        assertTrue("Center green in expected range (was $g)", abs(g - 226) <= 6)
        assertTrue("Center blue in expected range (was $b)", abs(b - 253) <= 4)
    }

    @Test
    fun testContrastAgainstDarkText_exceedsWcagAA() {
        val width = 100
        val height = 70
        val pixels = HomePanelWashDrawable.interpolateGridPixels(
            HomePanelWashDrawable.GRID_COLORS,
            width,
            height
        )

        val textColor = 0xFF0B0B0D.toInt() // Text on light panel
        var minContrast = Double.MAX_VALUE

        for (p in pixels) {
            val ratio = contrastRatio(p, textColor)
            if (ratio < minContrast) {
                minContrast = ratio
            }
        }

        assertTrue(
            "Text #0B0B0D against all wash pixels must exceed 4.5:1 (minimum was: $minContrast)",
            minContrast >= 4.5
        )
    }
}
