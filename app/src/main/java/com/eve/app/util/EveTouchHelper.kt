package com.eve.app.util

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator

/**
 * Apple iOS Tactile Touch Interaction Helper (Section 2d).
 * Provides physical compression on touch-down (scale 0.965f) with subtle opacity dip (0.88f)
 * and light haptic feedback, followed by smooth spring recovery on release.
 * In addition to ripple/highlight for accessibility.
 */
object EveTouchHelper {

    val standardInterpolator = PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f)
    const val DEFAULT_PRESS_SCALE = 0.965f
    const val DEFAULT_PRESS_ALPHA = 0.88f

    @SuppressLint("ClickableViewAccessibility")
    fun attachTactileFeedback(
        view: View,
        pressScale: Float = DEFAULT_PRESS_SCALE,
        pressAlpha: Float = DEFAULT_PRESS_ALPHA,
        enableHaptic: Boolean = true,
        onClick: (() -> Unit)? = null
    ) {
        view.isClickable = true
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    animatePress(v, pressScale, pressAlpha, 100L)
                    if (enableHaptic) {
                        try {
                            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        } catch (_: Throwable) {}
                    }
                    false
                }
                MotionEvent.ACTION_UP -> {
                    animateRelease(v, 200L)
                    if (onClick != null) {
                        v.performClick()
                        onClick.invoke()
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    animateRelease(v, 200L)
                    false
                }
                else -> false
            }
        }
    }

    private fun animatePress(view: View, targetScale: Float, targetAlpha: Float, durationMs: Long) {
        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .alpha(targetAlpha)
            .setDuration(durationMs)
            .setInterpolator(standardInterpolator)
            .start()
    }

    private fun animateRelease(view: View, durationMs: Long) {
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(durationMs)
            .setInterpolator(standardInterpolator)
            .start()
    }
}
