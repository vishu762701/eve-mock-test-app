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

    private val strokePx = 8f * density
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
        textSize = 15f * density
        isFakeBoldText = true
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 10f * density
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
        val colorBg = ContextCompat.getColor(context, R.color.eve_surface_2)
        val colorText = ContextCompat.getColor(context, R.color.eve_text)
        val colorSubText = ContextCompat.getColor(context, R.color.eve_text_secondary)
        val colorProgress = when {
            percentage >= 70f -> ContextCompat.getColor(context, R.color.eve_tile_right_text)
            percentage >= 40f -> ContextCompat.getColor(context, R.color.eve_tile_medium_text)
            else -> ContextCompat.getColor(context, R.color.eve_tile_wrong_text)
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
        val diameter = (w - pad * 2f).coerceAtLeast(10f)
        arcRect.set(pad, pad, pad + diameter, pad + diameter)
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
        canvas.drawText("${percentage.toInt()}%", cx, cy - (8f * density), textPaint)
        canvas.drawText("ACCURACY", cx, cy + (6f * density), labelPaint)
    }
}
