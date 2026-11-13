/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Cells/TextCheckCell.java:70-160 @ 3de37c8cfb039452be7398909461da6288babf15
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.eve.app.theme.TelegramThemeEngine

/**
 * Telegram TextCheckCell port.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Cells/TextCheckCell.java
 * - Height: 50dp single-line, 64dp with subtitle
 * - Start padding: 21dp (58dp if icon present)
 * - Switch: 37x20dp frame, 21dp end margin
 * - Text: 16sp
 * - Subtitle: 13sp
 * - Divider: 1 physical pixel at bottom
 * - Ripple selector feedback
 * - Disabled alpha: 0.5
 */
open class TextCheckCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    private val iconView: ImageView = ImageView(context)
    private val textLayout: LinearLayout = LinearLayout(context)
    val textView: TextView = TextView(context)
    val subtitleView: TextView = TextView(context)
    val checkBox: TelegramSwitch = TelegramSwitch(context)

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var needDivider: Boolean = true
        set(value) {
            field = value
            setWillNotDraw(!value)
            invalidate()
            requestLayout()
        }

    var isChecked: Boolean
        get() = checkBox.isChecked
        set(value) {
            checkBox.setCheckedSilently(value)
        }

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        dividerPaint.color = palette.hairline
        dividerPaint.strokeWidth = 1f
        setWillNotDraw(false)

        background = TelegramThemeSelectors.getSelectorDrawable(palette.actionSelector)

        isFocusable = true
        isClickable = true
        minimumHeight = dp(50f)

        // 1. Optional icon
        iconView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        iconView.visibility = View.GONE
        addView(iconView, LayoutParams(dp(24f), dp(24f), Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(16f)
        })

        // 2. Text layout
        textLayout.orientation = LinearLayout.VERTICAL
        textLayout.gravity = Gravity.CENTER_VERTICAL

        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        textView.setTextColor(palette.textPrimary)
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.maxLines = 1
        textLayout.addView(textView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        subtitleView.setTextColor(palette.textSecondary)
        subtitleView.ellipsize = TextUtils.TruncateAt.END
        subtitleView.maxLines = 1
        subtitleView.visibility = View.GONE
        textLayout.addView(subtitleView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(2f)
        })

        addView(textLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(21f)
            marginEnd = dp(64f)
        })

        // 3. Switch: 37x20dp frame, 21dp from end
        addView(checkBox, LayoutParams(dp(37f), dp(20f), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(21f)
        })

        setOnClickListener {
            checkBox.toggle()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1.0f else 0.5f
        checkBox.isEnabled = enabled
        textView.isEnabled = enabled
        subtitleView.isEnabled = enabled
    }

    fun setTextAndCheck(text: CharSequence, checked: Boolean, divider: Boolean) {
        textView.text = text
        subtitleView.visibility = View.GONE
        checkBox.setCheckedSilently(checked)
        needDivider = divider
        minimumHeight = dp(50f)
        requestLayout()
    }

    fun setTextAndValueAndCheck(text: CharSequence, subtitle: CharSequence?, checked: Boolean, divider: Boolean) {
        textView.text = text
        if (!subtitle.isNullOrBlank()) {
            subtitleView.text = subtitle
            subtitleView.visibility = View.VISIBLE
            minimumHeight = dp(64f)
        } else {
            subtitleView.visibility = View.GONE
            minimumHeight = dp(50f)
        }
        checkBox.setCheckedSilently(checked)
        needDivider = divider
        requestLayout()
    }

    fun setIcon(resId: Int) {
        if (resId != 0) {
            iconView.setImageResource(resId)
            iconView.visibility = View.VISIBLE
            (textLayout.layoutParams as? LayoutParams)?.marginStart = dp(58f)
        } else {
            iconView.visibility = View.GONE
            (textLayout.layoutParams as? LayoutParams)?.marginStart = dp(21f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (needDivider) {
            val start = (textLayout.layoutParams as? LayoutParams)?.marginStart ?: dp(21f)
            canvas.drawLine(start.toFloat(), (measuredHeight - 1).toFloat(), measuredWidth.toFloat(), (measuredHeight - 1).toFloat(), dividerPaint)
        }
    }
}
