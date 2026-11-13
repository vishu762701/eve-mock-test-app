/*
 * Derived from Telegram Android UndoView.java.
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later.
 */
package com.eve.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.ceil

/**
 * Telegram-style circular countdown timer ring matching UndoView.java:325-330.
 * Smoothly depletes a 2dp rounded arc around the remaining seconds integer.
 */
class CircularCountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(12f)
        typeface = Typeface.DEFAULT_BOLD
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private val oval = RectF()
    private var progress: Float = 1f
    private var totalDurationMs: Long = 5000L
    private var lastRemainingSec: Int = 5
    private var animator: ValueAnimator? = null
    private var tintColor: Int = Color.WHITE

    fun setInfoColor(color: Int) {
        tintColor = color
        progressPaint.color = color
        textPaint.color = color
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pad = dp(2f)
        oval.set(pad, pad, w.toFloat() - pad, h.toFloat() - pad)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        progressPaint.color = tintColor
        textPaint.color = tintColor

        // UndoView.java: drawArc(-90, -360 * progress) depleting counter-clockwise or clockwise
        val sweepAngle = -360f * progress
        canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)

        val label = lastRemainingSec.toString()
        val cy = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, width / 2f, cy, textPaint)
    }

    fun startCountdown(durationMs: Long, onFinish: (() -> Unit)? = null) {
        cancel()
        totalDurationMs = durationMs.coerceAtLeast(1L)
        progress = 1f
        lastRemainingSec = ceil(totalDurationMs / 1000.0).toInt().coerceAtLeast(1)
        animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = totalDurationMs
            interpolator = LinearInterpolator()
            addUpdateListener { va ->
                progress = va.animatedValue as Float
                val remMs = (totalDurationMs * progress).toLong()
                lastRemainingSec = ceil(remMs / 1000.0).toInt().coerceAtLeast(1)
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
