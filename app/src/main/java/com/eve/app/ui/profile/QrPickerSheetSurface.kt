package com.eve.app.ui.profile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.widget.FrameLayout
import android.view.animation.LinearInterpolator
import com.eve.app.designsystem.DesignMotion
import com.eve.app.telegram.qr.QrPickerSheetMorph

/**
 * ADAPTED original QrShareView island -> floating-sheet clipping/composition.
 * EVE keeps BottomSheetDialog's existing back, outside-tap, drag, accessibility
 * and window-inset ownership. The parent Material motion remains a parity gap.
 */
internal class QrPickerSheetSurface(context: Context) : FrameLayout(context) {
    private val geometry = QrPickerSheetMorph(resources.displayMetrics.density)
    private val clipPath = Path()
    private var animator: ValueAnimator? = null
    private var progress = 0f

    init {
        setWillNotDraw(false)
        clipChildren = false
        // Hide content while the Material host is finding its final sheet size.
        alpha = 1f
    }

    fun animateSourceOpen() {
        animator?.cancel()
        animator = null
        if (!isAttachedToWindow || width <= 0 || height <= 0 ||
            !DesignMotion.enabled(context)) {
            settle()
            return
        }
        val a = ValueAnimator.ofFloat(0f, 1f)
        animator = a
        a.duration = QrPickerSheetMorph.OPEN_MS
        a.interpolator = LinearInterpolator()
        a.addUpdateListener {
            progress = QrPickerSheetMorph.sourceEase(it.animatedValue as Float)
            invalidate()
        }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (animator === animation) {
                    animator = null
                    settle()
                }
            }
        })
        progress = 0f
        invalidate()
        a.start()
    }

    private fun settle() {
        progress = 1f
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        geometry.sample(width, height, progress)
        clipPath.rewind()
        clipPath.addRoundRect(geometry.left, geometry.top, geometry.right,
            geometry.bottom, geometry.radius, geometry.radius, Path.Direction.CW)
        val save = canvas.save()
        canvas.clipPath(clipPath)
        val layer = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(),
            (255f * geometry.contentAlpha).toInt().coerceIn(0, 255))
        super.dispatchDraw(canvas)
        canvas.restoreToCount(layer)
        canvas.restoreToCount(save)
    }

    fun dispose() {
        animator?.cancel()
        animator = null
        settle()
    }

    override fun onDetachedFromWindow() {
        dispose()
        super.onDetachedFromWindow()
    }
}
