package com.eve.app.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt

/**
 * Flat Telegram surface card replacing legacy DesignCardView / MaterialCardView.
 * Provides flat round-rect clipping and background tinting with 0dp default elevation.
 */
class TelegramCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val rect = RectF()

    var radius: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var cardBackgroundColor: ColorStateList? = null
        set(value) {
            field = value
            bgPaint.color = value?.defaultColor ?: 0
            invalidate()
        }

    var strokeWidth: Int = 0
        set(value) {
            field = value
            strokePaint.strokeWidth = value.toFloat()
            invalidate()
        }

    var strokeColor: Int = 0
        set(value) {
            field = value
            strokePaint.color = value
            invalidate()
        }

    var rippleColor: ColorStateList? = null

    var cardElevation: Float = 0f
    var maxCardElevation: Float = 0f

    init {
        setWillNotDraw(false)
        radius = 12f * context.resources.displayMetrics.density
    }

    fun setCardBackgroundColor(@ColorInt color: Int) {
        cardBackgroundColor = ColorStateList.valueOf(color)
    }

    fun setStrokeColor(colors: ColorStateList?) {
        strokeColor = colors?.defaultColor ?: 0
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = strokeWidth / 2f
        rect.set(
            paddingLeft + halfStroke,
            paddingTop + halfStroke,
            width - paddingRight - halfStroke,
            height - paddingBottom - halfStroke
        )
        val r = if (radius > 0f) radius else 0f
        if (bgPaint.color != 0) {
            canvas.drawRoundRect(rect, r, r, bgPaint)
        }
        if (strokeWidth > 0 && strokePaint.color != 0) {
            canvas.drawRoundRect(rect, r, r, strokePaint)
        }
        super.onDraw(canvas)
    }
}
