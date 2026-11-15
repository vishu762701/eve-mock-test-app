package com.eve.app.ui.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * 1:1 Telegram flat counter slot text view replacing legacy UiArcSlotTextView.
 */
class TelegramSlotTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    fun setSlotValue(number: Int, animate: Boolean = true) {
        text = number.toString()
    }

    fun setSlotValue(number: Long, animate: Boolean = true) {
        text = number.toString()
    }
}
