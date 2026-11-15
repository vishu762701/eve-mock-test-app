package com.eve.app.ui.common

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView

/**
 * 1:1 Telegram status badge view.
 */
class TelegramBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    fun setVerdict(verdictText: String, @DrawableRes backgroundRes: Int, @ColorInt textColor: Int) {
        text = verdictText
        setBackgroundResource(backgroundRes)
        setTextColor(textColor)
    }
}
