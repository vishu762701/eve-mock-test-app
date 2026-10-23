package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TelegramDrawerIconSourceTest {
    @Test fun exactTelegramDrawerIconTintDiffersFromActionBarAndIsQualified() {
        val day = File("src/main/res/values/telegram_colors.xml").readText()
        val night = File("src/main/res/values-night/telegram_colors.xml").readText()
        assertTrue(day.contains("<color name=\"telegram_drawer_icon\">#FF889198</color>"))
        assertTrue(night.contains("<color name=\"telegram_drawer_icon\">#FF828282</color>"))
        val main = File("src/main/res/layout/activity_main.xml").readText()
        val names = listOf("ic_person", "ic_history", "ic_bookmark", "ic_performance_increase",
            "ic_leaderboard_star", "ic_syllabus", "ic_info", "ic_settings")
        for (name in names) {
            val target = main.lineSequence().first { it.contains("android:src=\"@drawable/" + name + "\"") }
            assertTrue("Incorrect DrawerActionCell ink " + name,
                target.contains("app:tint=\"@color/telegram_drawer_icon\""))
        }
        assertEquals(8, names.size)
        val action = main.lineSequence().first { it.contains("android:id=\"@+id/btnOverflow\"") }
        assertTrue(action.contains("app:tint=\"@color/telegram_type_primary\""))
        assertTrue(main.contains("app:tint=\"@color/system_blue\""))
    }
}
