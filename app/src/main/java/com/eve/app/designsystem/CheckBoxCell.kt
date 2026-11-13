/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Cells/CheckBoxCell.java:40-150 @ 3de37c8cfb039452be7398909461da6288babf15
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
import com.eve.app.ui.common.TelegramCheckBox2

/**
 * Telegram CheckBoxCell port.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Cells/CheckBoxCell.java
 * - Row height: 50dp
 * - Start padding: 21dp (56dp text margin with round checkbox)
 * - Text: 16sp
 * - TelegramCheckBox2: 21dp size
 * - Divider: 1 physical pixel at bottom
 * - Ripple selector feedback
 * - Disabled alpha: 0.5
 */
class CheckBoxCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    val textView: TextView = TextView(context)
    val valueTextView: TextView = TextView(context)
    val checkBox: TelegramCheckBox2 = TelegramCheckBox2(context)

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var needDivider: Boolean = false
        set(value) {
            field = value
            setWillNotDraw(!value)
            invalidate()
        }

    var isChecked: Boolean
        get() = checkBox.isChecked
        set(value) {
            checkBox.setChecked(value, false)
        }

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        dividerPaint.color = palette.hairline
        dividerPaint.strokeWidth = 1f
        setWillNotDraw(true)

        background = TelegramThemeSelectors.getSelectorDrawable(palette.actionSelector)
        isFocusable = true
        isClickable = true
        minimumHeight = dp(50f)

        // 1. Checkbox: 21dp, 21dp from start, centered vertically
        addView(checkBox, LayoutParams(dp(22f), dp(22f), Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(21f)
        })

        // 2. Text: 16sp, textPrimary, start margin 56dp (21dp + 22dp + 13dp)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        textView.setTextColor(palette.textPrimary)
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.maxLines = 1
        addView(textView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(56f)
            marginEnd = dp(21f)
        })

        // 3. Optional value text view
        valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        valueTextView.setTextColor(palette.textSecondary)
        valueTextView.visibility = View.GONE
        addView(valueTextView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(21f)
        })

        setOnClickListener {
            checkBox.setChecked(!checkBox.isChecked, true)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1.0f else 0.5f
        checkBox.isEnabled = enabled
        textView.isEnabled = enabled
    }

    fun setText(text: CharSequence, checked: Boolean, divider: Boolean) {
        textView.text = text
        checkBox.setChecked(checked, false)
        needDivider = divider
        setWillNotDraw(!divider)
    }

    fun setChecked(checked: Boolean, animated: Boolean) {
        checkBox.setChecked(checked, animated)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (needDivider) {
            canvas.drawLine(
                dp(56f).toFloat(),
                (measuredHeight - 1).toFloat(),
                measuredWidth.toFloat(),
                (measuredHeight - 1).toFloat(),
                dividerPaint
            )
        }
    }
}
