package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Telegram ActionBar.java:268 + Theme.java:4856-4858,4864,4936-4940. */
class TelegramDefaultIconButtonThemeTest {
    @Test fun iconActionsUseTelegramNative20DpSelectorNotInventedScale() {
        val style = File("src/main/res/values/design_widgets.xml").readText()
        val begin = style.indexOf("<style name=\"Widget.Eve.Token.IconButton\"")
        assertTrue(begin >= 0)
        val section = style.substring(begin, style.indexOf("</style>", begin))
        assertTrue(section.contains("<item name=\"tint\">@color/telegram_type_primary</item>"))
        assertTrue(section.contains("<item name=\"android:minWidth\">@dimen/design_touch_min</item>"))
        assertTrue(section.contains("<item name=\"android:minHeight\">@dimen/design_touch_min</item>"))
        assertTrue(section.contains("<item name=\"android:background\">@drawable/telegram_action_icon_ripple</item>"))
        assertTrue(section.contains("<item name=\"android:stateListAnimator\">@null</item>"))
        val ripple = File("src/main/res/drawable/telegram_action_icon_ripple.xml").readText()
        assertTrue(ripple.contains("<ripple"))
        assertTrue(ripple.contains("android:radius=\"20dp\""))
        assertTrue(ripple.contains("android:color=\"@color/telegram_action_selector\""))
        assertTrue(File("src/main/res/values/telegram_colors.xml").readText()
            .contains("<color name=\"telegram_action_selector\">#121A1D21</color>"))
        assertTrue(File("src/main/res/values-night/telegram_colors.xml").readText()
            .contains("<color name=\"telegram_action_selector\">#16FFFFFF</color>"))
        // Preserve generic scale motion for EVE's non-icon card, tab and row controls.
        assertTrue(style.contains("<item name=\"android:stateListAnimator\">@animator/design_press_scale</item>"))
    }
}
