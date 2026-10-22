package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Source-traced role guards for the migrated subset only. */
class TelegramTypeSourceRolesTest {
    private fun read(path: String): String = File("src/main/res/" + path).readText()

    @Test fun sizesAndFonts() {
        val s = read("values/telegram_typography.xml")
        val roles = mapOf("NavigationTitle" to 20, "SettingsLabel" to 16,
            "SettingsDetail" to 13, "SettingsSection" to 14, "MenuItem" to 16,
            "DialogTitle" to 20, "DialogMessage" to 16, "SearchInput" to 18)
        for ((role, size) in roles) {
            val start = s.indexOf("<style name=\"TextAppearance.Eve.Telegram." + role + "\"")
            assertTrue("No role: " + role, start >= 0)
            val end = s.indexOf("</style>", start)
            assertTrue("Unclosed role: " + role, end > start)
            val body = s.substring(start, end)
            assertTrue("Wrong SP size: " + role, body.contains(">" + size + "sp</item>"))
            assertTrue("Invalid font family: " + role, body.contains("sans-serif"))
            assertTrue("Inter leaked: " + role, !body.contains("@font/inter"))
        }
    }
    @Test fun pairedColorsAndProtectedUsages() {
        for (role in listOf("primary", "secondary", "dialog")) {
            for (qualifier in listOf("values", "values-night")) {
                assertTrue("Missing " + role + " in " + qualifier,
                    read(qualifier + "/telegram_colors.xml").contains("name=\"telegram_type_" + role + "\""))
            }
        }
        for (file in listOf("activity_settings.xml", "activity_notifications.xml",
            "activity_result.xml", "popup_telegram_menu.xml")) {
            val text = read("layout/" + file)
            val roles = "TextAppearance.Eve.Telegram.".toRegex().findAll(text).count()
            val marks = "android:tag=\"eve_telegram_type\"".toRegex().findAll(text).count()
            assertTrue("Wrong tag count for " + file, roles > 0 && roles == marks)
        }
    }
}
