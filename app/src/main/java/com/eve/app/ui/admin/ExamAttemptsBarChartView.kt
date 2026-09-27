package com.eve.app.ui.admin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.eve.app.R

data class ExamBarData(
    val examName: String,
    val attemptCount: Int
)

class ExamAttemptsBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var items: List<ExamBarData> = emptyList()

    private val primaryColor = ContextCompat.getColor(context, R.color.eve_primary)
    private val barTrackColor = ContextCompat.getColor(context, R.color.eve_surface)
    private val textColor = ContextCompat.getColor(context, R.color.eve_text)
    private val subTextColor = ContextCompat.getColor(context, R.color.eve_text_secondary)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryColor
        style = Paint.Style.FILL
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = barTrackColor
        style = Paint.Style.FILL
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = dpToPx(13f)
        isFakeBoldText = true
    }

    private val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = subTextColor
        textSize = dpToPx(12f)
        textAlign = Paint.Align.RIGHT
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = subTextColor
        textSize = dpToPx(13f)
        textAlign = Paint.Align.CENTER
    }

    private val rectF = RectF()

    fun setData(data: List<ExamBarData>) {
        items = data.take(8) // Top 8 exams for clean display
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val desiredHeight = if (items.isEmpty()) {
            dpToPx(100f).toInt()
        } else {
            (items.size * dpToPx(52f) + dpToPx(24f)).toInt()
        }
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val finalHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }
        setMeasuredDimension(width, finalHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (items.isEmpty()) {
            canvas.drawText(
                "No attempt data available for selected period",
                width / 2f,
                height / 2f,
                emptyPaint
            )
            return
        }

        val maxAttempts = items.maxOfOrNull { it.attemptCount }?.coerceAtLeast(1) ?: 1
        val paddingLeft = dpToPx(12f)
        val paddingRight = dpToPx(12f)
        val contentWidth = width - paddingLeft - paddingRight

        var currentY = dpToPx(18f)
        val barHeight = dpToPx(12f)
        val cornerRadius = dpToPx(6f)

        for (item in items) {
            // Draw title and count
            val displayName = if (item.examName.length > 28) item.examName.take(26) + "…" else item.examName
            canvas.drawText(displayName, paddingLeft, currentY, titlePaint)

            val countText = "${item.attemptCount} attempt${if (item.attemptCount == 1) "" else "s"}"
            canvas.drawText(countText, width - paddingRight, currentY, countPaint)

            currentY += dpToPx(8f)

            // Draw track
            rectF.set(paddingLeft, currentY, paddingLeft + contentWidth, currentY + barHeight)
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, trackPaint)

            // Draw filled bar
            val barRatio = item.attemptCount.toFloat() / maxAttempts.toFloat()
            val filledWidth = (contentWidth * barRatio).coerceAtLeast(dpToPx(10f))
            rectF.set(paddingLeft, currentY, paddingLeft + filledWidth, currentY + barHeight)
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, barPaint)

            currentY += barHeight + dpToPx(24f)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
