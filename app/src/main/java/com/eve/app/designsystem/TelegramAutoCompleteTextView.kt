/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/EditTextBoldCursor.java:40-220 @ 3de37c8cfb03
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * 1:1 Telegram AutoCompleteTextView with authentic Telegram theme dropdown,
 * custom accent cursor, and active focus outline animation from EditTextBoldCursor.java.
 */
class TelegramAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private val palette = TelegramThemeEngine.resolveCurrentPalette(context)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val lineRect = RectF()
    private var focusProgress = 0f
    private var focusAnimator: ValueAnimator? = null

    init {
        typeface = FontManager.telegramRegularTypeface(context)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTextColor(palette.textPrimary)
        setHintTextColor(palette.textSecondary)
        setDropDownBackgroundDrawable(TelegramThemeSelectors.getRoundRectSelectorDrawable(
            dp(10f),
            palette.dialogSurface
        ))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cursorDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(palette.accent)
                setSize((dp(2f) + 0.5f).toInt(), (dp(20f) + 0.5f).toInt())
                cornerRadius = dp(1f)
            }
            textCursorDrawable = cursorDrawable
        }

        setOnFocusChangeListener { _, hasFocus ->
            animateFocus(hasFocus)
        }
    }

    private fun animateFocus(focused: Boolean) {
        focusAnimator?.cancel()
        val target = if (focused) 1f else 0f
        focusAnimator = ValueAnimator.ofFloat(focusProgress, target).apply {
            duration = 200L
            interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
            addUpdateListener {
                focusProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val lineY = h - dp(1f)

        linePaint.strokeWidth = dp(1f)
        linePaint.color = palette.hairline
        canvas.drawLine(0f, lineY, w, lineY, linePaint)

        if (focusProgress > 0f) {
            linePaint.strokeWidth = dp(2f)
            linePaint.color = palette.accent
            val cx = w / 2f
            val halfWidth = (w / 2f) * focusProgress
            canvas.drawLine(cx - halfWidth, lineY, cx + halfWidth, lineY, linePaint)
        }
    }
}
