package com.eve.app.designsystem

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.StateSet

/**
 * Authentic Telegram Theme selector drawables ported from upstream Telegram Android:
 * Theme.java:4798-4850 (getRoundRectSelectorDrawable, getSelectorDrawable).
 */
object TelegramThemeSelectors {

    /**
     * Port of Telegram's Theme.getRoundRectSelectorDrawable(int corners, int color).
     * Creates a RippleDrawable with rounded-rectangle mask and Telegram ripple alpha.
     */
    fun getRoundRectSelectorDrawable(cornersPx: Float, selectorColor: Int): Drawable {
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornersPx
            setColor(Color.WHITE)
        }
        val alpha = (selectorColor ushr 24) and 0xFF
        val colorWithAlpha = if (alpha == 0 || alpha == 0xFF) {
            (selectorColor and 0x00FFFFFF) or 0x19000000
        } else {
            selectorColor
        }
        return RippleDrawable(
            ColorStateList(arrayOf(StateSet.WILD_CARD), intArrayOf(colorWithAlpha)),
            null,
            mask
        )
    }

    /**
     * Port of Telegram's Theme.getSelectorDrawable(int color, boolean whiteBackground).
     * Creates a RippleDrawable with rectangular mask and Telegram ripple alpha.
     */
    fun getSelectorDrawable(selectorColor: Int): Drawable {
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.WHITE)
        }
        val alpha = (selectorColor ushr 24) and 0xFF
        val colorWithAlpha = if (alpha == 0 || alpha == 0xFF) {
            (selectorColor and 0x00FFFFFF) or 0x19000000
        } else {
            selectorColor
        }
        return RippleDrawable(
            ColorStateList(arrayOf(StateSet.WILD_CARD), intArrayOf(colorWithAlpha)),
            null,
            mask
        )
    }
}
