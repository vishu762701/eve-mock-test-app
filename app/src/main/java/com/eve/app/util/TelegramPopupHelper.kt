package com.eve.app.util

import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Telegram-style popup animations matching ActionBarPopupWindow pattern.
 * Provides anchored scale (0.8 -> 1.0) + fade-in with spring overshoot easing on entrance,
 * and reverse scale (1.0 -> 0.8) + fade-out on exit.
 */
object TelegramPopupHelper {
    private val pending = java.util.WeakHashMap<View, Runnable>()
    private val detachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) { cancel(v) }
    }

    fun cancel(view: View) {
        pending.remove(view)?.let(view::removeCallbacks)
        view.animate().withEndAction(null).cancel()
        view.removeOnAttachStateChangeListener(detachListener)
    }

    fun animateEntrance(contentView: View, anchorView: View? = null, onStart: (() -> Unit)? = null) {
        cancel(contentView)
        contentView.addOnAttachStateChangeListener(detachListener)
        val setup = Runnable {
            if (pending.remove(contentView) == null || !contentView.isAttachedToWindow) return@Runnable
            val pw = contentView.width.toFloat()
            val ph = contentView.height.toFloat()

            if (anchorView != null && pw > 0 && ph > 0) {
                val anchorLoc = IntArray(2)
                anchorView.getLocationOnScreen(anchorLoc)
                val contentLoc = IntArray(2)
                contentView.getLocationOnScreen(contentLoc)

                val targetPivotX = (anchorLoc[0] + anchorView.width / 2f - contentLoc[0]).coerceIn(0f, pw)
                val targetPivotY = (anchorLoc[1] + anchorView.height / 2f - contentLoc[1]).coerceIn(0f, ph)

                contentView.pivotX = targetPivotX
                contentView.pivotY = targetPivotY
            } else {
                contentView.pivotX = pw
                contentView.pivotY = 0f
            }

            contentView.scaleX = 0.8f
            contentView.scaleY = 0.8f
            contentView.alpha = 0f

            onStart?.invoke()

            contentView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(220)
                .setInterpolator(OvershootInterpolator(1.1f))
                .start()
        }
        pending[contentView] = setup
        contentView.post(setup)
    }

    fun animateExit(contentView: View, onEnd: () -> Unit) {
        cancel(contentView)
        contentView.addOnAttachStateChangeListener(detachListener)
        contentView.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0f)
            .setDuration(160)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                onEnd()
            }
            .start()
    }
}
