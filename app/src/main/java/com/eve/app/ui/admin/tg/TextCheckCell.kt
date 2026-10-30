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
 * Official Telegram TextCheckCell ported for EVE Admin Hub.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Cells/TextCheckCell.java:70-120
 * - Height: 50dp
 * - Start padding: 21dp (or 58dp if icon present)
 * - Switch: 37x20dp on the right (21dp margin end)
 * - Text: 16sp
 * - Subtitle: 13sp
 * - Divider: hairline 1px at bottom
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
        }

    var isChecked: Boolean
        get() = checkBox.isChecked
        set(value) {
            checkBox.isChecked = value
        }

    private var offsetFromImage = dp(21f)

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        dividerPaint.color = palette.hairline
        dividerPaint.strokeWidth = dp(0.75f).coerceAtLeast(1).toFloat()
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
            marginEnd = dp(21f)
        }
        addView(checkBox, switchLp)

        setOnClickListener {
            checkBox.isChecked = !checkBox.isChecked
        }
    }

    fun setTextAndCheck(text: CharSequence, checked: Boolean, needDivider: Boolean = true) {
        textView.text = text
        this.needDivider = needDivider
        checkBox.isChecked = checked
    }

    fun setSubtitle(subtitle: CharSequence?) {
        if (!subtitle.isNullOrBlank()) {
            subtitleView.text = subtitle
            subtitleView.visibility = View.VISIBLE
        } else {
            subtitleView.visibility = View.GONE
        }
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (needDivider) {
            val start = offsetFromImage.toFloat()
            val y = height - dividerPaint.strokeWidth
            canvas.drawLine(start, y, width.toFloat(), y, dividerPaint)
        }
    }
}
