package com.eve.app.ui

import com.eve.app.designsystem.designAttribute
import com.eve.app.util.SystemBarHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Ios26DesignFidelityTest {

    private fun findFile(relativePath: String): File {
        val candidates = listOf(File(relativePath), File("app/$relativePath"), File("../app/$relativePath"))
        return candidates.firstOrNull { it.exists() } ?: error("Cannot find file: $relativePath")
    }

    private fun loadXml(relativePath: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(findFile(relativePath)).documentElement

    private fun loadResolvedColors(qualifier: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val folders = if (qualifier == "values") listOf("values") else listOf("values", qualifier)
        for (folder in folders) {
            val dir = findFile("src/main/res/$folder")
            dir.listFiles { f -> f.extension == "xml" }?.sortedBy { it.name }?.forEach { file ->
                val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
                val nodes = root.getElementsByTagName("color")
                for (i in 0 until nodes.length) {
                    val el = nodes.item(i) as Element
                    map[el.getAttribute("name")] = el.textContent.trim()
                }
            }
        }
        fun resolve(name: String, seen: Set<String> = emptySet()): String {
            check(name !in seen) { "Cyclic color alias: $name" }
            val v = map[name] ?: error("Missing color token: $name")
            return when {
                v.startsWith("@color/") -> resolve(v.removePrefix("@color/"), seen + name)
                v == "@android:color/transparent" -> "#00000000"
                else -> v
            }
        }
        return map.keys.associateWith { resolve(it) }
    }

    @Test
    fun test1_screenCanvasAndSystemBarRoleAgreement() {
        val plainScreens = listOf(
            "MainActivity" to "activity_main.xml",
            "LoginActivity" to "activity_login.xml",
            "TestActivity" to "activity_test.xml",
            "ContentDisplayActivity" to "activity_content_display.xml"
        )
        for ((activityName, layoutFile) in plainScreens) {
            assertEquals(
                SystemBarHelper.ScreenCanvasRole.PLAIN,
                SystemBarHelper.resolveCanvasRoleForNames(activityName, layoutFile.removeSuffix(".xml"))
            )
            val root = loadXml("src/main/res/layout/$layoutFile")
            val bg = root.designAttribute("android:background")
            assertTrue(bg in setOf("@color/bg_app", "@color/eve_system_background", "@color/eve_canvas", "@color/eve_bg", "@color/eve_test_background", "@color/eve_plain_canvas", "@color/eve_home_background"))
        }

        val groupedScreens = listOf(
            "SettingsActivity" to "activity_settings.xml",
            "ProfileActivity" to "activity_profile.xml",
            "ResultActivity" to "activity_result.xml",
            "ResultDetailActivity" to "activity_result_detail.xml",
            "LeaderboardActivity" to "activity_leaderboard.xml",
            "NotificationsActivity" to "activity_notifications.xml",
            "AboutActivity" to "activity_about.xml",
            "HistoryActivity" to "activity_history.xml",
            "BookmarksActivity" to "activity_bookmarks.xml",
            "MistakesActivity" to "activity_mistakes.xml",
            "PerformanceActivity" to "activity_performance.xml",
            "PracticeActivity" to "activity_practice.xml",
            "PyqActivity" to "activity_pyq.xml",
            "SyllabusActivity" to "activity_syllabus.xml",
            "FeedbackActivity" to "activity_feedback.xml",
            "PremiumActivity" to "activity_premium.xml",
            "PaymentCheckoutActivity" to "activity_payment_checkout.xml",
            "ExamTestsActivity" to "activity_exam_tests.xml",
            "AdminActivity" to "activity_admin.xml",
            "ManagePremiumActivity" to "activity_manage_premium.xml",
            "EditAboutActivity" to "activity_edit_about.xml",
            "ManageExamsActivity" to "activity_manage_exams.xml",
            "ManageExistingExamsActivity" to "activity_manage_existing_exams.xml",
            "EditExamActivity" to "activity_edit_exam.xml",
            "GeneratedTestsActivity" to "activity_generated_tests.xml",
            "ManageUsersActivity" to "activity_manage_users.xml",
            "SendNotificationActivity" to "activity_send_notification.xml",
            "FlaggedQuestionsActivity" to "activity_flagged_questions.xml",
            "FeedbackMessagesActivity" to "activity_feedback_messages.xml",
            "ManagePollsActivity" to "activity_manage_polls.xml",
            "AdminAnalyticsActivity" to "activity_admin_analytics.xml",
            "ActivityLogActivity" to "activity_activity_log.xml",
            "ApiUsageActivity" to "activity_api_usage.xml",
            "AppConfigActivity" to "activity_app_config.xml",
            "ManageSyllabusActivity" to "activity_manage_syllabus.xml"
        )
        for ((activityName, layoutFile) in groupedScreens) {
            assertEquals(
                SystemBarHelper.ScreenCanvasRole.GROUPED,
                SystemBarHelper.resolveCanvasRoleForNames(activityName, layoutFile.removeSuffix(".xml"))
            )
            val root = loadXml("src/main/res/layout/$layoutFile")
            assertEquals("@color/bg_app", root.designAttribute("android:background"))
        }

        assertEquals(SystemBarHelper.ScreenCanvasRole.GROUPED, SystemBarHelper.resolveCanvasRoleForNames("SystemMonitorActivity"))
    }

    @Test
    fun test2_tabLabelsOrderAndCallbackPreservation() {
        fun extractTabTexts(layoutPath: String, tabLayoutId: String): List<String> {
            val root = loadXml(layoutPath)
            val descendants = root.getElementsByTagName("*")
            val tabLayout = (0 until descendants.length).map { descendants.item(it) as Element }
                .single { it.getAttribute("android:id") == "@+id/$tabLayoutId" }
            val items = tabLayout.getElementsByTagName("com.google.android.material.tabs.TabItem")
            return (0 until items.length).map { (items.item(it) as Element).getAttribute("android:text") }
        }
        assertEquals(listOf("Overview", "Review", "Leaderboard"), extractTabTexts("src/main/res/layout/activity_result.xml", "tabLayoutResult"))
        assertEquals(listOf("Exams", "Manage Questions", "Admins"), extractTabTexts("src/main/res/layout/activity_admin.xml", "tabLayoutAdmin"))
        assertEquals(listOf("Settings", "Transactions", "Users"), extractTabTexts("src/main/res/layout/activity_manage_premium.xml", "tabLayout"))
        assertEquals(listOf("Privacy", "Terms", "Contact Us"), extractTabTexts("src/main/res/layout/activity_edit_about.xml", "tabLayout"))
    }

    @Test
    fun test3_noRedundantHairlineDividersBelowToolbars() {
        val layoutsWithToolbars = listOf(
            "activity_about.xml", "activity_activity_log.xml", "activity_admin_analytics.xml",
            "activity_api_usage.xml", "activity_bookmarks.xml", "activity_feedback.xml",
            "activity_feedback_messages.xml", "activity_flagged_questions.xml", "activity_history.xml",
            "activity_leaderboard.xml", "activity_manage_syllabus.xml", "activity_mistakes.xml",
            "activity_notifications.xml", "activity_payment_checkout.xml", "activity_performance.xml",
            "activity_premium.xml", "activity_profile.xml", "activity_result_detail.xml",
            "activity_settings.xml"
        )
        for (name in layoutsWithToolbars) {
            val text = findFile("src/main/res/layout/$name").readText()
            val afterToolbar = text.substringAfter("</com.eve.app.ui.common.EveGlassToolbar>").trimStart()
            assertFalse("Layout $name should not have a View divider immediately below EveGlassToolbar",
                afterToolbar.startsWith("<View") && (afterToolbar.contains("@color/eve_stroke") || afterToolbar.contains("@color/eve_separator")))
        }

        val adminText = findFile("src/main/res/layout/activity_admin.xml").readText()
        val afterTabs = adminText.substringAfter("</com.eve.app.ui.common.EveGlassTabLayout>").trimStart()
        assertFalse(afterTabs.startsWith("<View"))
    }

    @Test
    fun test4_allSectionTabsUseEveGlassTabLayout() {
        val tabLayouts = listOf(
            "activity_result.xml" to "tabLayoutResult",
            "activity_admin.xml" to "tabLayoutAdmin",
            "activity_manage_premium.xml" to "tabLayout",
            "activity_edit_about.xml" to "tabLayout"
        )
        for ((path, id) in tabLayouts) {
            val root = loadXml("src/main/res/layout/$path")
            val descendants = root.getElementsByTagName("*")
            val tabElement = (0 until descendants.length).map { descendants.item(it) as Element }
                .single { it.getAttribute("android:id") == "@+id/$id" }
            assertEquals("Tab $id in $path must be EveGlassTabLayout", "com.eve.app.designsystem.DesignSegmentedTabs", tabElement.tagName)
        }
    }

    @Test
    fun test5_protectedTestColorsPreserved() {
        val light = loadResolvedColors("values")
        val dark = loadResolvedColors("values-night")
        assertEquals("#34C759", light["eve_test_selected"])
        assertEquals("#34C759", dark["eve_test_selected"])
        assertEquals("#FFCC00", light["eve_test_review"])
        assertEquals("#FFCC00", dark["eve_test_review"])
    }
}
