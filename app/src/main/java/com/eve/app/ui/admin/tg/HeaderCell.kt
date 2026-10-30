/*
 * Derived from Telegram Android HeaderCell.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official Telegram HeaderCell ported for EVE Admin Hub.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Cells/HeaderCell.java:80-120
 * - Text size: 14sp bold
 * - Min height: 40dp (height 40 - topMargin 7)
 * - Padding: 20dp horizontal, 7dp top margin
 * - Accessibility heading marked
 */
class HeaderCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val textView: TextView = TextView(context)
    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    fun getText(): CharSequence? = textView.text

    fun setText(title: CharSequence) {
        textView.text = title
    }

    init {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        textView.minHeight = dp(33f) // 40dp - 7dp topMargin

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        textView.setTextColor(palette.accent)

        val lp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.START or Gravity.TOP
        ).apply {
            setMargins(dp(20f), dp(7f), dp(20f), 0)
        }
        addView(textView, lp)

        ViewCompat.setAccessibilityHeading(this, true)
    }

    fun setTextColor(color: Int) {
        textView.setTextColor(color)
    }
}
