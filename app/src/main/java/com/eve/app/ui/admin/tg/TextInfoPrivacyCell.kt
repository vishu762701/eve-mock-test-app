/*
 * Derived from Telegram Android TextInfoPrivacyCell.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official Telegram TextInfoPrivacyCell ported for EVE Admin Hub.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Cells/TextInfoPrivacyCell.java:60-80
 * - Text size: 14sp
 * - Padding: 24dp horizontal, 10dp top, 17dp bottom
 * - Text color: windowBackgroundWhiteGrayText4
 */
class TextInfoPrivacyCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    val textView: TextView = TextView(context)

    var text: CharSequence?
        get() = textView.text
        set(value) {
            textView.text = value
        }

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        textView.setTextColor(palette.textSecondary)
        textView.gravity = Gravity.START
        textView.setPadding(dp(24f), dp(10f), dp(24f), dp(17f))

        addView(textView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START))
    }


}
