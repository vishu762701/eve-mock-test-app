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
    @Test fun everyActivityNavigationActionUsesTelegramSourceIconColors() {
        val files = File("src/main/res/layout").listFiles()!!.filter {
            it.isFile && it.name.startsWith("activity_") && it.extension == "xml"
        }
        var affected = 0
        for (file in files) {
            val lines = file.readLines().filter { line ->
                line.contains("android:src=\"@drawable/ic_back\"") ||
                    line.contains("android:src=\"@drawable/ic_more_vert\"") ||
                    line.contains("app:navigationIcon=\"@drawable/ic_back\"")
            }
            for (line in lines) {
                val tint = if (line.contains("app:navigationIcon=\"")) {
                    "app:navigationIconTint=\"@color/telegram_type_primary\""
                } else {
                    "app:tint=\"@color/telegram_type_primary\""
                }
                assertTrue("Incorrect ActionBar tint in " + file.name, line.contains(tint))
                if (line.contains("android:src=\"")) {
                    assertTrue("Navigation icon lacks content description in " + file.name,
                        line.contains("android:contentDescription="))
                }
                affected++
            }
        }
        assertTrue("Incomplete source role coverage of ActionBar navigation", affected >= 40)
    }

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
