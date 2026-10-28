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

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine

internal object TelegramDialogSourceBackground {
    fun apply(dialog: AlertDialog) {
        val drawable = AppCompatResources.getDrawable(
            dialog.context, R.drawable.telegram_dialog_popup_fixed_alert4
        )?.mutate() ?: return
        val palette = TelegramThemeEngine.resolveCurrentPalette(dialog.context)
        // Original AlertDialog.java:307,314-317: dialogBackground role and MULTIPLY.
        drawable.colorFilter = PorterDuffColorFilter(
            palette.dialogSurface, PorterDuff.Mode.MULTIPLY
        )
        dialog.window?.setBackgroundDrawable(drawable)
    }
}
