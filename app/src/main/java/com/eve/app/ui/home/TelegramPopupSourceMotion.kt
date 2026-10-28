package com.eve.app.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.eve.app.designsystem.UiArcMotion
import com.eve.app.designsystem.TelegramPopupSourceBackground
import java.util.WeakHashMap

/**
 * EVE-isolated original Telegram popup row timing / cascade. NOT the complete renderer.
 *
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * ActionBarPopupWindow.java:843-913,1024-1069 (GPL-2-or-later header).
 * AndroidUtilities.java:5225-5230 (GPL-2-or-later header).
 *
 * The original backScaleY belongs to ActionBarPopupWindowLayout's custom background
 * renderer, and is deliberately NOT emulated by scaling the whole EVE popup.
 * The original account/notification/INavigationLayout runtime is not imported.
 */
internal object TelegramPopupSourceMotion {
    private val running = WeakHashMap<View, Animator>()

    // Exact AndroidUtilities.cascade math, translated without Telegram MathUtils.
    fun cascade(fullAnimationT: Float, position: Float, count: Float, waveLength: Float): Float {
        if (count <= 0f) return fullAnimationT
        val waveDuration = 1f / count * minOf(waveLength, count)
        val waveOffset = position / count * (1f - waveDuration)
        return ((fullAnimationT - waveOffset) / waveDuration).coerceIn(0f, 1f)
    }

    private fun settle(root: View, rows: List<View>) {
        root.alpha = 1f
        root.translationY = 0f
        val source = root.background as? TelegramPopupSourceBackground
        if (source != null) {
            source.backScaleY = 1f
            source.backAlpha = 255
        } else {
            root.background?.alpha = 255
        }
        rows.forEach { row ->
            row.translationY = 0f
            row.alpha = if (row.isEnabled) 1f else 0.5f
        }
    }

    fun cancel(root: View, rows: List<View>) {
        running.remove(root)?.cancel()
        settle(root, rows)
    }

    fun entrance(root: View, rows: List<View>) {
        cancel(root, rows)
        if (!UiArcMotion.isMotionEnabled(root.context)) return
        val visible = rows.filter { it.visibility == View.VISIBLE }
        if (visible.isEmpty()) return
        val count = visible.size
        val rise = -6f * root.resources.displayMetrics.density
        val source = root.background as? TelegramPopupSourceBackground
        if (source != null) {
            source.backScaleY = 0f
            source.backAlpha = 0
        } else {
            root.background?.alpha = 0
        }
        visible.forEach { it.alpha = 0f }
        // Original ActionBarPopupWindow.java:843-848 sets content/root alpha 1f.
        // Animate ONLY the 9-patch backAlpha/backScaleY and menu row cascade;
        // a second root fade multiplied their opacity and looked sluggish.
        root.alpha = 1f
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            // Original ActionBarPopupWindow.startAnimation: 150 + 16 * visibleCount.
            duration = (150 + 16 * count).toLong()
            // Original ActionBarPopupWindow.java:66,397 uses decelerateInterpolator.
            interpolator = DecelerateInterpolator()
            addUpdateListener { frame ->
                val progress = frame.animatedValue as Float
                if (source != null) {
                    // Original background-only top-down growth, not root.scaleY.
                    source.backScaleY = progress
                    source.backAlpha = (progress * 255f).toInt().coerceIn(0, 255)
                } else {
                    root.background?.alpha = (progress * 255f).toInt().coerceIn(0, 255)
                }
                visible.forEachIndexed { position, row ->
                    // Original AndroidUtilities.cascade(t, position, count, 4).
                    val at = cascade(progress, position.toFloat(), count.toFloat(), 4f)
                    row.translationY = (1f - at) * rise
                    row.alpha = at * if (row.isEnabled) 1f else 0.5f
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) {
                    if (running[root] === animation) running.remove(root)
                    if (!cancelled) settle(root, rows)
                }
            })
        }
        running[root] = animator
        animator.start()
    }

    fun exit(root: View, rows: List<View>, onEnd: () -> Unit) {
        cancel(root, rows)
        if (!UiArcMotion.isMotionEnabled(root.context)) {
            onEnd()
            return
        }
        // Original ActionBarPopupWindow.dismiss(scaleOut=false): -5dp and alpha 0,
        // duration 150ms. Do not use the older EVE 0.8 whole-window scale.
        val distance = -5f * root.resources.displayMetrics.density
        val animator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(root, View.TRANSLATION_Y, 0f, distance),
                ObjectAnimator.ofFloat(root, View.ALPHA, 1f, 0f)
            )
            duration = 150L
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) {
                    if (running[root] === animation) running.remove(root)
                    if (!cancelled) {
                        settle(root, rows)
                        onEnd()
                    }
                }
            })
        }
        running[root] = animator
        animator.start()
    }
}
