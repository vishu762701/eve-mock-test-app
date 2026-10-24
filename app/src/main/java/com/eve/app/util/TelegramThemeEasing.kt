package com.eve.app.util

import android.view.animation.Interpolator
import kotlin.math.abs

/**
 * Theme easing solver ported from Telegram for Android:
 * org.telegram.ui.Components.CubicBezierInterpolator
 * and Easings.easeInOutQuad.
 * Source snapshot:
 * dc780e81ed1261c369c27870e8e0999a1eb0b600.
 * Retain upstream attribution and applicable license notices.
 */
internal object TelegramThemeEasing : Interpolator {
    private const val START_X = 0.455f
    private const val START_Y = 0.03f
    private const val END_X = 0.515f
    private const val END_Y = 0.955f

    override fun getInterpolation(input: Float): Float {
        val time = getXForTime(input)
        val c = 3f * START_Y
        val b = 3f * (END_Y - START_Y) - c
        val a = 1f - c - b
        return time * (c + time * (b + time * a))
    }

    private fun getXForTime(time: Float): Float {
        val c = 3f * START_X
        val b = 3f * (END_X - START_X) - c
        val a = 1f - c - b
        var x = time
        for (i in 1 until 14) {
            val z = x * (c + x * (b + x * a)) - time
            if (abs(z) < 1e-3f) break
            val derivative = c + x * (2f * b + 3f * a * x)
            x -= z / derivative
        }
        return x
    }
}
