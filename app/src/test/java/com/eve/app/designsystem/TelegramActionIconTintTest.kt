package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Direct matching of Telegram ActionBar default icon color roles.
 * This test does NOT verify the GPL icon bitmap/shape.
 */
class TelegramActionIconTintTest {
    @Test fun sourceMappedToolbarActionsHavePairedThemeTintsAndPreservedTargets() {
        val day = File("src/main/res/values/telegram_colors.xml").readText()
        val night = File("src/main/res/values-night/telegram_colors.xml").readText()
        assertTrue(day.contains("<color name=\"telegram_type_primary\">#FF1A1D21</color>"))
        assertTrue(night.contains("<color name=\"telegram_type_primary\">#FFFFFFFF</color>"))
        val screens = listOf("activity_main", "activity_settings", "activity_notifications",
            "activity_result", "activity_profile", "activity_result_detail",
            "activity_about", "activity_admin")
        var actions = 0
        for (screen in screens) {
            val layout = File("src/main/res/layout/" + screen + ".xml").readText()
            val iconLines = layout.lineSequence().filter {
                it.contains("android:src=\"@drawable/ic_back\"") ||
                    it.contains("android:src=\"@drawable/ic_more_vert\"")
            }.toList()
            assertTrue("No corresponding ActionBar action in " + screen, iconLines.isNotEmpty())
            for (line in iconLines) {
                assertTrue("Tint is not Telegram Day/Night action ink in " + screen,
                    line.contains("app:tint=\"@color/telegram_type_primary\""))
                assertTrue("Touch size missing " + screen,
                    line.contains("android:layout_width=\"@dimen/design_touch_min\""))
                assertTrue("Content description missing " + screen,
                    line.contains("android:contentDescription="))
                actions++
            }
        }
        assertEquals(9, actions)
    }
}
