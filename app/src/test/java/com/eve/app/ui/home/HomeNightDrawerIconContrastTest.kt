package com.eve.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Owner-approved source deviation: Night drawer icons must be visible on pure black. */
class HomeNightDrawerIconContrastTest {
    @Test fun drawerNightIconsAreOpaquePrimaryWhiteOnOwnersPureBlackSurface() {
        val xml = File("src/main/res/values-night/telegram_colors.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml)
        val colors = doc.getElementsByTagName("color")
        val map = (0 until colors.length).associate { i ->
            val item = colors.item(i)
            item.attributes.getNamedItem("name").nodeValue to item.textContent.trim()
        }
        assertEquals("#FFFFFFFF", map["telegram_drawer_icon"])
        val drawer = File("src/main/res/values-night/design_colors.xml").readText()
        assertTrue("Owner's opaque Night drawer must be maintained",
            drawer.contains("name=\"home_drawer_background\">#FF000000"))
        val layout = File("src/main/res/layout/activity_main.xml").readText()
        assertEquals("All five static navigation icons share this Night role", 5,
            Regex("app:tint=\"@color/telegram_drawer_icon\"").findAll(layout).count())
        assertTrue("Dynamic premium Lottie is not force-tinted",
            !layout.contains("lottieDrawerPremiumStarFallback\" app:tint="))
    }

    @Test fun thisAccessibilityAccommodationDoesNotRewriteRawTelegramNightRole() {
        val roles = File("src/main/java/com/eve/app/theme/TelegramThemeEngine.kt").readText()
        assertTrue(roles.contains("drawerIcon = role(\"chats_menuItemIcon\")"))
        val day = File("src/main/res/values/telegram_colors.xml").readText()
        assertTrue(day.contains("name=\"telegram_drawer_icon\">#FF889198"))
    }

    @Test fun dynamicThemeSwitchAppliesDrawerContrastOverrideWithoutRestart() {
        val applier = File("src/main/java/com/eve/app/theme/TelegramThemeApplier.kt").readText()
        assertTrue("TelegramThemeApplier recognizes telegram_drawer_icon",
            applier.contains("drawerColor = androidx.core.content.ContextCompat.getColor(view.context, R.color.telegram_drawer_icon)"))
        assertTrue("TelegramThemeApplier binds role 3 to opaque white in Night",
            applier.contains("3 -> if (palette.isDark) 0xFFFFFFFF.toInt() else palette.drawerIcon"))
        assertTrue("TelegramThemeApplier recognizes telegram_type_primary",
            applier.contains("telegramPrimaryColor -> 0"))
    }
}
