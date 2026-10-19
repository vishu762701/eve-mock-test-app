package com.eve.app.designsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.eve.app.R
import com.eve.app.util.FontManager

/** The existing accuracy value/meaning thresholds, painted with the shared thin-ring tokens. */
class DesignAccuracyRing @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var percentage = 0f
    private val rect = RectF()
    private val stroke = resources.getDimension(R.dimen.design_ring_stroke)
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.design_type_secondary)
        typeface = FontManager.typeface(context, bold = false)
    }
    fun setPercentage(pct: Float) {
        percentage = pct.coerceIn(0f, 100f)
        contentDescription = "${percentage.toInt()}% accuracy"
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = stroke / 2
        val diameter = minOf(width, height).toFloat()
        val left = (width - diameter) / 2
        val top = (height - diameter) / 2
        rect.set(left + pad, top + pad, left + diameter - pad, top + diameter - pad)
        arc.color = DesignTokens.track(context)
        canvas.drawOval(rect, arc)
        arc.color = DesignTokens.color(context, when {
            percentage >= 70f -> R.attr.systemGreen
            percentage >= 40f -> R.attr.systemOrange
            else -> R.attr.systemRed
        })
        canvas.drawArc(rect, -90f, 360f * percentage / 100f, false, arc)
        label.color = DesignTokens.primary(context)
        canvas.drawText("${percentage.toInt()}%", width / 2f, height / 2f - (label.ascent() + label.descent()) / 2, label)
    }
}
