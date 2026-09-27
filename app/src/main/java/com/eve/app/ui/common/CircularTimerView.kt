package com.eve.app.ui.common

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.res.ResourcesCompat
import com.eve.app.R

/**
 * Telegram-style Circular Countdown Timer (matching RadialProgressView.java pattern).
 * Displays a circular ring that smoothly depletes in sync with remaining time,
 * with animated color transition to warning red/amber when time is low.
 */
class CircularTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val strokePx = 3f * density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        color = Color.argb(60, 255, 255, 255)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
        textSize = sp(15f)
        typeface = try {
            ResourcesCompat.getFont(context, R.font.poppins_bold) ?: Typeface.DEFAULT_BOLD
        } catch (_: Throwable) {
            Typeface.DEFAULT_BOLD
        }
    }

    private val oval = RectF()
    private val argbEvaluator = ArgbEvaluator()

    private var currentProgress = 1f
    private var progressAnimator: ValueAnimator? = null
    private var isFirstUpdate = true

    private var currentColor = Color.WHITE
    private var colorAnimator: ValueAnimator? = null

    private var displayText: String = "--:--"
    private var totalSeconds: Long = -1L

    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pad = strokePx / 2f + 2f
        val diameter = minOf(w, h).toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = (diameter / 2f) - pad
        oval.set(cx - r, cy - r, cx + r, cy + r)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (oval.isEmpty) return

        // 1. Background ring track
        canvas.drawOval(oval, trackPaint)

        // 2. Depleting progress arc
        val sweepAngle = 360f * currentProgress
        canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)

        // 3. Centered MM:SS text
        val textY = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(displayText, width / 2f, textY, textPaint)
    }

    fun setTime(remainingSeconds: Long, total: Long = totalSeconds) {
        if (remainingSeconds < 0) return
        if (total > 0) {
            totalSeconds = total
        } else if (totalSeconds <= 0) {
            totalSeconds = remainingSeconds.coerceAtLeast(1L)
        }

        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        displayText = String.format("%02d:%02d", m, s)

        val targetProgress = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

        if (isFirstUpdate) {
            isFirstUpdate = false
            currentProgress = targetProgress
            invalidate()
        } else {
            // Smoothly animate progress arc continuously between second updates
            progressAnimator?.cancel()
            progressAnimator = ValueAnimator.ofFloat(currentProgress, targetProgress).apply {
                duration = 980L
                interpolator = LinearInterpolator()
                addUpdateListener { va ->
                    currentProgress = va.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        // Color transition: warning red if <= 60s or remaining <= 10%
        val isWarning = (remainingSeconds <= 60L || targetProgress <= 0.10f)
        val targetColor = if (isWarning) Color.parseColor("#FF5252") else Color.WHITE

        if (targetColor != currentColor && colorAnimator == null) {
            colorAnimator = ValueAnimator.ofObject(argbEvaluator, currentColor, targetColor).apply {
                duration = 400L
                addUpdateListener { va ->
                    val c = va.animatedValue as Int
                    currentColor = c
                    progressPaint.color = c
                    textPaint.color = c
                    invalidate()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        colorAnimator = null
                    }
                })
                start()
            }
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnimator?.cancel()
        colorAnimator?.cancel()
    }
}
