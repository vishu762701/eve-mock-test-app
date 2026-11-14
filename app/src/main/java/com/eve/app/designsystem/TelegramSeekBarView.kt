/*
 * Derived from Telegram Android SeekBarView.java:1-200
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.eve.app.theme.TelegramThemeEngine
import kotlin.math.roundToInt

/**
 * Authentic Telegram SeekBar View for Text Size and Corner Radius sliders.
 * Upstream: SeekBarView.java
 */
class TelegramSeekBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val paintTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }
    private val paintProgress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }
    private val paintThumb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintThumbShadow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
    }

    var minValue: Int = 12
        set(value) {
            field = value
            invalidate()
        }

    var maxValue: Int = 30
        set(value) {
            field = value
            invalidate()
        }

    var currentValue: Int = 16
        set(value) {
            field = value.coerceIn(minValue, maxValue)
            progress = if (maxValue > minValue) (field - minValue).toFloat() / (maxValue - minValue) else 0f
            invalidate()
        }

    private var progress: Float = (16f - 12f) / (30f - 12f)
    private var isDragging = false
    private var startTouchX = 0f

    var onValueChanged: ((value: Int) -> Unit)? = null

    init {
        paintThumbShadow.color = 0x26000000
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minH = dp(44f)
        val h = resolveSize(minH, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        val thumbRadius = dp(10f).toFloat()
        val textSpace = dp(36f).toFloat()
        val trackLeft = thumbRadius + dp(4f)
        val trackRight = width.toFloat() - textSpace - thumbRadius - dp(4f)
        val trackWidth = (trackRight - trackLeft).coerceAtLeast(1f)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startTouchX = event.x
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                updateProgressFromTouch(event.x, trackLeft, trackWidth)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateProgressFromTouch(event.x, trackLeft, trackWidth)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    updateProgressFromTouch(event.x, trackLeft, trackWidth)
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateProgressFromTouch(touchX: Float, trackLeft: Float, trackWidth: Float) {
        val clampedX = touchX.coerceIn(trackLeft, trackLeft + trackWidth)
        progress = (clampedX - trackLeft) / trackWidth

        val steps = maxValue - minValue
        val stepped = (minValue + (progress * steps).roundToInt()).coerceIn(minValue, maxValue)
        if (stepped != currentValue) {
            currentValue = stepped
            onValueChanged?.invoke(currentValue)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        val trackH = dp(2f).toFloat()
        val thumbRadius = dp(9f).toFloat()
        val textSpace = dp(36f).toFloat()

        val trackLeft = thumbRadius + dp(4f)
        val trackRight = width.toFloat() - textSpace - thumbRadius - dp(4f)
        val trackWidth = (trackRight - trackLeft).coerceAtLeast(1f)
        val cy = height / 2f

        // 1. Inactive Track
        paintTrack.color = palette.hairline
        paintTrack.strokeWidth = trackH
        canvas.drawLine(trackLeft, cy, trackRight, cy, paintTrack)

        // 2. Active Progress Track
        val currentThumbX = trackLeft + trackWidth * progress
        paintProgress.color = palette.accent
        paintProgress.strokeWidth = trackH
        canvas.drawLine(trackLeft, cy, currentThumbX, cy, paintProgress)

        // 3. Thumb Shadow & Thumb Circle
        paintThumb.color = palette.accent
        canvas.drawCircle(currentThumbX, cy + dp(1f), thumbRadius + dp(1f), paintThumbShadow)
        canvas.drawCircle(currentThumbX, cy, thumbRadius, paintThumb)

        // 4. Value Text on Right
        paintValue.color = palette.textSecondary
        paintValue.textSize = dp(14f).toFloat()
        paintValue.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        val fontMetrics = paintValue.fontMetrics
        val textY = cy - (fontMetrics.descent + fontMetrics.ascent) / 2f
        canvas.drawText(currentValue.toString(), width.toFloat() - dp(8f), textY, paintValue)
    }
}
