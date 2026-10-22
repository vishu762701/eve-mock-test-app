package com.eve.app.designsystem

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.eve.app.R

/** Bind the shared destructive role after native show, keeping every button listener intact. */
fun AlertDialog.Builder.showDestructive(): AlertDialog = show().also { dialog ->
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { button ->
        DesignType.apply(button, DesignType.Role.BUTTON)
        // Keep EVE's required destructive contrast/handler, but use Telegram's actual
        // AlertDialog action font, which DesignType.BUTTON would otherwise replace.
        button.typeface = com.eve.app.util.FontManager.telegramMediumTypeface(button.context)
        ViewCompat.setBackgroundTintList(button, ContextCompat.getColorStateList(button.context, R.color.design_destructive_fill))
        button.setTextColor(ContextCompat.getColorStateList(button.context, R.color.design_destructive_text))
    }
}
