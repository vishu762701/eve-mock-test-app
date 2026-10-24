package com.eve.app.typography

import android.app.Activity

/**
 * Searchable, discoverable catalog of actual EVE screens and text elements.
 * Provides stable element keys, human-readable titles, default semantic roles,
 * and Activity class mappings.
 */
object TypographyRegistry {

    data class ElementDescriptor(
        val key: String,
        val displayName: String,
        val defaultRole: String,
        val description: String = ""
    )

    data class ScreenDescriptor(
        val key: String,
        val displayName: String,
        val activityClassNames: List<String> = emptyList(),
        val elements: List<ElementDescriptor>
    )

    val SCREENS = listOf(
        ScreenDescriptor(
            key = "home",
            displayName = "Home",
            activityClassNames = listOf("MainActivity"),
            elements = listOf(
                ElementDescriptor("home_welcome_title", "Welcome Title", "title_1", "Main greeting on home canvas"),
                ElementDescriptor("home_streak_label", "Streak Counter", "chip", "Pill counter with flame icon"),
                ElementDescriptor("home_category_title", "Category Header", "headline", "Section title for exam categories"),
                ElementDescriptor("home_exam_card_title", "Exam Card Title", "headline", "Title on exam selection cards"),
                ElementDescriptor("home_exam_card_desc", "Exam Card Description", "subheadline", "Supporting text under exam titles"),
                ElementDescriptor("home_banner_title", "Banner Heading", "title_3", "Rotating content banner hero text"),
                ElementDescriptor("home_search_hint", "Search Bar Text", "body", "Search input placeholder and query")
            )
        ),
        ScreenDescriptor(
            key = "mock_test",
            displayName = "Mock Test",
            activityClassNames = listOf("TestActivity"),
            elements = listOf(
                ElementDescriptor("test_exam_title", "Exam Title", "headline", "Top bar exam name"),
                ElementDescriptor("test_timer_text", "Timer Countdown", "caption_1", "Circular timer digital time display"),
                ElementDescriptor("test_question_number", "Question Number", "caption_1", "Question indexing badge"),
                ElementDescriptor("test_question_body", "Question Text", "body", "Scrollable exam question content"),
                ElementDescriptor("test_option_text", "Answer Option Text", "body", "Radio option choice text"),
                ElementDescriptor("test_btn_prev", "Previous Button", "button", "Navigational action"),
                ElementDescriptor("test_btn_next", "Next Button", "button", "Navigational action"),
                ElementDescriptor("test_btn_clear", "Clear Selection", "button", "Option reset action"),
                ElementDescriptor("test_btn_review", "Mark for Review", "button", "Review toggle action"),
                ElementDescriptor("test_palette_number", "Palette Number", "caption_2", "Question palette circle label")
            )
        ),
        ScreenDescriptor(
            key = "result",
            displayName = "Result & Summary",
            activityClassNames = listOf("ResultActivity"),
            elements = listOf(
                ElementDescriptor("result_screen_title", "Screen Title", "title_2", "Main header title"),
                ElementDescriptor("result_total_score", "Total Score", "large_title", "Prominent hero score display"),
                ElementDescriptor("result_score_pct", "Score Percentage", "headline", "Secondary hero accuracy badge"),
                ElementDescriptor("result_stat_heading", "Statistics Heading", "headline", "Performance breakdown title"),
                ElementDescriptor("result_stat_numbers", "Metric Values", "title_2", "Correct/Wrong/Skipped counts"),
                ElementDescriptor("result_stat_labels", "Metric Labels", "footnote", "Label under metric counts"),
                ElementDescriptor("result_tab_label", "Tab Labels", "tab", "Review, Overview, Leaderboard tabs"),
                ElementDescriptor("result_explanation", "Explanation Text", "body", "Detailed question explanations"),
                ElementDescriptor("result_btn_reattempt", "Reattempt Button", "button", "Primary action button")
            )
        ),
        ScreenDescriptor(
            key = "result_detail",
            displayName = "Result Details",
            activityClassNames = listOf("ResultDetailActivity"),
            elements = listOf(
                ElementDescriptor("tvDetailTitle", "Detail Screen Title", "title_2", "Header title for analytics/breakdown"),
                ElementDescriptor("tvStatHeroScore", "Net Score Hero", "large_title", "Prominent net marks scored"),
                ElementDescriptor("tvStatHeroPercentage", "Accuracy Badge", "headline", "Percentage badge"),
                ElementDescriptor("tvStatCorrectMarks", "Correct Marks", "body", "Positive marks value"),
                ElementDescriptor("tvStatWrongMarks", "Negative Marks", "body", "Deductions value"),
                ElementDescriptor("tvStatFinalNetScore", "Final Total Marks", "headline", "Calculated score sum"),
                ElementDescriptor("tvPerfDetailRank", "Candidate Rank", "title_2", "Computed rank among test takers"),
                ElementDescriptor("tvPerfDetailPercentile", "Percentile Score", "body", "Comparative percentile badge")
            )
        ),
        ScreenDescriptor(
            key = "leaderboard",
            displayName = "Leaderboard",
            activityClassNames = listOf("LeaderboardActivity"),
            elements = listOf(
                ElementDescriptor("tvExamName", "Exam Title", "title_2", "Exam title in header"),
                ElementDescriptor("tvYourRank", "Your Rank", "headline", "User rank position badge"),
                ElementDescriptor("tvYourScore", "Your Score", "headline", "Score achieved in test"),
                ElementDescriptor("tvYourPercentile", "Your Percentile", "subheadline", "Candidate percentile"),
                ElementDescriptor("tvMessage", "Empty/Status Message", "headline", "Notice message when list empty")
            )
        ),
        ScreenDescriptor(
            key = "performance",
            displayName = "Performance Analytics",
            activityClassNames = listOf("PerformanceActivity"),
            elements = listOf(
                ElementDescriptor("tvOverallScore", "Overall Average", "title_1", "Aggregated performance score"),
                ElementDescriptor("tvOverallAccuracy", "Overall Accuracy", "headline", "Percentage accuracy across tests"),
                ElementDescriptor("tvHeadingScoreTrend", "Score Trend Heading", "headline", "Section title for score graph"),
                ElementDescriptor("tvWeakAreas", "Weak Areas Summary", "subheadline", "Topics needing improvement"),
                ElementDescriptor("tvHeadingTopicAccuracy", "Topic Accuracy Heading", "headline", "Topic performance breakdown")
            )
        ),
        ScreenDescriptor(
            key = "history",
            displayName = "Attempt History",
            activityClassNames = listOf("HistoryActivity"),
            elements = listOf(
                ElementDescriptor("history_title", "History Title", "title_2", "Screen header for past tests"),
                ElementDescriptor("history_exam_name", "Exam Name", "headline", "Name of completed mock test"),
                ElementDescriptor("history_score", "Score Value", "headline", "Recorded marks on card"),
                ElementDescriptor("history_date", "Attempt Date", "footnote", "Timestamp of test completion")
            )
        ),
        ScreenDescriptor(
            key = "bookmarks",
            displayName = "Bookmarks",
            activityClassNames = listOf("BookmarksActivity"),
            elements = listOf(
                ElementDescriptor("tvBookmarkCount", "Bookmarks Counter", "subheadline", "Count of saved questions"),
                ElementDescriptor("tvQuestionText", "Question Preview", "body", "Bookmarked question stem snippet"),
                ElementDescriptor("tvOfflineBanner", "Offline Notice", "footnote", "Notice when offline cache is in use")
            )
        ),
        ScreenDescriptor(
            key = "practice",
            displayName = "Practice Mode",
            activityClassNames = listOf("PracticeActivity"),
            elements = listOf(
                ElementDescriptor("practice_title", "Practice Title", "title_2", "Header navigation title"),
                ElementDescriptor("practice_topic_name", "Topic Name", "headline", "Selected study topic title"),
                ElementDescriptor("practice_btn_start", "Start Button", "button", "Launch practice session action")
            )
        ),
        ScreenDescriptor(
            key = "pyq",
            displayName = "Previous Year Papers",
            activityClassNames = listOf("PyqActivity"),
            elements = listOf(
                ElementDescriptor("tvPapersLabel", "Papers Header", "headline", "Section heading for papers"),
                ElementDescriptor("chipYear", "Year Filter Chip", "chip", "Selectable year filter chips"),
                ElementDescriptor("tvMessage", "Status Message", "body", "Paper availability message")
            )
        ),
        ScreenDescriptor(
            key = "exam_tests",
            displayName = "Exam Tests List",
            activityClassNames = listOf("ExamTestsActivity"),
            elements = listOf(
                ElementDescriptor("exam_tests_title", "Tests Screen Title", "title_2", "Title of exam test list"),
                ElementDescriptor("exam_tests_item_title", "Test Card Title", "headline", "Mock test title card"),
                ElementDescriptor("exam_tests_item_meta", "Test Metadata", "subheadline", "Questions and duration label")
            )
        ),
        ScreenDescriptor(
            key = "mistakes",
            displayName = "Mistakes Notebook",
            activityClassNames = listOf("MistakesActivity"),
            elements = listOf(
                ElementDescriptor("mistakes_title", "Mistakes Title", "title_2", "Mistakes screen title"),
                ElementDescriptor("mistakes_count", "Mistakes Count", "headline", "Total incorrect answers counter"),
                ElementDescriptor("mistakes_filter_tab", "Filter Tabs", "tab", "All, Wrong, Skipped tabs")
            )
        ),
        ScreenDescriptor(
            key = "notifications",
            displayName = "Notifications",
            activityClassNames = listOf("NotificationsActivity"),
            elements = listOf(
                ElementDescriptor("notif_title", "Screen Title", "title_2", "Header notification title"),
                ElementDescriptor("notif_item_title", "Notification Headline", "headline", "Title of broadcast alert"),
                ElementDescriptor("notif_item_body", "Notification Body", "body", "Full text content of alert"),
                ElementDescriptor("notif_item_time", "Timestamp", "caption_1", "Time elapsed since send")
            )
        ),
        ScreenDescriptor(
            key = "premium",
            displayName = "Premium Subscription",
            activityClassNames = listOf("PremiumActivity", "PaymentCheckoutActivity"),
            elements = listOf(
                ElementDescriptor("tvTagline", "Premium Tagline", "subheadline", "Plan benefit highlight"),
                ElementDescriptor("tvPlanPrice", "Plan Price", "title_1", "Price in currency"),
                ElementDescriptor("tvPlanValidity", "Validity Duration", "caption_1", "Subscription active period"),
                ElementDescriptor("btnBuyPremium", "Purchase Button", "button", "Buy Premium CTA"),
                ElementDescriptor("tvActiveTitle", "Active Plan Title", "headline", "Current plan active status")
            )
        ),
        ScreenDescriptor(
            key = "settings",
            displayName = "Settings",
            activityClassNames = listOf("SettingsActivity"),
            elements = listOf(
                ElementDescriptor("settings_screen_title", "Screen Title", "title_3", "Header navigation title"),
                ElementDescriptor("settings_section_header", "Section Header", "caption_1", "Uppercase category overline"),
                ElementDescriptor("settings_item_title", "Setting Row Title", "body", "Primary setting title"),
                ElementDescriptor("settings_item_value", "Setting Row Value", "subheadline", "Secondary setting status")
            )
        ),
        ScreenDescriptor(
            key = "profile",
            displayName = "User Profile",
            activityClassNames = listOf("ProfileActivity"),
            elements = listOf(
                ElementDescriptor("profile_user_name", "User Name Display", "title_2", "Profile student name"),
                ElementDescriptor("profile_user_email", "User Email Display", "subheadline", "Profile student email"),
                ElementDescriptor("profile_section_title", "Section Title", "headline", "Form section headings"),
                ElementDescriptor("profile_btn_save", "Save Profile Button", "button", "Save action button")
            )
        ),
        ScreenDescriptor(
            key = "feedback",
            displayName = "Student Feedback",
            activityClassNames = listOf("FeedbackActivity"),
            elements = listOf(
                ElementDescriptor("feedback_title", "Feedback Title", "title_2", "Community feedback title"),
                ElementDescriptor("feedback_item_subject", "Thread Subject", "headline", "User feedback topic subject"),
                ElementDescriptor("feedback_item_body", "Thread Body", "body", "Detailed feedback question/comment"),
                ElementDescriptor("feedback_btn_new", "New Feedback Button", "button", "Post new feedback action")
            )
        ),
        ScreenDescriptor(
            key = "about",
            displayName = "About App",
            activityClassNames = listOf("AboutActivity", "ContentDisplayActivity"),
            elements = listOf(
                ElementDescriptor("tvAppName", "App Name", "headline", "Branded name"),
                ElementDescriptor("tvVersion", "App Version", "subheadline", "Release version string"),
                ElementDescriptor("rowPrivacyPolicy", "Privacy Link", "body", "Privacy policy navigation"),
                ElementDescriptor("rowTerms", "Terms Link", "body", "Terms of service navigation")
            )
        ),
        ScreenDescriptor(
            key = "syllabus",
            displayName = "Exam Syllabus",
            activityClassNames = listOf("SyllabusActivity"),
            elements = listOf(
                ElementDescriptor("syllabus_title", "Syllabus Title", "title_2", "Syllabus viewer title"),
                ElementDescriptor("syllabus_topic_title", "Topic Heading", "headline", "Subject topic heading"),
                ElementDescriptor("syllabus_subtopic", "Subtopic Item", "body", "Checklist item of syllabus")
            )
        ),
        ScreenDescriptor(
            key = "login",
            displayName = "Login / Sign-In",
            activityClassNames = listOf("LoginActivity"),
            elements = listOf(
                ElementDescriptor("login_logo_text", "App Brand Title", "title_2", "Branded typography"),
                ElementDescriptor("login_tagline", "Tagline / Hero", "title_1", "Primary welcome tagline"),
                ElementDescriptor("login_subtitle", "Subtitle / Instructions", "body", "Supporting explanation text"),
                ElementDescriptor("login_input_text", "Form Field Text", "body", "Email and credential input"),
                ElementDescriptor("login_btn_submit", "Submit Button", "button", "Primary sign-in button")
            )
        ),
        ScreenDescriptor(
            key = "admin",
            displayName = "Admin Dashboard",
            activityClassNames = listOf("AdminActivity"),
            elements = listOf(
                ElementDescriptor("admin_header_title", "Dashboard Title", "title_3", "Admin header text"),
                ElementDescriptor("admin_section_header", "Section Heading", "caption_1", "Admin section overlines"),
                ElementDescriptor("admin_action_button", "Action Buttons", "button", "Management entry buttons"),
                ElementDescriptor("admin_metric_value", "Status Metrics", "body", "Dashboard counters and statistics")
            )
        ),
        ScreenDescriptor(
            key = "admin_analytics",
            displayName = "Admin Analytics",
            activityClassNames = listOf("AdminAnalyticsActivity"),
            elements = listOf(
                ElementDescriptor("tvSummary", "Analytics Summary", "subheadline", "Key statistics overview"),
                ElementDescriptor("tvCrashFreePercentage", "Crash-Free Metric", "headline", "Crash-free user rate"),
                ElementDescriptor("tvExamHeading", "Exam Attempts Heading", "headline", "Bar chart title"),
                ElementDescriptor("tvTopCrashIssues", "Issues Summary", "subheadline", "Crashlytics breakdown")
            )
        ),
        ScreenDescriptor(
            key = "admin_manage_exams",
            displayName = "Manage Exams",
            activityClassNames = listOf("ManageExamsActivity", "ManageExistingExamsActivity", "EditExamActivity"),
            elements = listOf(
                ElementDescriptor("manage_exams_title", "Screen Title", "title_2", "Admin exams management title"),
                ElementDescriptor("manage_exams_item_name", "Exam Name", "headline", "Configured exam label"),
                ElementDescriptor("manage_exams_btn_add", "Add Exam Button", "button", "Create new exam CTA")
            )
        ),
        ScreenDescriptor(
            key = "admin_system_monitor",
            displayName = "System Monitor",
            activityClassNames = listOf("SystemMonitorActivity"),
            elements = listOf(
                ElementDescriptor("sysmon_title", "Monitor Title", "title_2", "Health monitor title"),
                ElementDescriptor("sysmon_service_status", "Service Status", "headline", "Worker/D1 status"),
                ElementDescriptor("sysmon_db_latency", "Database Latency", "subheadline", "Response ping latency")
            )
        ),
        ScreenDescriptor(
            key = "dialogs",
            displayName = "Dialogs & Sheets",
            activityClassNames = emptyList(),
            elements = listOf(
                ElementDescriptor("dialog_title", "Dialog Title", "title_3", "Header text of modal alert"),
                ElementDescriptor("dialog_message", "Dialog Message", "body", "Descriptive content of prompt"),
                ElementDescriptor("dialog_btn_positive", "Positive Action", "button", "Confirm/Submit action"),
                ElementDescriptor("dialog_btn_negative", "Negative Action", "button", "Cancel/Dismiss action"),
                ElementDescriptor("dialog_report_reason", "Report Option Label", "body", "Radio option in report dialog")
            )
        )
    )

    fun findScreen(key: String): ScreenDescriptor? {
        return SCREENS.firstOrNull { it.key.equals(key, ignoreCase = true) }
    }

    fun findScreenForActivity(activity: Activity): String? {
        val simpleName = activity::class.java.simpleName
        return SCREENS.firstOrNull { it.activityClassNames.contains(simpleName) }?.key
    }

    fun allElements(): List<ElementDescriptor> {
        return SCREENS.flatMap { it.elements }
    }

    fun findElement(elementKey: String): ElementDescriptor? {
        return allElements().firstOrNull { it.key.equals(elementKey, ignoreCase = true) }
    }
}
