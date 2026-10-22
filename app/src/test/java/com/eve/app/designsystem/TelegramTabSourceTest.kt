package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TelegramTabSourceTest {
    @Test fun nativeTabTypefaceAndSizeMatchSourceInAllQualifiers() {
        val xml = File("src/main/res/values/design_type.xml").readText()
        for (name in listOf("TextAppearance.Eve.Token.Tab", "TextAppearance.Eve.Token.Tab.Selected")) {
            val start = xml.indexOf("<style name=\"" + name + "\"")
            assertTrue("Missing " + name, start >= 0)
            val end = xml.indexOf("</style>", start)
            assertTrue("Incorrect source font " + name, xml.substring(start, end).contains("@font/telegram_roboto_medium"))
        }
        for (qualifier in listOf("values", "values-sw360dp", "values-sw600dp")) {
            val dims = File("src/main/res/" + qualifier + "/design_dimens.xml").readText()
            assertTrue("Tab 15sp missing in " + qualifier,
                dims.contains("<dimen name=\"design_type_tab\">15sp</dimen>"))
        }
        val tabs = File("src/main/java/com/eve/app/designsystem/DesignSegmentedTabs.kt").readText()
        assertTrue(tabs.contains("FontManager.telegramMediumTypeface(context)"))
        assertTrue(tabs.contains("view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)"))
        val type = File("src/main/java/com/eve/app/designsystem/DesignType.kt").readText()
        assertTrue(type.contains("role == Role.HEADING || role == Role.TAB"))
    }
}
