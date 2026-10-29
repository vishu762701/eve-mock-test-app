package com.eve.app.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver

/**
 * Authentic Telegram 13.1.1 ItemOptions.DimView overlay.
 *
 * Source: ItemOptions.java:1310-1350, 1768-1806, 1965-2200.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 *
 * Replaces WindowManager.FLAG_DIM_BEHIND with the genuine host overlay:
 * - dimAlpha: 0x08 (~3% window dim per DialogsActivity.java:13678 io.setDimAlpha(0x08))
 * - 150ms entrance and dismissal ValueAnimator progress
 * - Anchored scrimView drawn un-dimmed on top of the dim layer
 * - Synchronized invalidation via OnPreDrawListener
 */
class TelegramPopupDimOverlay(
    context: Context,
    private val container: ViewGroup,
    private val scrimView: View?,
    private val dimAlpha: Int = 0x08
) {
    private var dimAnimator: ValueAnimator? = null
    private var preDrawListener: ViewTreeObserver.OnPreDrawListener? = null
    private val point = FloatArray(2)

    inner class DimView(context: Context) : View(context) {
        var dimProgress = 0f
            set(value) {
                if (field == value) return
                field = value
                invalidate()
            }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (dimProgress <= 0f) return
            // ItemOptions.java:1999: dim = ColorUtils.setAlphaComponent(0x00000000, dimAlpha)
            // ItemOptions.java:2050: canvas.drawColor(Theme.multAlpha(dim, dimProgress))
            val alpha = (dimAlpha * dimProgress).toInt().coerceIn(0, 255)
            canvas.drawColor(Color.argb(alpha, 0, 0, 0))

            // ItemOptions.java:2093, 2192-2194: Scrim/anchor rendering on top of dim layer
            val anchor = scrimView
            if (anchor != null && anchor.parent is View) {
                canvas.save()
                canvas.translate(point[0], point[1])
                canvas.translate(-anchor.scrollX.toFloat(), -anchor.scrollY.toFloat())
                anchor.draw(canvas)
                canvas.restore()
            }
        }
    }

    val dimView = DimView(context)

    init {
        dimView.isClickable = false
        dimView.isFocusable = false
    }

    fun show() {
        cancelAnimator()
        updateAnchorPoint()

        preDrawListener = ViewTreeObserver.OnPreDrawListener {
            val oldX = point[0]
            val oldY = point[1]
            updateAnchorPoint()
            if (point[0] != oldX || point[1] != oldY) {
                dimView.invalidate()
            }
            true
        }
        container.viewTreeObserver.addOnPreDrawListener(preDrawListener)

        if (dimView.parent == null) {
            container.addView(
                dimView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        dimView.dimProgress = 0f

        // ItemOptions.java:1347: dimAnimator.setDuration(150)
        dimAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150L
            addUpdateListener { anim ->
                dimView.dimProgress = anim.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dimView.dimProgress = 1f
                    dimAnimator = null
                }
            })
            start()
        }
    }

    fun dismiss(animated: Boolean = true) {
        cancelAnimator()
        removePreDrawListener()

        if (!animated || dimView.parent == null || dimView.dimProgress <= 0f) {
            cleanupImmediate()
            return
        }

        // ItemOptions.java:1778-1804: dismissDim animation over 150ms
        dimAnimator = ValueAnimator.ofFloat(dimView.dimProgress, 0f).apply {
            duration = 150L
            addUpdateListener { anim ->
                dimView.dimProgress = anim.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    cleanupImmediate()
                }
            })
            start()
        }
    }

    private fun cancelAnimator() {
        dimAnimator?.cancel()
        dimAnimator = null
    }

    private fun removePreDrawListener() {
        preDrawListener?.let { listener ->
            if (container.viewTreeObserver.isAlive) {
                container.viewTreeObserver.removeOnPreDrawListener(listener)
            }
            preDrawListener = null
        }
    }

    private fun cleanupImmediate() {
        cancelAnimator()
        removePreDrawListener()
        dimView.dimProgress = 0f
        if (dimView.parent != null) {
            container.removeView(dimView)
        }
    }

    private fun updateAnchorPoint() {
        val anchor = scrimView ?: return
        val anchorLoc = IntArray(2)
        val containerLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        container.getLocationOnScreen(containerLoc)
        point[0] = (anchorLoc[0] - containerLoc[0]).toFloat()
        point[1] = (anchorLoc[1] - containerLoc[1]).toFloat()
    }
}
