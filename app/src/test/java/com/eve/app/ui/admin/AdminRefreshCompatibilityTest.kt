package com.eve.app.ui.admin

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Contract test ensuring misplaced refresh buttons are eliminated from top toolbars
 * while maintaining backward-compatible test hooks and Telegram-canonical SwipeRefreshLayout.
 */
class AdminRefreshCompatibilityTest {

    private fun layout(name: String): String {
        val f = File("src/main/res/layout/$name.xml")
        val alt = File("app/src/main/res/layout/$name.xml")
        return (if (f.exists()) f else alt).readText()
    }

    private fun source(name: String): String {
        val f = File("src/main/java/com/eve/app/ui/admin/$name.kt")
        val alt = File("app/src/main/java/com/eve/app/ui/admin/$name.kt")
        return (if (f.exists()) f else alt).readText()
    }

    @Test
    fun misplacedRefreshButtonsAreHiddenFromTopToolbars() {
        val layoutsWithCompatButton = listOf(
            "activity_admin_analytics",
            "activity_api_usage",
            "activity_feedback_messages",
            "activity_flagged_questions",
            "activity_generated_tests",
            "activity_manage_existing_exams",
            "activity_manage_premium",
            "activity_manage_users"
        )
        for (name in layoutsWithCompatButton) {
            val content = layout(name)
            assertTrue("btnRefresh must exist for test and contract compatibility in $name",
                content.contains("android:id=\"@+id/btnRefresh\""))
            assertTrue("btnRefresh must be hidden (visibility gone) to eliminate misplaced toolbar buttons in $name",
                content.contains("android:id=\"@+id/btnRefresh\"") &&
                content.contains("android:visibility=\"gone\""))
        }
    }

    @Test
    fun canonicalSwipeRefreshLayoutPresentInAdminListScreens() {
        val layoutsWithSwipeRefresh = listOf(
            "activity_admin_analytics",
            "activity_api_usage",
            "activity_feedback_messages",
            "activity_flagged_questions",
            "activity_generated_tests",
            "activity_manage_existing_exams",
            "activity_manage_users"
        )
        for (name in layoutsWithSwipeRefresh) {
            val content = layout(name)
            assertTrue("SwipeRefreshLayout must be present in $name",
                content.contains("androidx.swiperefreshlayout.widget.SwipeRefreshLayout"))
            assertTrue("SwipeRefreshLayout must have id @+id/swipeRefresh in $name",
                content.contains("android:id=\"@+id/swipeRefresh\""))
        }
    }

    @Test
    fun activitiesWireUpSwipeRefreshListeners() {
        val activities = listOf(
            "AdminAnalyticsActivity",
            "ApiUsageActivity",
            "FeedbackMessagesActivity",
            "FlaggedQuestionsActivity",
            "GeneratedTestsActivity",
            "ManageExistingExamsActivity",
            "ManageUsersActivity"
        )
        for (act in activities) {
            val code = source(act)
            assertTrue("$act must wire up swipeRefresh.setOnRefreshListener",
                code.contains("binding.swipeRefresh.setOnRefreshListener"))
        }
    }
}
