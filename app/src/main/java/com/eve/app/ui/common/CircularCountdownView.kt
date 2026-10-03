package com.eve.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.eve.app.R

/**
 * Telegram-style circular countdown timer ring matching UndoView.java.
 * Smoothly depletes an arc around an inner trash icon frame-by-frame.
 */
class CircularCountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokePx = 2.5f * resources.displayMetrics.density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
    }

    private val oval = RectF()
    private var progress: Float = 1f // 1.0 = full circle, 0.0 = depleted
    private var animator: ValueAnimator? = null
    private var iconDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_delete)?.mutate()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pad = strokePx / 2f + 1f
        oval.set(pad, pad, w.toFloat() - pad, h.toFloat() - pad)

        // Center the trash icon with padding inside the ring
        val iconPad = (w * 0.28f).toInt()
        iconDrawable?.setBounds(iconPad, iconPad, w - iconPad, h - iconPad)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val fgColor = ContextCompat.getColor(context, R.color.eve_bulletin_text)
        val trackColor = Color.argb(
            60,
            Color.red(fgColor),
            Color.green(fgColor),
            Color.blue(fgColor)
        )
        trackPaint.color = trackColor
        progressPaint.color = fgColor
        iconDrawable?.setTint(fgColor)

        // Draw background ring track
        canvas.drawOval(oval, trackPaint)

        // Draw depleting progress arc from top (-90 degrees) clockwise
        val sweepAngle = 360f * progress
        canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)

        // Draw centered trash icon
        iconDrawable?.draw(canvas)
    }

    fun startCountdown(durationMs: Long, onFinish: (() -> Unit)? = null) {
        cancel()
        progress = 1f
        animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { va ->
                progress = va.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onFinish?.invoke()
                }
            })
            start()
        }
    }

    fun cancel() {
        animator?.cancel()
        animator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancel()
    }
}
