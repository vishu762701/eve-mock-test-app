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

    private var offsetFromImage = dp(21f)

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

        val textLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = offsetFromImage
            marginEnd = dp(70f)
        }
        addView(textLayout, textLp)

        // 3. Switch: 37x20dp frame, 21dp from end
        val switchLp = LayoutParams(dp(37f), dp(20f), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(21f)
        }
        checkBox.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(checkBox, switchLp)

        setOnClickListener {
            if (isEnabled) checkBox.isChecked = !checkBox.isChecked
        }
    }

    fun setChecked(checked: Boolean) {
        this.isChecked = checked
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1.0f else 0.5f
        checkBox.isEnabled = enabled
        textView.isEnabled = enabled
        subtitleView.isEnabled = enabled
    }

    fun setTextAndCheck(text: CharSequence, checked: Boolean, needDivider: Boolean = true) {
        textView.text = text
        contentDescription = text
        this.needDivider = needDivider
        checkBox.setCheckedSilently(checked)
    }

    fun setTextAndValueAndCheck(text: CharSequence, subtitle: CharSequence?, checked: Boolean, divider: Boolean = true) {
        textView.text = text
        setSubtitle(subtitle)
        checkBox.setCheckedSilently(checked)
        needDivider = divider
        requestLayout()
    }

    fun setSubtitle(subtitle: CharSequence?) {
        if (!subtitle.isNullOrBlank()) {
            subtitleView.text = subtitle
            subtitleView.visibility = View.VISIBLE
        } else {
            subtitleView.visibility = View.GONE
        }
        requestLayout()
    }

    fun setIcon(resId: Int) {
        if (resId != 0) {
            iconView.setImageResource(resId)
            val palette = TelegramThemeEngine.resolveCurrentPalette(context)
            iconView.setColorFilter(palette.textSecondary)
            iconView.visibility = View.VISIBLE
            offsetFromImage = dp(58f)
            (textLayout.layoutParams as? LayoutParams)?.marginStart = offsetFromImage
        } else {
            iconView.visibility = View.GONE
            offsetFromImage = dp(21f)
            (textLayout.layoutParams as? LayoutParams)?.marginStart = offsetFromImage
        }
    }

    fun setOnCheckedChangeListener(listener: TelegramSwitch.OnCheckedChangeListener?) {
        checkBox.setOnCheckedChangeListener(listener)
    }

    fun updatePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        dividerPaint.color = palette.hairline
        background = TelegramThemeSelectors.getSelectorDrawable(palette.actionSelector)
        textView.setTextColor(palette.textPrimary)
        subtitleView.setTextColor(palette.textSecondary)
        checkBox.invalidate()
        invalidate()
    }

    override fun onInitializeAccessibilityNodeInfo(info: android.view.accessibility.AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Switch"
        info.isCheckable = checkBox.visibility == View.VISIBLE
        info.isChecked = isChecked
        info.text = listOf(textView.text, subtitleView.text).filter { it.isNotBlank() }.joinToString(". ")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val rowHeight = dp(if (subtitleView.visibility == View.VISIBLE) 64f else 50f) + (if (needDivider) 1 else 0)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(rowHeight, MeasureSpec.EXACTLY)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (needDivider) {
            val start = if (iconView.visibility == View.VISIBLE) offsetFromImage.toFloat() else dp(21f).toFloat()
            val y = height - 1f
            canvas.drawLine(
                if (layoutDirection == View.LAYOUT_DIRECTION_RTL) 0f else start,
                y,
                if (layoutDirection == View.LAYOUT_DIRECTION_RTL) width - start else width.toFloat(),
                y,
                dividerPaint
            )
        }
    }
}
