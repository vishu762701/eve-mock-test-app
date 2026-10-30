package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Source: uploaded Telegram ActionBarPopupWindow.java:233-234,1110-1126;
 * ThemeColors.java:206 and assets/night.attheme:108. The host is ADAPTED. */
class HomePopupSectionGapTest {
    private fun xml(path: String) = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder().parse(File(path))

    @Test fun themeRowIsSeparatedFromNavigationByTheOriginalSixDpGap() {
        val elements = xml("src/main/res/layout/popup_telegram_menu.xml").getElementsByTagName("*")
        val ids = (0 until elements.length).map { (elements.item(it) as Element)
            .getAttribute("android:id") }
        val theme = ids.indexOf("@+id/cardTheme")
        val gap = ids.indexOf("@+id/menuSectionGap")
        val history = ids.indexOf("@+id/cardMenuItems")
        assertTrue("Gap must appear between theme and navigation groups", theme >= 0 && theme < gap && gap < history)
        val gapElement = (0 until elements.length).map { elements.item(it) as Element }
            .first { it.getAttribute("android:id") == "@+id/menuSectionGap" }
        assertEquals("6dp", gapElement.getAttribute("android:layout_height"))
        assertEquals("@color/telegram_menu_section_gap", gapElement.getAttribute("android:background"))
        assertEquals("no", gapElement.getAttribute("android:importantForAccessibility"))
    }

    @Test fun dayAndNightSeparatorColorsMatchUploadedUpstreamThemeKeys() {
        val variants = mapOf(
            "src/main/res/values/telegram_colors.xml" to "#FFF5F5F5",
            "src/main/res/values-night/telegram_colors.xml" to "#FF1E1E1F"
        )
        variants.forEach { (path, expected) ->
            val nodes = xml(path).getElementsByTagName("color")
            val actual = (0 until nodes.length).map { nodes.item(it) as Element }
                .first { it.getAttribute("name") == "telegram_menu_section_gap" }
                .textContent.trim()
            assertEquals(expected, actual)
        }
    }
}
