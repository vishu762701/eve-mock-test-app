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

    @Test fun byteExactOriginalNinePatchShadowAndSourceHost() {
        val sha = mapOf(
            "mdpi" to "08fcaed4891c9dec8cd9774df438d5e367419886bb0a9b867f0c7eb8ff89df8b",
            "hdpi" to "84f8905c4e97c8eefb64d6c5d88b9d8790e9ea4e254202cd06d8ac5eb6cfbb15",
            "xhdpi" to "daf5731b3290361fa2bfa4fc7fe934dace0d8018cc270bf9e32b36d153978d58",
            "xxhdpi" to "bcb4e022907ef3a3a23235a26a1ea230db37ce438966961282868de88a3300e2"
        )
        sha.forEach { (density, expected) ->
            val file = File("src/main/res/drawable-$density/telegram_menu_greydivider.9.png")
            assertTrue("Missing original Telegram $density divider", file.isFile)
            val hash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(file.readBytes()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
            assertEquals("Upstream Telegram greydivider must remain byte exact", expected, hash)
        }
        val source = File("src/main/java/com/eve/app/ui/home/TelegramPopupGapView.kt").readText()
        assertTrue(source.contains("R.drawable.telegram_menu_greydivider"))
        assertTrue(source.contains("PorterDuff.Mode.MULTIPLY"))
        val layout = File("src/main/res/layout/popup_telegram_menu.xml").readText()
        assertTrue(layout.contains("<com.eve.app.ui.home.TelegramPopupGapView android:id=\"@+id/menuSectionGap\""))
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
