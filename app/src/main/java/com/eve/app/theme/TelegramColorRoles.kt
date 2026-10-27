package com.eve.app.theme

import android.graphics.Color
import androidx.core.graphics.ColorUtils

/** Verified original source defaults. See docs/color-migration/source-role-contract.json.
 * EVE uses Theme.getColor's default-theme branch for Light, raw night.attheme for Night.
 * No built-in accent transform is applied. Imported themes remain explicit overrides.
 */
object TelegramColorRoles {
    val defaults: Map<String, Int> = mapOf(
        // Bulletin.java / ThemeColors.java values supplied in the Mission 04 contract.
        "undo_background" to 0xEA272F38.toInt(),
        "undo_infoColor" to 0xFFFFFFFF.toInt(),
        "undo_cancelColor" to 0xFF85CAFF.toInt(),
        "windowBackgroundGray" to 0xFFF1F1F3.toInt(),
        "windowBackgroundWhite" to 0xFFFFFFFF.toInt(),
        "windowBackgroundWhiteBlackText" to 0xFF1A1D21.toInt(),
        "windowBackgroundWhiteGrayText" to 0xFF808384.toInt(),
        "dialogBackground" to 0xFFFFFFFF.toInt(),
        "dialogTextBlack" to 0xFF1A1D21.toInt(),
        "dialogTextGray" to 0xFF348BC1.toInt(),
        "glass_defaultIcon" to 0x991B2227.toInt(),
        "glass_defaultText" to 0x991B2227.toInt(),
        "glass_targetMainTabs" to 0xFFFFFFFF.toInt(),
        "glass_targetMainTopPanel" to 0xFFFFFFFF.toInt(),
        "glass_tabSelected" to 0xFF1A91E6.toInt(),
        "glass_tabSelectedText" to 0xFF0D7FCF.toInt(),
        "glass_tabUnselected" to 0xFF1A1D21.toInt(),
        "profile_actionBackground" to 0xFFFFFFFF.toInt(),
        "profile_tabText" to 0xFF878C90.toInt(),
        "profile_tabSelectedText" to 0xFF3A95D5.toInt(),
        "profile_tabSelectedLine" to 0xFF4FA6E9.toInt(),
        "profile_tabSelector" to 0x0F000000.toInt(),
        "chat_messagePanelSend" to 0xFF229AF0.toInt(),
        "actionBarDefaultSelector" to 0x121A1D21.toInt(),
        "chats_menuItemIcon" to 0xFF889198.toInt(),
        "divider" to 0xFFD9D9D9.toInt(),
        "listSelector" to 0x0F000000.toInt(),
        "actionBarDefaultSubmenuBackground" to 0xFFFFFFFF.toInt(),
        // ThemeColors.java:203-206: Telegram original Day submenu chrome.
        "actionBarDefaultSubmenuItem" to 0xFF1A1D21.toInt(),
        "actionBarDefaultSubmenuItemIcon" to 0xFF1A1D21.toInt(),
        "actionBarDefaultSubmenuSeparator" to 0xFFF5F5F5.toInt()
    )

    val fallbackKeys = mapOf(
        "glass_defaultIcon" to "chat_messagePanelIcons",
        "glass_defaultText" to "chat_messagePanelText",
        "glass_targetMainTabs" to "dialogBackground",
        "glass_targetMainTopPanel" to "dialogBackground",
        "glass_tabSelected" to "chat_messagePanelSend",
        "glass_tabSelectedText" to "chat_messagePanelSend",
        "glass_tabUnselected" to "windowBackgroundWhiteBlackText",
        "profile_tabText" to "windowBackgroundWhiteGrayText",
        "profile_tabSelectedText" to "windowBackgroundWhiteBlueHeader",
        "profile_tabSelectedLine" to "windowBackgroundWhiteBlueHeader",
        "profile_tabSelector" to "listSelector",
        "listSelector" to "listSelectorSDK21"
    )

    /** Theme.java:9024–9052: direct active key, ONE active fallback, then source default.
     * Do not walk arbitrary fallback chains in the active theme or apply Light's default
     * before checking Night's explicit chat_messagePanelSend/text keys.
     */
    fun resolve(active: Map<String, Int>, key: String): Int {
        val direct = active[key]
        if (direct != null) return if (key in opaqueKeys) direct or 0xff000000.toInt() else direct
        fallbackKeys[key]?.let { active[it]?.let { color -> return color } }
        return defaults.getValue(key)
    }
    private val opaqueKeys = setOf("windowBackgroundWhite", "windowBackgroundGray")

    /** EVE AA accommodation: preserve source RGB and first increase alpha; if needed,
     * minimally blend toward the contrast-maximizing endpoint. Test every backing role.
     * Raw source colors remain in defaults/the resolved glass roles for provenance/overlay.
     */
    fun accessibleForeground(source: Int, backgrounds: List<Int>, ratio: Double = 4.5): Int {
        // Imported themes may intentionally use translucent surfaces. Calculate contrast
        // on their opaque backing without changing the imported ARGB or file bytes.
        val backing = backgrounds.firstOrNull { Color.alpha(it) == 255 } ?: Color.BLACK
        val opaqueBackgrounds = backgrounds.map { ColorUtils.compositeColors(it, backing) }
        fun passes(color: Int) = opaqueBackgrounds.all { bg ->
            ColorUtils.calculateContrast(ColorUtils.compositeColors(color, bg), bg) >= ratio
        }
        for (alpha in Color.alpha(source)..255) {
            val candidate = ColorUtils.setAlphaComponent(source, alpha)
            if (passes(candidate)) return candidate
        }
        val opaque = source or 0xff000000.toInt()
        val endpoint = listOf(Color.BLACK, Color.WHITE).maxByOrNull { color ->
            opaqueBackgrounds.minOf { ColorUtils.calculateContrast(color, it) }
        }!!
        for (step in 1..255) {
            val candidate = ColorUtils.blendARGB(opaque, endpoint, step / 255f)
            if (passes(candidate)) return candidate
        }
        return endpoint // Conflicting custom backdrops are explicitly outside fixed-default fidelity.
    }
}
