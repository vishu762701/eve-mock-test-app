package com.eve.app.uistudio

import com.eve.app.R

/** Explicit inventory of production layouts. Preview never starts their Activities. */
object StudioScreens {
    data class Screen(val key: String, val activity: String, val layout: Int) {
        val label get() = key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    val all = listOf(
        Screen("about", "AboutActivity", R.layout.activity_about),
        Screen("content_display", "ContentDisplayActivity", R.layout.activity_content_display),
        Screen("activity_log", "ActivityLogActivity", R.layout.activity_activity_log),
        Screen("admin", "AdminActivity", R.layout.activity_admin),
        Screen("admin_analytics", "AdminAnalyticsActivity", R.layout.activity_admin_analytics),
        Screen("api_usage", "ApiUsageActivity", R.layout.activity_api_usage),
        Screen("app_config", "AppConfigActivity", R.layout.activity_app_config),
        Screen("edit_about", "EditAboutActivity", R.layout.activity_edit_about),
        Screen("edit_exam", "EditExamActivity", R.layout.activity_edit_exam),
        Screen("feedback_messages", "FeedbackMessagesActivity", R.layout.activity_feedback_messages),
        Screen("flagged_questions", "FlaggedQuestionsActivity", R.layout.activity_flagged_questions),
        Screen("generated_tests", "GeneratedTestsActivity", R.layout.activity_generated_tests),
        Screen("manage_exams", "ManageExamsActivity", R.layout.activity_manage_exams),
        Screen("manage_existing_exams", "ManageExistingExamsActivity", R.layout.activity_manage_existing_exams),
        Screen("manage_polls", "ManagePollsActivity", R.layout.activity_manage_polls),
        Screen("manage_premium", "ManagePremiumActivity", R.layout.activity_manage_premium),
        Screen("manage_syllabus", "ManageSyllabusActivity", R.layout.activity_manage_syllabus),
        Screen("manage_users", "ManageUsersActivity", R.layout.activity_manage_users),
        Screen("send_notification", "SendNotificationActivity", R.layout.activity_send_notification),
        Screen("bookmarks", "BookmarksActivity", R.layout.activity_bookmarks),
        Screen("feedback", "FeedbackActivity", R.layout.activity_feedback),
        Screen("history", "HistoryActivity", R.layout.activity_history),
        Screen("exam_tests", "ExamTestsActivity", R.layout.activity_exam_tests),
        Screen("home", "MainActivity", R.layout.activity_main),
        Screen("leaderboard", "LeaderboardActivity", R.layout.activity_leaderboard),
        Screen("login", "LoginActivity", R.layout.activity_login),
        Screen("mistakes", "MistakesActivity", R.layout.activity_mistakes),
        Screen("notifications", "NotificationsActivity", R.layout.activity_notifications),
        Screen("performance", "PerformanceActivity", R.layout.activity_performance),
        Screen("practice", "PracticeActivity", R.layout.activity_practice),
        Screen("payment_checkout", "PaymentCheckoutActivity", R.layout.activity_payment_checkout),
        Screen("premium", "PremiumActivity", R.layout.activity_premium),
        Screen("profile", "ProfileActivity", R.layout.activity_profile),
        Screen("pyq", "PyqActivity", R.layout.activity_pyq),
        Screen("result", "ResultActivity", R.layout.activity_result),
        Screen("result_detail", "ResultDetailActivity", R.layout.activity_result_detail),
        Screen("settings", "SettingsActivity", R.layout.activity_settings),
        Screen("syllabus", "SyllabusActivity", R.layout.activity_syllabus),
        Screen("test", "TestActivity", R.layout.activity_test)
    )
}
