package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TelegramDefaultIconButtonThemeTest {
    @Test fun untintedIconButtonsUseTelegramActionColorButKeepTouchAndPress() {
        val xml = File("src/main/res/values/design_widgets.xml").readText()
        val begin = xml.indexOf("<style name=\"Widget.Eve.Token.IconButton\"")
        assertTrue(begin >= 0)
        val section = xml.substring(begin, xml.indexOf("</style>", begin))
        assertTrue(section.contains("<item name=\"tint\">@color/telegram_type_primary</item>"))
        assertTrue(section.contains("<item name=\"android:minWidth\">@dimen/design_touch_min</item>"))
        assertTrue(section.contains("<item name=\"android:minHeight\">@dimen/design_touch_min</item>"))
        assertTrue(section.contains("<item name=\"android:stateListAnimator\">@animator/design_press_scale</item>"))
        assertTrue(File("src/main/res/values/telegram_colors.xml").readText()
            .contains("<color name=\"telegram_type_primary\">#FF1A1D21</color>"))
        assertTrue(File("src/main/res/values-night/telegram_colors.xml").readText()
            .contains("<color name=\"telegram_type_primary\">#FFFFFFFF</color>"))
    }
}
