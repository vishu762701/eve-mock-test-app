package com.eve.app.ui.profile

import android.widget.ImageView
import android.widget.LinearLayout
import com.eve.app.R

/**
 * Telegram ProfileActionsView.java:412-460,1215-1234 switches outline/filled
 * original glyphs by button brightness and parent expansion.
 *
 * EVE has light buttons in Day (#E5E5EA) and dark in Night (#1C1C1E).
 * EVE's single-photo expansion retains the themed action surfaces below it.
 * The upstream parentExpanded icon/shader crossfade remains unported; this
 * binding implements verified stable light/dark endpoints and explicit ink.
 */
internal object ProfileActionIcons {
    data class Resources(val photo: Int, val edit: Int, val settings: Int)

    /**
     * AndroidUtilities.java:4925–4926 computes weighted perceived brightness
     * (not gamma-corrected ColorUtils.calculateLuminance).
     * ProfileActionsView.java:185–186 uses a strict > 0.72 threshold.
     */
    fun isLightButtonColor(color: Int): Boolean {
        val r = (color ushr 16) and 0xff
        val g = (color ushr 8) and 0xff
        val b = color and 0xff
        return (r * 0.2126f + g * 0.7152f + b * 0.0722f) / 255f > 0.72f
    }

    fun resourcesForColor(surfaceColor: Int): Resources =
        resources(darkSurface = !isLightButtonColor(surfaceColor))

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

    fun applyTo(buttons: List<LinearLayout>, surfaceColor: Int) {
        require(buttons.size == 3)
        val assets = resourcesForColor(surfaceColor)
        for ((index, asset) in listOf(assets.photo, assets.edit, assets.settings).withIndex()) {
            // Existing icon is the first child; preserve the button label,
            // click handler, source bounce physics, and content fade state.
            (buttons[index].getChildAt(0) as ImageView).apply {
                setImageResource(asset)
                imageTintList = android.content.res.ColorStateList.valueOf(
                    com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(context).textPrimary)
            }
        }
    }
}
