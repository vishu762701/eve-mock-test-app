package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Telegram ActionBar.java:1443-1462 navigation titles: 20dp -> EVE 20sp. */
class TelegramNavigationDestinationsTest {
    private val destinations = mapOf(
        "activity_profile.xml" to "Profile",
        "activity_history.xml" to "Test History",
        "activity_leaderboard.xml" to "Leaderboard",
        "activity_syllabus.xml" to "Exam Syllabus",
        "activity_premium.xml" to "Premium",
        "activity_feedback.xml" to "Feedback",
        "activity_performance.xml" to "Performance Insights",
        "activity_admin.xml" to "Admin Dashboard",
        "activity_manage_users.xml" to "Manage Users"
    )

    @Test fun mainDestinationHeadingsUseSourceMatchedTypography() {
        val dir = listOf(File("src/main/res/layout"), File("app/src/main/res/layout")).first { it.isDirectory }
        val xml = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val ns = "http://schemas.android.com/apk/res/android"
        for ((filename, text) in destinations) {
            val root = xml.newDocumentBuilder().parse(File(dir, filename)).documentElement
            val nodes = root.getElementsByTagName("TextView")
            val matches = (0 until nodes.length).map { nodes.item(it) as Element }
                .filter { it.getAttributeNS(ns, "text") == text
                    && it.getAttribute("style") == "@style/TextAppearance.Eve.Telegram.NavigationTitle" }
            assertEquals(filename + " title role", 1, matches.size)
            assertEquals(filename + " protects source typeface", "eve_telegram_type",
                matches.single().getAttributeNS(ns, "tag"))
        }
    }
}
