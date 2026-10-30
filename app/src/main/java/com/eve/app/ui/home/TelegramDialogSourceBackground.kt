/*
 * Adapted UI-only background setup from the officially verified Telegram Android
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * TMessagesProj/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java:302-318,680-693.
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL version 2 or later.
 *
 * EVE adapter uses the original unmodified popup_fixed_alert4.9.png assets and
 * PorterDuff MULTIPLY with the original dialogBackground theme role.
 * The EVE MaterialAlertDialog host, layout, buttons, behavior and admin access
 * checks are NOT the Telegram AlertDialog renderer and remain unchanged.
 */
package com.eve.app.ui.home

import android.graphics.Color
import android.graphics.PorterDuff
import android.widget.TextView
import android.util.TypedValue
import com.eve.app.util.FontManager
import android.graphics.PorterDuffColorFilter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine

internal object TelegramDialogSourceBackground {
    fun apply(dialog: AlertDialog, preserveDestructivePositive: Boolean = false) {
        val drawable = AppCompatResources.getDrawable(
            dialog.context, R.drawable.telegram_dialog_popup_fixed_alert4
        )?.mutate() ?: return
        val palette = TelegramThemeEngine.resolveCurrentPalette(dialog.context)
        // Normal AlertDialog.java:307,314-317 uses the dialogBackground role.
        // The original DarkAlertDialog.java:13-17 *explicitly overrides* it with
        // #262626 and assigns white to dialog text, buttons and scroll glow.
        // Scope: EVE-owned confirmations and choice dialogs, not system dialogs.
        // A destructive positive action is the only intentional source color exception.
        val surface = if (palette.isDark) 0xFF262626.toInt() else palette.dialogSurface
        drawable.colorFilter = PorterDuffColorFilter(surface, PorterDuff.Mode.MULTIPLY)
        dialog.window?.setBackgroundDrawable(drawable)
        dialog.window?.setDimAmount(0.5f)

        // Original AlertDialog.java:489-504: display width minus 56dp;
        // phone max356dp, small tablet446dp, large tablet496dp.
        // AndroidUtilities.java:3002-3004: small tablet min side <=690dp.
        // This window sizing atom is source-backed; the Material dialog host
        // remains ADAPTED and is not the Telegram AlertDialog renderer.
        val dm = dialog.context.resources.displayMetrics
        val calculatedWidth = dm.widthPixels - (56 * dm.density).toInt()
        val isTablet = (dialog.context.resources.configuration.screenLayout and
                android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) >=
                android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
        val minSideDp = minOf(dm.widthPixels, dm.heightPixels) / dm.density
        val maxWidthDp = when {
            !isTablet -> 356
            minSideDp <= 690f -> 446
            else -> 496
        }
        val maxWidth = (maxWidthDp * dm.density).toInt()
        val backgroundPaddings = android.graphics.Rect()
        drawable.getPadding(backgroundPaddings)
        val dialogWidth = minOf(maxWidth, calculatedWidth) + backgroundPaddings.left + backgroundPaddings.right
        dialog.window?.let { window ->
            val lp = window.attributes
            if (lp != null && lp.width != dialogWidth) {
                lp.width = dialogWidth
                window.attributes = lp
            }
        }
        // Original AlertDialog.java:809-821,874-878,1137-1204 uses title=20,
        // message=16, actions=16 and AndroidUtilities.bold() (Roboto Medium).
        // EVE deliberately specifies SP instead of original DIP to retain font scaling.
        dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.let { title ->
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            title.typeface = FontManager.telegramMediumTypeface(dialog.context)
        }
        dialog.findViewById<TextView>(android.R.id.message)?.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, 16f
        )
        for (which in intArrayOf(
            AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL
        )) {
            dialog.getButton(which)?.let { button ->
                button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                button.typeface = FontManager.telegramMediumTypeface(dialog.context)
                // Keep EVE's existing 48dp-or-larger accessible targets and styling.
            }
        }

        if (palette.isDark) {
            dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(Color.WHITE)
            dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(Color.WHITE)
            // Original DarkAlertDialog.java:11-19: key_dialogButton=white.
            // Do not overwrite EVE's explicitly authorized red Delete/Remove action.
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(Color.WHITE)
            if (!preserveDestructivePositive) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.WHITE)
            }
        } else {
            // Day mode: Theme.key_dialogButton (Telegram accent blue #298ACF)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(palette.accent)
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(palette.accent)
            if (!preserveDestructivePositive) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(palette.accent)
            }
        }
    }
}
