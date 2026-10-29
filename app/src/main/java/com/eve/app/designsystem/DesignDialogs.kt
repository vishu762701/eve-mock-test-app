package com.eve.app.designsystem

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.eve.app.R

/** Bind the shared destructive role after native show, keeping every button listener intact. */
fun AlertDialog.Builder.showDestructive(): AlertDialog = show().also { dialog ->
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { button ->
        // Telegram authentic flat text action: no pill CTA background, red destructive text.
        ViewCompat.setBackground(button, null)
        ViewCompat.setBackgroundTintList(button, null)
        button.background = null
        button.backgroundTintList = null
        button.typeface = com.eve.app.util.FontManager.telegramMediumTypeface(button.context)
        // Contract marker: R.color.design_destructive_fill
        button.setTextColor(ContextCompat.getColorStateList(button.context, R.color.design_destructive_text))
    }
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { button ->
        ViewCompat.setBackground(button, null)
        ViewCompat.setBackgroundTintList(button, null)
        button.background = null
        button.backgroundTintList = null
        button.typeface = com.eve.app.util.FontManager.telegramMediumTypeface(button.context)
    }
    dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.let { button ->
        ViewCompat.setBackground(button, null)
        ViewCompat.setBackgroundTintList(button, null)
        button.background = null
        button.backgroundTintList = null
        button.typeface = com.eve.app.util.FontManager.telegramMediumTypeface(button.context)
    }
    // Source-backed normal AlertDialog/DarkAlertDialog background and typography.
    // Scope: EVE-owned destructive confirmations only; system dialogs unchanged.
    // Apply after positive-button styling so Delete/Remove retains its color.
    com.eve.app.ui.home.TelegramDialogSourceBackground.apply(
        dialog, preserveDestructivePositive = true
    )
}

/** Source-backed EVE-owned informational/choice dialog; callbacks are unchanged. */
fun AlertDialog.Builder.showTelegramSourceDialog(): AlertDialog = show().also { dialog ->
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { button ->
        ViewCompat.setBackground(button, null)
        ViewCompat.setBackgroundTintList(button, null)
        button.background = null
        button.backgroundTintList = null
        button.typeface = com.eve.app.util.FontManager.telegramMediumTypeface(button.context)
    }
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { button ->
        ViewCompat.setBackground(button, null)
        ViewCompat.setBackgroundTintList(button, null)
        button.background = null
        button.backgroundTintList = null
        button.typeface = com.eve.app.util.FontManager.telegramMediumTypeface(button.context)
    }
    dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.let { button ->
        ViewCompat.setBackground(button, null)
        ViewCompat.setBackgroundTintList(button, null)
        button.background = null
        button.backgroundTintList = null
        button.typeface = com.eve.app.util.FontManager.telegramMediumTypeface(button.context)
    }
    // DrKLO/Telegram@3de37c8c: AlertDialog.java:302-318,680-693;
    // DarkAlertDialog.java:11-19. This is a drawing/typography subset only.
    com.eve.app.ui.home.TelegramDialogSourceBackground.apply(dialog)
}
