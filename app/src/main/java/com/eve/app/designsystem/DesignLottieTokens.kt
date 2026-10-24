package com.eve.app.designsystem

/** Existing artwork palette, preserved under the owner's animation instruction.
 * These are color-lookup tokens, not replacements for the system UI palette.
 * Animation callbacks, alpha, assets, timing and triggers are unchanged.
 */
object DesignLottieTokens {
    fun emptyStateColor(original: Int, isStroke: Boolean): Int {
        val rgb = original and 0x00FFFFFF
        val alpha = original and 0xFF000000.toInt()
        val mappedRgb = if (isStroke) {
            when (rgb) {
                0xC7EBF5, 0xC4EDF5 -> 0x6CB6FF // High-contrast cyan/blue stroke
                0xE6E6E6 -> 0xE2E8F0 // Clean light outline
                0xA6CCD6 -> 0x93C5FD
                0x0A2B4A -> 0xDCEBF5 // Light outline on dark bg
                0x0A4F80 -> 0x6CB6FF
                0x2B4559 -> 0xCBD5E1
                0xFFFFFF -> 0xF8FAFC // Bright white stroke
                0xEB0000 -> 0xFF6B6B // Vivid coral red X
                0x6EE3FF -> 0x6EE3FF
                0x020B19 -> 0xE2E8F0 // Notification bell high-contrast light outline on dark bg
                else -> rgb
            }
        } else {
            when (rgb) {
                0xFFFFFF -> 0xF8FAFC // Retain crisp white paper/shapes on dark background
                0xE6E6E6 -> 0xE2E8F0 // Bright soft grey instead of near-black #1C1C1F
                0xC7EBF5, 0xC4EDF5 -> 0x38BDF8 // Radiant lens glass
                0xA6CCD6 -> 0x60A5FA
                0x0A2B4A -> 0xDCEBF5 // Outlines visible against dark canvas
                0x0A4F80 -> 0x6CB6FF // Accent blue
                0x2B4559 -> 0x94A3B8 // Soft slate
                0xEB0000 -> 0xFF6B6B // High-contrast red
                0x6EE3FF -> 0x6EE3FF
                0x020B19 -> 0xE2E8F0 // Notification bell high-contrast light fill on dark bg
                else -> rgb
            }
        }
        return alpha or mappedRgb
    }


    fun airplaneColor(original: Int, isDark: Boolean, targetColor: Int): Int {
        val rgb = original and 0x00FFFFFF
        val alpha = original and 0xFF000000.toInt()
        return if (isDark) {
            when (rgb) {
                0x000000, 0x231F20 -> alpha or (targetColor and 0x00FFFFFF)
                else -> original
            }
        } else {
            when (rgb) {
                0xFFFFFF, 0xFCFCFC -> alpha or (targetColor and 0x00FFFFFF)
                else -> original
            }
        }
    }
}
