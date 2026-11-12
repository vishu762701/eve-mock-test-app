/*
 * Derived from Telegram Android AndroidUtilities.shakeViewSpring,
 * Copyright Nikolai Kudashov / Telegram contributors.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.util

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import java.util.WeakHashMap

/**
 * Port of `AndroidUtilities.shakeViewSpring(View, float, Runnable)`
 * (`AndroidUtilities.java:3514-3554`): a 600-stiffness translation spring
 * seeded with `-shift * 100` velocity. The source stores its animation in
 * View tags; EVE keeps the same single-animation-per-view contract in a
 * weak registry so the helper works on any view without new R.id tags.
 */
object TelegramShake {
    private class State(val animation: SpringAnimation, val baseX: Float)

    private val states = WeakHashMap<View, State>()
    private val simpleShakes = WeakHashMap<View, android.animation.ValueAnimator>()

    /**
     * Port of `AndroidUtilities.shakeView(View)`
     * (`AndroidUtilities.java:3489-3512`): 300 ms, 4dp damped-sine horizontal
     * shake. ChangeBioActivity.java:112-128 uses this for a rejected bio.
     */
    fun shakeView(view: View) {
        simpleShakes.remove(view)?.cancel()
        val amplitude = 4f * view.resources.displayMetrics.density
        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 300L
        animator.addUpdateListener { animation ->
            val x = (animation.animatedValue as Float).toDouble()
            view.translationX =
                (4.0 * x * (1.0 - x) * kotlin.math.sin(4.0 * x * Math.PI) *
                    amplitude.toDouble()).toFloat()
        }
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                view.translationX = 0f
                if (simpleShakes[view] === animator) simpleShakes.remove(view)
            }
        })
        simpleShakes[view] = animator
        animator.start()
    }

    fun shakeViewSpring(view: View, shiftDp: Float = 10f, endCallback: (() -> Unit)? = null) {
        states.remove(view)?.let { previous ->
            previous.animation.cancel()
            view.translationX = previous.baseX
        }
        val baseX = view.translationX
        val shift = shiftDp * view.resources.displayMetrics.density
        val animation = SpringAnimation(view, DynamicAnimation.TRANSLATION_X, baseX).apply {
            spring = SpringForce(baseX).setStiffness(600f)
            setStartVelocity(-shift * 100f)
            addEndListener { _, _, _, _ ->
                if (states[view]?.animation === this) {
                    view.translationX = baseX
                    states.remove(view)
                    endCallback?.invoke()
                }
            }
        }
        states[view] = State(animation, baseX)
        animation.start()
    }
}
