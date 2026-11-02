package com.eve.app.ui.profile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.view.MotionEvent
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
    private var closing = false
    private var closeAlpha = 1f

    init {
        setWillNotDraw(false)
        clipChildren = false
        // Hide content while the Material host is finding its final sheet size.
        alpha = 1f
    }

    fun sourceSheetProgress(): Float = progress

    fun animateSourceOpen() {
        val previous = animator
        animator = null
        previous?.cancel()
        closing = false
        closeAlpha = 1f
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

    /**
     * Adapted original QrShareView.stepSheet closing: 360ms cubic-bezier
     * island collapse and independent 170ms power-1.7 content fade.
     * Start from the currently sampled opening fraction on interruption.
     */
    fun animateSourceClose(onClosed: () -> Unit) {
        val previous = animator
        animator = null
        previous?.cancel()
        closing = true
        if (!isAttachedToWindow || width <= 0 || height <= 0 ||
            !DesignMotion.enabled(context)) {
            progress = 0f
            closeAlpha = 0f
            invalidate()
            onClosed()
            return
        }
        val from = progress
        val fromAlpha = QrPickerSheetMorph.openContent(from)
        val a = ValueAnimator.ofFloat(0f, 1f)
        animator = a
        a.duration = QrPickerSheetMorph.CLOSE_MS
        a.interpolator = LinearInterpolator()
        a.addUpdateListener { frame ->
            val fraction = frame.animatedValue as Float
            progress = from * (1f - QrPickerSheetMorph.sourceEase(fraction))
            closeAlpha = minOf(fromAlpha, QrPickerSheetMorph.closeContent(
                (fraction * QrPickerSheetMorph.CLOSE_MS).toLong()))
            invalidate()
        }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (animator === animation) {
                    animator = null
                    progress = 0f
                    closeAlpha = 0f
                    invalidate()
                    onClosed()
                }
            }
        })
        a.start()
    }

    private fun settle() {
        progress = 1f
        closing = false
        closeAlpha = 1f
        invalidate()
    }

    // QrShareView.onTouchEvent only routes sheet input after sheet > 0.5.
    // Never allow invisible controls inside the expanding clip to activate.
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean =
        if (closing || progress < .5f) true else super.onInterceptTouchEvent(event)

    override fun onTouchEvent(event: MotionEvent): Boolean =
        if (closing || progress < .5f) true else super.onTouchEvent(event)

    override fun dispatchDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        geometry.sample(width, height, progress)
        clipPath.rewind()
        clipPath.addRoundRect(geometry.left, geometry.top, geometry.right,
            geometry.bottom, geometry.radius, geometry.radius, Path.Direction.CW)
        val save = canvas.save()
        canvas.clipPath(clipPath)
        val layer = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(),
            (255f * if (closing) closeAlpha else geometry.contentAlpha).toInt().coerceIn(0, 255))
        super.dispatchDraw(canvas)
        canvas.restoreToCount(layer)
        canvas.restoreToCount(save)
    }

    fun dispose() {
        val previous = animator
        animator = null
        previous?.cancel()
        settle()
    }

    override fun onDetachedFromWindow() {
        dispose()
        super.onDetachedFromWindow()
    }
}
