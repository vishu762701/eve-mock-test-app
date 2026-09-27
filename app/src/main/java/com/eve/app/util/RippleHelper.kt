package com.eve.app.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RoundRectShape

/**
 * Helper for creating Telegram-style masked, bounded RippleDrawables matching
 * Theme.createSelectorDrawable in Telegram's Theme.java.
 */
object RippleHelper {

    /**
     * Creates a masked, bounded RippleDrawable.
     *
     * @param context Android Context
     * @param tintColor Optional custom tint color. If null, a theme-adaptive tint is used.
     * @param cornerRadiusDp Corner radius in dp. If < 0, an OvalShape (circular mask) is used for icon buttons.
     * @param isDark Whether dark theme is active (or inferred automatically)
     */
    fun createPremiumRippleDrawable(
        context: Context,
        tintColor: Int? = null,
        cornerRadiusDp: Float = 12f,
        isDark: Boolean = ThemeSwitchAnimator.isDarkMode(context)
    ): RippleDrawable {
        // Theme-adaptive tint: subtle white-based tint in dark mode, subtle dark-based tint in light mode
        val rippleColor = tintColor ?: if (isDark) {
            Color.argb(38, 255, 255, 255) // ~15% white
        } else {
            Color.argb(31, 0, 0, 0) // ~12% dark
        }
        val colorStateList = ColorStateList.valueOf(rippleColor)

        val maskDrawable: Drawable = if (cornerRadiusDp < 0f) {
            ShapeDrawable(OvalShape())
        } else {
            val density = context.resources.displayMetrics.density
            val radiusPx = cornerRadiusDp * density
            val outerRadii = FloatArray(8) { radiusPx }
            ShapeDrawable(RoundRectShape(outerRadii, null, null))
        }

        return RippleDrawable(colorStateList, null, maskDrawable)
    }

    fun createPremiumRippleDrawable(
        context: Context,
        cornerRadiusDp: Float,
        isDark: Boolean = ThemeSwitchAnimator.isDarkMode(context)
    ): RippleDrawable = createPremiumRippleDrawable(context, null, cornerRadiusDp, isDark)
}
