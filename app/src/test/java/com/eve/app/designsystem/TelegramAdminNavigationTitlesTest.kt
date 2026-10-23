package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Telegram ActionBar.java title style applied to admin destinations, not KPI numbers. */
class TelegramAdminNavigationTitlesTest {
    @Test fun adminDestinationsUseTelegramNavigationType() {
        val dir = listOf(File("src/main/res/layout"), File("app/src/main/res/layout")).first { it.isDirectory }
        for ((filename, title) in mapOf(
            "activity_manage_existing_exams.xml" to "Manage Existing Exams",
            "activity_manage_syllabus.xml" to "Manage Syllabus",
            "activity_manage_polls.xml" to "Polls Management",
            "activity_manage_exams.xml" to "Manage Exam",
            "activity_manage_premium.xml" to "Premium Management",
            "activity_send_notification.xml" to "Publish In-App Notice",
            "activity_edit_exam.xml" to "Edit Exam",
            "activity_generated_tests.xml" to "AI Generated Tests"
        )) {
            val xml = File(dir, filename).readText()
            val textView = xml.lineSequence().first { it.contains("android:text=\"" + title + "\"") }
            assertTrue(filename + " source-aligned title", textView.contains("style=\"@style/TextAppearance.Eve.Telegram.NavigationTitle\""))
            assertTrue(filename + " protected against old config", textView.contains("android:tag=\"eve_telegram_type\""))
        }
    }
}
