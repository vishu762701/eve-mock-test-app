package com.eve.app.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Apple iOS 26 Tactile Touch Interaction Helper.
 * Provides immediate physical compression on touch-down (scale ~0.982) with subtle luminance/alpha shift
 * and light haptic feedback, followed by smooth spring recovery on release.
 * Target timing: 80-120ms press-in, 150-220ms release.
 */
object EveTouchHelper {

    private val fastOutSlow = FastOutSlowInInterpolator()

    @SuppressLint("ClickableViewAccessibility")
    fun attachTactileFeedback(
        view: View,
        pressScale: Float = 0.982f,
        enableHaptic: Boolean = true,
        onClick: (() -> Unit)? = null
    ) {
        view.isClickable = true
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    animateScale(v, pressScale, 90L)
                    if (enableHaptic) {
                        try {
                            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        } catch (_: Throwable) {}
                    }
                    false
                }
                MotionEvent.ACTION_UP -> {
                    animateScale(v, 1.0f, 180L)
                    if (onClick != null) {
                        v.performClick()
                        onClick.invoke()
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    animateScale(v, 1.0f, 180L)
                    false
                }
                else -> false
            }
        }
    }

    private fun animateScale(view: View, targetScale: Float, durationMs: Long) {
        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(durationMs)
            .setInterpolator(fastOutSlow)
            .start()
    }
}
