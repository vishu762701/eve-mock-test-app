package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TelegramMenuIconSemanticsTest {
    @Test fun homeToolbarUsesCoherentDayNightSourceInkWithoutHidingNotifications() {
        val xml = File("src/main/res/layout/activity_main.xml").readText()
        for (id in listOf("btnSearch", "ivNotificationIdle", "btnOverflow", "btnClearSearch")) {
            val line = xml.lineSequence().first { it.contains("android:id=\"@+id/" + id + "\"") }
            assertTrue("Wrong ActionBar tint on " + id, line.contains("app:tint=\"@color/telegram_type_primary\""))
        }
        assertTrue(xml.contains("android:id=\"@+id/notificationAction\""))
        assertTrue(xml.contains("android:src=\"@drawable/ic_notification\""))
        assertTrue(xml.contains("app:lottie_rawRes=\"@raw/notification_bell\""))
        val main = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        assertTrue(main.contains("binding.ivNotificationIdle.visibility = View.VISIBLE"))
        assertTrue(main.contains("binding.btnNotification.alpha = 0f"))
    }

    @Test fun menuIconsPreserveSemanticsAndMapTelegramInk() {
        val menu = File("src/main/res/layout/popup_telegram_menu.xml").readText()
        for (line in menu.lineSequence().filter { it.contains("<ImageView") && it.contains("android:src=\"@drawable/ic_") }) {
            assertTrue("Mixed menu icon tint: " + line, line.contains("app:tint=\"@color/telegram_type_primary\""))
        }
        val mistakeLine = menu.lineSequence().first { it.contains("android:contentDescription=\"Mistake Notebook\"") }
        assertTrue("Mistake action must not be represented by Close", mistakeLine.contains("android:src=\"@drawable/ic_flag\""))
        assertTrue(menu.contains("android:id=\"@+id/menuRowMistakes\""))
    }
}
