package com.eve.app.ui.result

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.eve.app.R

class SemiCircleGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private var percentage: Float = 0f

    private val strokePx = resources.getDimension(R.dimen.design_ring_stroke)
    private val arcRect = RectF()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = com.eve.app.util.FontManager.typeface(context, bold = false)
        textSize = resources.getDimension(com.eve.app.R.dimen.design_type_heading)
        typeface = com.eve.app.util.FontManager.typeface(context, bold = true)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = com.eve.app.util.FontManager.typeface(context, bold = false)
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(com.eve.app.R.dimen.design_type_secondary)
    }

    init {
        resolveColors()
    }

    fun setPercentage(pct: Float) {
        percentage = pct.coerceIn(0f, 100f)
        resolveColors()
        invalidate()
    }

    private fun resolveColors() {
        val colorBg = com.eve.app.designsystem.DesignTokens.color(context, com.eve.app.R.attr.designTrack)
        val colorText = com.eve.app.designsystem.DesignTokens.color(context, com.eve.app.R.attr.designTextPrimary)
        val colorSubText = com.eve.app.designsystem.DesignTokens.color(context, com.eve.app.R.attr.designTextSecondary)
        val colorProgress = when {
            percentage >= 70f -> com.eve.app.designsystem.DesignTokens.color(context, com.eve.app.R.attr.systemGreen)
            percentage >= 40f -> com.eve.app.designsystem.DesignTokens.color(context, com.eve.app.R.attr.systemOrange)
            else -> com.eve.app.designsystem.DesignTokens.color(context, com.eve.app.R.attr.systemRed)
        }

        bgPaint.color = colorBg
        bgPaint.strokeWidth = strokePx
        progressPaint.color = colorProgress
        progressPaint.strokeWidth = strokePx
        textPaint.color = colorText
        labelPaint.color = colorSubText
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pad = strokePx / 2f + 4f * density
        val maxDiamW = (w - pad * 2f).coerceAtLeast(10f)
        val maxDiamH = ((h - pad - 16f * density) * 2f).coerceAtLeast(10f)
        val diameter = minOf(maxDiamW, maxDiamH)
        val left = (w - diameter) / 2f
        arcRect.set(left, pad, left + diameter, pad + diameter)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        resolveColors()

        // Semicircle arc: 180° to 360° (top half)
        canvas.drawArc(arcRect, 180f, 180f, false, bgPaint)

        // Progress arc
        val sweep = 180f * (percentage / 100f)
        if (sweep > 0f) {
            canvas.drawArc(arcRect, 180f, sweep, false, progressPaint)
        }

        // Center typography
        val cx = arcRect.centerX()
        val cy = arcRect.centerY()
        canvas.drawText("${percentage.toInt()}%", cx, cy - (6f * density), textPaint)
        canvas.drawText("ACCURACY", cx, cy + (10f * density), labelPaint)
    }
}
