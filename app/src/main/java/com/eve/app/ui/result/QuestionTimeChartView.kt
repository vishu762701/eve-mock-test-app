package com.eve.app.ui.result

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.util.FontManager

class QuestionTimeChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class BarItem(
        val questionNumber: Int,
        val timeSeconds: Long,
        val isCorrect: Boolean,
        val isAttempted: Boolean
    )

    private var items: List<BarItem> = emptyList()
    private var selectedIndex: Int = -1
    var onBarSelected: ((BarItem) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val barWidth = 24f * density
    private val barSpacing = 12f * density
    private val bottomMargin = 28f * density
    private val topMargin = 24f * density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * density
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * density
        textAlign = Paint.Align.CENTER
    }

    private var colorNeutral = 0
    private var colorSlowest = 0
    private var colorSkipped = 0
    private var colorLabel = 0
    private var colorSelected = 0

    private val barRect = RectF()

    init {
        resolveColors()
    }

    private fun resolveColors() {
        colorNeutral = ContextCompat.getColor(context, R.color.eve_text_secondary)
        colorSlowest = ContextCompat.getColor(context, R.color.eve_status_error)
        colorSkipped = ContextCompat.getColor(context, R.color.eve_border)
        colorLabel = ContextCompat.getColor(context, R.color.eve_text_secondary)
        colorSelected = ContextCompat.getColor(context, R.color.eve_text)

        textPaint.typeface = FontManager.typeface(context, bold = false)
        valuePaint.typeface = FontManager.typeface(context, bold = false)
        valuePaint.color = colorLabel
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resolveColors()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        resolveColors()
        invalidate()
    }

    fun setItems(barItems: List<BarItem>) {
        items = barItems
        selectedIndex = -1
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val count = items.size
        val desiredWidth = if (count > 0) {
            ((count * (barWidth + barSpacing)) + barSpacing + paddingLeft + paddingRight).toInt()
        } else {
            suggestedMinimumWidth
        }
        val desiredHeight = (180f * density).toInt()

        val w = resolveSize(desiredWidth, widthMeasureSpec)
        val h = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (items.isEmpty()) return

        val maxSec = (items.maxOfOrNull { it.timeSeconds } ?: 30L).coerceAtLeast(15L).toFloat()
        val slowestSec = items.maxOfOrNull { it.timeSeconds } ?: 0L
        val chartHeight = height - topMargin - bottomMargin
        val startX = paddingLeft + barSpacing

        for (i in items.indices) {
            val item = items[i]
            val left = startX + i * (barWidth + barSpacing)
            val right = left + barWidth

            val barH = if (item.timeSeconds > 0) {
                ((item.timeSeconds / maxSec) * chartHeight).coerceAtLeast(6f * density)
            } else {
                4f * density
            }
            val top = height - bottomMargin - barH
            val bottom = height - bottomMargin

            val isSlowest = item.timeSeconds > 0 && item.timeSeconds == slowestSec
            barPaint.color = when {
                i == selectedIndex -> colorSelected
                isSlowest -> colorSlowest
                item.timeSeconds > 0 -> colorNeutral
                else -> colorSkipped
            }

            barRect.set(left, top, right, bottom)
            val radius = 4f * density
            canvas.drawRoundRect(barRect, radius, radius, barPaint)

            // Draw time value on top of bar
            if (item.timeSeconds > 0) {
                valuePaint.color = if (isSlowest) colorSlowest else colorLabel
                canvas.drawText("${item.timeSeconds}s", left + barWidth / 2, top - 4f * density, valuePaint)
            }

            // Draw Q# below bar
            textPaint.color = if (i == selectedIndex) colorSelected else colorLabel
            canvas.drawText("Q${item.questionNumber}", left + barWidth / 2, height - 8f * density, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val touchX = event.x
            val startX = paddingLeft + barSpacing
            for (i in items.indices) {
                val left = startX + i * (barWidth + barSpacing)
                val right = left + barWidth
                if (touchX >= left - barSpacing / 2 && touchX <= right + barSpacing / 2) {
                    selectedIndex = i
                    invalidate()
                    onBarSelected?.invoke(items[i])
                    return true
                }
            }
        }
        return true
    }
}
