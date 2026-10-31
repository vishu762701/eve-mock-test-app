package com.eve.app.ui.profile

import android.widget.ImageView
import android.widget.LinearLayout
import com.eve.app.R

/**
 * Telegram ProfileActionsView.java:412-460,1215-1234 switches outline/filled
 * original glyphs by button brightness and parent expansion.
 *
 * EVE has light buttons in Day (#E5E5EA) and dark in Night (#1C1C1E).
 * The full-screen photo viewer "parentExpanded" transition is not present,
 * so this binding implements the source's stable 0f-expanded endpoints only.
 */
internal object ProfileActionIcons {
    data class Resources(val photo: Int, val edit: Int, val settings: Int)

    fun resources(darkSurface: Boolean): Resources =
        if (darkSurface) {
            Resources(
                R.drawable.telegram_profile_photo,
                R.drawable.telegram_profile_edit,
                R.drawable.telegram_profile_settings
            )
        } else {
            Resources(
                R.drawable.telegram_profile_outline_photo,
                R.drawable.telegram_profile_outline_edit,
                R.drawable.telegram_profile_outline_settings
            )
        }

    fun applyTo(buttons: List<LinearLayout>, darkSurface: Boolean) {
        require(buttons.size == 3)
        val assets = resources(darkSurface)
        for ((index, asset) in listOf(assets.photo, assets.edit, assets.settings).withIndex()) {
            // Existing icon is the first child; preserve the button label,
            // click handler, source bounce physics, and content fade state.
            (buttons[index].getChildAt(0) as ImageView).setImageResource(asset)
        }
    }
}
