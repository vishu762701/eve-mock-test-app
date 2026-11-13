/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Cells/RadioCell.java:30-130 @ 3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.TelegramRadioIndicator

/**
 * Telegram RadioCell port.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Cells/RadioCell.java
 * - Row height: 50dp
 * - Text: 16sp (RadioCell.java:62)
 * - Radio: 22x22dp frame, 21dp margin (RadioCell.java:75)
 * - Divider: 1 physical pixel at bottom
 * - Ripple selector feedback
 * - Disabled alpha: 0.5
 */
class RadioCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    val textView: TextView = TextView(context)
    private val radioIndicator = TelegramRadioIndicator(density, dp(22f)) { true }
    private val radioView = View(context).apply {
        background = radioIndicator
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var needDivider: Boolean = false
        set(value) {
            field = value
            setWillNotDraw(!value)
            invalidate()
        }

    var isChecked: Boolean
        get() = radioIndicator.progress >= 0.5f
        set(value) {
            radioIndicator.setChecked(value, false)
        }

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        dividerPaint.color = palette.hairline
        dividerPaint.strokeWidth = 1f
        setWillNotDraw(true)

        radioIndicator.setColors(palette.textSecondary, palette.accent)

        background = TelegramThemeSelectors.getSelectorDrawable(palette.actionSelector)
        isFocusable = true
        isClickable = true
        minimumHeight = dp(50f)

        // 1. Text: 16sp, start margin 21dp, right margin 56dp
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        textView.setTextColor(palette.textPrimary)
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.maxLines = 1
        addView(textView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(21f)
            marginEnd = dp(56f)
        })

        // 2. Radio button: 22x22dp at END with 21dp margin
        addView(radioView, LayoutParams(dp(22f), dp(22f), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(21f)
        })

        setOnClickListener {
            setChecked(!isChecked, true)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1.0f else 0.5f
        textView.isEnabled = enabled
        radioView.isEnabled = enabled
    }

    fun setText(text: CharSequence, checked: Boolean, divider: Boolean) {
        textView.text = text
        radioIndicator.setChecked(checked, false)
        needDivider = divider
        setWillNotDraw(!divider)
    }

    fun setChecked(checked: Boolean, animated: Boolean) {
        radioIndicator.setChecked(checked, animated)
    }

    fun updatePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        dividerPaint.color = palette.hairline
        background = TelegramThemeSelectors.getSelectorDrawable(palette.actionSelector)
        textView.setTextColor(palette.textPrimary)
        radioView.invalidate()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (needDivider) {
            canvas.drawLine(
                dp(21f).toFloat(),
                (measuredHeight - 1).toFloat(),
                measuredWidth.toFloat(),
                (measuredHeight - 1).toFloat(),
                dividerPaint
            )
        }
    }
}
