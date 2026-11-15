package com.eve.app.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt

/**
 * 1:1 Telegram Bulletin layout container matching Bulletin.Layout (16dp pill).
 */
class TelegramBulletinLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val rect = RectF()

    var radius: Float = 16f * context.resources.displayMetrics.density
        set(value) {
            field = value
            invalidate()
        }

    var cardBackgroundColor: ColorStateList = ColorStateList.valueOf(0)
        get() = ColorStateList.valueOf(bgPaint.color)
        set(value) {
            field = value
            bgPaint.color = value.defaultColor
            invalidate()
        }

    init {
        setWillNotDraw(false)
    }

    fun setCardBackgroundColor(@ColorInt color: Int) {
        cardBackgroundColor = ColorStateList.valueOf(color)
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        super.onDraw(canvas)
    }
}
