package com.eve.app.ui.performance

import android.content.Context
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.eve.app.data.model.ScorePoint

/**
 * Performance Analytics (Phase 17) ka score/accuracy trend line chart.
 * Koi external charting library nahi use ki — Canvas par khud draw karte hain taaki
 * naya Gradle dependency na jodna pade aur CI build simple rahe.
 *
 * X-axis = attempts, oldest se newest (left se right). Y-axis = accuracy % (fixed 0-100
 * range), taaki alag exams ke attempts bhi ek hi scale par compare ho sakein.
 */
class ScoreTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var points: List<ScorePoint> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, com.eve.app.R.color.eve_chart_primary)
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, com.eve.app.R.color.eve_chart_fill)
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, com.eve.app.R.color.eve_chart_primary)
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, com.eve.app.R.color.eve_chart_grid)
        strokeWidth = 2f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, com.eve.app.R.color.eve_chart_label)
        textSize = 24f
    }

    fun submit(newPoints: List<ScorePoint>) {
        points = newPoints
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val paddingLeft = 56f
        val paddingRight = 16f
        val paddingTop = 16f
        val paddingBottom = 16f
        val chartW = w - paddingLeft - paddingRight
        val chartH = h - paddingTop - paddingBottom
        if (chartW <= 0f || chartH <= 0f) return

        // Grid lines + Y labels at 0/25/50/75/100%
        listOf(0, 25, 50, 75, 100).forEach { v ->
            val y = paddingTop + chartH * (1 - v / 100f)
            canvas.drawLine(paddingLeft, y, w - paddingRight, y, gridPaint)
            canvas.drawText("$v%", 4f, y + 8f, labelPaint)
        }

        if (points.size < 2) {
            val msg = if (points.isEmpty()) "No data available yet"
            else "At least 2 test attempts are required to show score trend"
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(msg, paddingLeft + chartW / 2, paddingTop + chartH / 2, labelPaint)
            labelPaint.textAlign = Paint.Align.LEFT
            return
        }

        val n = points.size
        val stepX = chartW / (n - 1)
        val path = Path()
        val fillPath = Path()

        fun xOf(i: Int) = paddingLeft + stepX * i
        fun yOf(pct: Int) = paddingTop + chartH * (1 - pct.coerceIn(0, 100) / 100f)

        points.forEachIndexed { i, p ->
            val x = xOf(i)
            val y = yOf(p.percent)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, paddingTop + chartH)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (i == n - 1) {
                fillPath.lineTo(x, paddingTop + chartH)
                fillPath.close()
            }
        }
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
        points.forEachIndexed { i, p -> canvas.drawCircle(xOf(i), yOf(p.percent), 7f, dotPaint) }
    }
}
