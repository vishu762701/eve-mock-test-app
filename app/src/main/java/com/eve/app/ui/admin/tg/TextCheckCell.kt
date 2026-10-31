/*
 * Derived from Telegram Android TextCheckCell.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

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
import com.eve.app.designsystem.TelegramSwitch
import com.eve.app.theme.TelegramThemeEngine

/**
 * ADAPTED EVE admin rights cell using source-exact Telegram row metrics.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Cells/TextCheckCell.java:70-120
 * - Height: 50dp
 * - Start padding: 21dp (or 58dp if icon present)
 * - Switch: 37x20dp, 22dp end margin (TextCheckCell.java:124-126)
 * - Text: 16sp
 * - Subtitle: 13sp
 * - Row: 50dp single-line, 64dp with subtitle, plus 1px divider (TextCheckCell.java:147-154)
 * - Divider: 1 physical pixel at bottom (TextCheckCell.java:395-403)
 */
class TextCheckCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

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

        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)

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
        textLayout.addView(subtitleView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val textLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = offsetFromImage
            marginEnd = dp(70f)
        }
        addView(textLayout, textLp)

        // 3. Telegram Switch
        val switchLp = LayoutParams(dp(37f), dp(20f), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(22f)
        }
        checkBox.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(checkBox, switchLp)

        setOnClickListener {
            if (isEnabled) checkBox.isChecked = !checkBox.isChecked
        }
    }

    fun setTextAndCheck(text: CharSequence, checked: Boolean, needDivider: Boolean = true) {
        textView.text = text
        contentDescription = text
        this.needDivider = needDivider
        checkBox.setCheckedSilently(checked)
    }

    fun setSubtitle(subtitle: CharSequence?) {
        if (!subtitle.isNullOrBlank()) {
            subtitleView.text = subtitle
            subtitleView.visibility = View.VISIBLE
        } else {
            subtitleView.visibility = View.GONE
        }
        requestLayout() // Telegram TextCheckCell.java:148-153: 50dp/64dp measurement.
    }

    fun setIcon(iconResId: Int) {
        iconView.setImageResource(iconResId)
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        iconView.setColorFilter(palette.textSecondary)
        iconView.visibility = View.VISIBLE
        offsetFromImage = dp(58f)
        (textLayout.layoutParams as? LayoutParams)?.marginStart = offsetFromImage
    }

    fun setOnCheckedChangeListener(listener: TelegramSwitch.OnCheckedChangeListener?) {
        checkBox.setOnCheckedChangeListener(listener)
    }

    override fun onInitializeAccessibilityNodeInfo(info: android.view.accessibility.AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Switch"
        info.isCheckable = checkBox.visibility == View.VISIBLE
        info.isChecked = isChecked
        info.text = listOf(textView.text, subtitleView.text).filter { it.isNotBlank() }.joinToString(". ")
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        checkBox.isEnabled = enabled
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
            val start = if (iconView.visibility == View.VISIBLE) offsetFromImage.toFloat() else dp(20f).toFloat()
            val y = height - 1f
            canvas.drawLine(if (layoutDirection == View.LAYOUT_DIRECTION_RTL) 0f else start, y,
                if (layoutDirection == View.LAYOUT_DIRECTION_RTL) width - start else width.toFloat(), y, dividerPaint)
        }
    }
}
