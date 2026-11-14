/*
 * Derived from Telegram Android TextInfoPrivacyCell.java @ 3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official Telegram TextInfoPrivacyCell ported for EVE.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Cells/TextInfoPrivacyCell.java
 * - Text size: 14sp
 * - Default padding: 21dp horizontal, 10dp top, 17dp bottom
 * - Supports dynamic link movement and theme updates
 */
class TextInfoPrivacyCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    val textView: TextView = TextView(context)
    private var topPadding: Int = 10
    private var bottomPadding: Int = 17

    fun getText(): CharSequence? = textView.text

    fun setText(info: CharSequence?) {
        textView.text = info
    }

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        textView.setTextColor(palette.textSecondary)
        textView.setLinkTextColor(palette.accent)
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.gravity = Gravity.START
        updatePadding()

        val lp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START
        )
        addView(textView, lp)
    }

    private fun updatePadding() {
        textView.setPadding(dp(21f), dp(topPadding.toFloat()), dp(21f), dp(bottomPadding.toFloat()))
    }

    fun setTopPadding(dpValue: Int) {
        if (topPadding != dpValue) {
            topPadding = dpValue
            updatePadding()
        }
    }

    fun setBottomPadding(dpValue: Int) {
        if (bottomPadding != dpValue) {
            bottomPadding = dpValue
            updatePadding()
        }
    }

    fun updatePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        textView.setTextColor(palette.textSecondary)
        textView.setLinkTextColor(palette.accent)
        invalidate()
    }
}
