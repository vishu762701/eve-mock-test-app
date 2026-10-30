/*
 * Derived from Telegram Android TextDetailCell.java, Copyright Nikolai Kudashov, 2013-2018.
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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official Telegram TextDetailCell ported for EVE Admin Hub.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Cells/TextDetailCell.java:60-110
 * - Text: 16sp
 * - Value: 13sp
 * - Min height: 60dp
 * - Horizontal padding: 23dp
 * - Divider: hairline 1px at bottom
 */
class TextDetailCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private val textLayout: LinearLayout = LinearLayout(context)
    val textView: TextView = TextView(context)
    val valueTextView: TextView = TextView(context)

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var needDivider: Boolean = true
        set(value) {
            field = value
            setWillNotDraw(!value)
            invalidate()
        }

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
        minimumHeight = dp(60f)

        textLayout.orientation = LinearLayout.VERTICAL
        textLayout.gravity = Gravity.CENTER_VERTICAL

        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        textView.setTextColor(palette.textPrimary)
        textView.ellipsize = TextUtils.TruncateAt.END
        textLayout.addView(textView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        valueTextView.setTextColor(palette.textSecondary)
        val valueLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(2f)
        }
        textLayout.addView(valueTextView, valueLp)

        val layoutLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL).apply {
            setMargins(dp(23f), dp(8f), dp(23f), dp(8f))
        }
        addView(textLayout, layoutLp)
    }

    fun setTextAndValue(text: CharSequence, value: CharSequence, needDivider: Boolean = true) {
        textView.text = text
        valueTextView.text = value
        this.needDivider = needDivider
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (needDivider) {
            val start = dp(23f).toFloat()
            val y = height - dividerPaint.strokeWidth
            canvas.drawLine(start, y, width.toFloat(), y, dividerPaint)
        }
    }
}
