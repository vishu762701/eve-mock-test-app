package com.eve.app.ui.test

import android.content.res.ColorStateList
import android.graphics.Color
import com.eve.app.theme.TelegramThemeEngine
import com.google.android.material.button.MaterialButton

/** R08. The exam-specific primary action is an EVE owner monochrome override,
 * not an upstream Telegram dialog/button color. Other actions are unaffected. */
object TestPrimaryActionAppearance {
    fun apply(button: MaterialButton, night: Boolean = TelegramThemeEngine.isDarkActive(button.context)) {
        val enabledFill = if (night) Color.WHITE else Color.BLACK
        val enabledInk = if (night) Color.BLACK else Color.WHITE
        val disabledFill = if (night) 0xFF363638.toInt() else 0xFFE5E5EA.toInt()
        val disabledInk = if (night) Color.WHITE else Color.BLACK
        val states = arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf())
        button.backgroundTintList = ColorStateList(states, intArrayOf(disabledFill, enabledFill))
        button.setTextColor(ColorStateList(states, intArrayOf(disabledInk, enabledInk)))
        button.strokeWidth = 0
        button.rippleColor = ColorStateList.valueOf(
            if (night) 0x29000000 else 0x33FFFFFF)
        button.invalidate()
    }
}
