package com.eve.app.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.designsystem.TelegramChip
import com.google.android.material.button.MaterialButton

/** The same shared actions used by Practice/PYQ and native visual verification. */
object StudentSelectionControls {
    fun action(context: Context): MaterialButton =
        (LayoutInflater.from(context).inflate(R.layout.item_student_action, null) as MaterialButton).apply {
            layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(context)
            rippleColor = ColorStateList.valueOf(palette.actionSelector)
            setTextColor(palette.textPrimary)
        }

    fun year(context: Context): TelegramChip = TelegramChip(context).apply {
        isCheckable = true
        isCheckedIconVisible = false
        setEnsureMinTouchTargetSize(true)
        chipMinHeight = resources.getDimension(R.dimen.design_pill_height)
        chipCornerRadius = chipMinHeight / 2
        chipStrokeWidth = 0f
        com.eve.app.designsystem.DesignType.apply(this, com.eve.app.designsystem.DesignType.Role.CHIP)
        val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(context)
        val accentColor = palette.accent
        val surfaceColor = if (palette.isDark) 0x1AFFFFFF else palette.surface
        val onAccentColor = palette.onAccent
        val textPrimaryColor = palette.textPrimary
        chipBackgroundColor = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(accentColor, surfaceColor)
        )
        setTextColor(
            ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(onAccentColor, textPrimaryColor)
            )
        )
        rippleColor = ColorStateList.valueOf(palette.actionSelector)
        chipStartPadding = resources.getDimension(R.dimen.design_pill_padding)
        chipEndPadding = chipStartPadding
    }
}
