package com.eve.app.util

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator

/**
 * Apple iOS Motion System Helper (Section 2d).
 * - Standard transition curve: PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f), 300ms.
 * - Spring / overshoot curve for sheets, dialogs, floating chips, and playful entrances.
 */
object EveMotionHelper {

    val standardInterpolator = PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f)
    val springInterpolator = OvershootInterpolator(1.25f)

    const val DURATION_STANDARD_MS = 300L
    const val DURATION_SPRING_MS = 360L

    fun areAnimationsEnabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()) {
                return false
            }
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        } catch (_: Throwable) {
            true
        }
    }

    /**
     * Spring entrance animation for floating chips, sheets, and cards.
     */
    fun animateSpringEntrance(view: View, startScale: Float = 0.85f, delayMs: Long = 0L) {
        if (!areAnimationsEnabled(view.context)) {
            view.scaleX = 1f
            view.scaleY = 1f
            view.alpha = 1f
            view.translationY = 0f
            return
        }
        view.scaleX = startScale
        view.scaleY = startScale
        view.alpha = 0f
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setStartDelay(delayMs)
            .setDuration(DURATION_SPRING_MS)
            .setInterpolator(springInterpolator)
            .start()
    }

    /**
     * Spring dismissal animation for dialogs, sheets, or popups.
     */
    fun animateDismiss(view: View, onEnd: () -> Unit) {
        if (!areAnimationsEnabled(view.context)) {
            view.scaleX = 0.92f
            view.scaleY = 0.92f
            view.alpha = 0f
            view.translationY = view.resources.displayMetrics.density * 16f
            onEnd()
            return
        }
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .alpha(0f)
            .translationY(view.resources.displayMetrics.density * 16f)
            .setDuration(220L)
            .setInterpolator(standardInterpolator)
            .withEndAction { onEnd() }
            .start()
    }
}
