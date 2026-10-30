/*
 * Derived from Telegram Android NumberTextView.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official Telegram NumberTextView ported for EVE Admin Hub.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Components/NumberTextView.java
 * Smooth animated digit counter view with sliding vertical transitions.
 */
class NumberTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var currentNumber: Int = 0
    private var oldNumber: Int = 0
    private var progress: Float = 1f
    private var animator: ObjectAnimator? = null

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        textPaint.color = palette.textPrimary
        textPaint.textSize = 18f * resources.displayMetrics.density
        textPaint.typeface = Typeface.DEFAULT_BOLD
    }

    fun setTextColor(color: Int) {
        textPaint.color = color
        invalidate()
    }

    fun setTextSize(sizeSp: Float) {
        textPaint.textSize = sizeSp * resources.displayMetrics.scaledDensity
        requestLayout()
        invalidate()
    }

    @Keep
    fun setProgress(value: Float) {
        if (progress == value) return
        progress = value
        invalidate()
    }

    fun getProgress(): Float = progress

    fun setNumber(number: Int, animated: Boolean = true) {
        if (currentNumber == number) return
        animator?.cancel()
        oldNumber = currentNumber
        currentNumber = number

        if (!animated) {
            progress = 1f
            invalidate()
            return
        }

        progress = 0f
        animator = ObjectAnimator.ofFloat(this, "progress", 0f, 1f).apply {
            duration = 180L
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val currentStr = currentNumber.toString()
        val oldStr = oldNumber.toString()
        val fontMetrics = textPaint.fontMetrics
        val baseline = height / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f

        if (progress == 1f || oldNumber == currentNumber) {
            canvas.drawText(currentStr, 0f, baseline, textPaint)
        } else {
            val slideHeight = height.toFloat()
            // Old number sliding up and fading out
            val oldAlpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
            textPaint.alpha = oldAlpha
            canvas.drawText(oldStr, 0f, baseline - slideHeight * progress, textPaint)

            // New number sliding up from bottom and fading in
            val newAlpha = (progress * 255).toInt().coerceIn(0, 255)
            textPaint.alpha = newAlpha
            canvas.drawText(currentStr, 0f, baseline + slideHeight * (1f - progress), textPaint)
            textPaint.alpha = 255
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val str = currentNumber.coerceAtLeast(oldNumber).toString()
        val width = textPaint.measureText(str).toInt() + paddingLeft + paddingRight
        val height = (textPaint.fontMetrics.bottom - textPaint.fontMetrics.top).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }
}
