package com.eve.app.designsystem

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.ui.home.TelegramDialogSourceBackground
import com.eve.app.util.FontManager

/**
 * Original Telegram ActionBar/AlertDialog.java:1137-1207 uses 64dp minimum
 * button width, 12dp horizontal padding and 40dp visual button rows.
 * Theme.java:4802-4808 supplies a transparent 20dp round-rect masked
 * ripple (0x19 alpha) tinted from each button's real text color.
 *
 * EVE keeps its AppCompat host, callback wiring, accessible 48dp minimum
 * touch targets and destructive owner-red text. This renderer remains ADAPTED;
 * Android RippleDrawable substitutes for BaseCell.RippleDrawableSafe.
 */
private fun applyTelegramDialogActionAtoms(dialog: AlertDialog) {
    val density = dialog.context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density + 0.5f).toInt()
    for (which in intArrayOf(
        AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL
    )) {
        val button = dialog.getButton(which) ?: continue
        button.typeface = FontManager.telegramMediumTypeface(dialog.context)
        button.minWidth = maxOf(button.minWidth, dp(64))
        button.minHeight = maxOf(button.minHeight, dp(48))
        button.setPadding(dp(12), button.paddingTop, dp(12), button.paddingBottom)

        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setColor(Color.WHITE)
        }
        val ink = button.currentTextColor
        val selectorColor = (ink and 0x00FFFFFF) or 0x19000000
        // Original Theme.getRoundRectSelectorDrawable() has no idle fill.
        button.backgroundTintList = null
        button.background = RippleDrawable(
            ColorStateList.valueOf(selectorColor), null, mask
        )
    }
}

/** EVE-owned destructive confirmations only; native Android permission UI is untouched. */
fun AlertDialog.Builder.showDestructive(): AlertDialog = show().also { dialog ->
    TelegramDialogSourceBackground.apply(dialog, preserveDestructivePositive = true)
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
        ContextCompat.getColorStateList(dialog.context, R.color.design_destructive_text)
    )
    applyTelegramDialogActionAtoms(dialog)
}

/** Source-backed EVE dialog atoms while preserving each existing builder callback. */
fun AlertDialog.Builder.showTelegramSourceDialog(): AlertDialog = show().also { dialog ->
    TelegramDialogSourceBackground.apply(dialog)
    applyTelegramDialogActionAtoms(dialog)
}
