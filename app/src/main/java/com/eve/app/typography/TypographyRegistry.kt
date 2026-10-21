package com.eve.app.typography

/**
 * Searchable, discoverable catalog of actual EVE screens and text elements.
 * Provides stable element keys, human-readable titles, and default semantic roles.
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
        val elements: List<ElementDescriptor>
    )

    val SCREENS = listOf(
        ScreenDescriptor(
            key = "home",
            displayName = "Home",
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
            key = "settings",
            displayName = "Settings",
            elements = listOf(
                ElementDescriptor("settings_screen_title", "Screen Title", "title_3", "Header navigation title"),
                ElementDescriptor("settings_section_header", "Section Header", "caption_1", "Uppercase category overline"),
                ElementDescriptor("settings_item_title", "Setting Row Title", "body", "Primary setting title"),
                ElementDescriptor("settings_item_value", "Setting Row Value", "subheadline", "Secondary setting status")
            )
        ),
        ScreenDescriptor(
            key = "login",
            displayName = "Login / Sign-In",
            elements = listOf(
                ElementDescriptor("login_logo_text", "App Brand Title", "title_2", "Branded typography"),
                ElementDescriptor("login_tagline", "Tagline / Hero", "title_1", "Primary welcome tagline"),
                ElementDescriptor("login_subtitle", "Subtitle / Instructions", "body", "Supporting explanation text"),
                ElementDescriptor("login_input_text", "Form Field Text", "body", "Email and credential input"),
                ElementDescriptor("login_btn_submit", "Submit Button", "button", "Primary sign-in button")
            )
        ),
        ScreenDescriptor(
            key = "profile",
            displayName = "User Profile",
            elements = listOf(
                ElementDescriptor("profile_user_name", "User Name Display", "title_2", "Profile student name"),
                ElementDescriptor("profile_user_email", "User Email Display", "subheadline", "Profile student email"),
                ElementDescriptor("profile_section_title", "Section Title", "headline", "Form section headings"),
                ElementDescriptor("profile_btn_save", "Save Profile Button", "button", "Save action button")
            )
        ),
        ScreenDescriptor(
            key = "admin",
            displayName = "Admin Dashboard",
            elements = listOf(
                ElementDescriptor("admin_header_title", "Dashboard Title", "title_3", "Admin header text"),
                ElementDescriptor("admin_section_header", "Section Heading", "caption_1", "Admin section overlines"),
                ElementDescriptor("admin_action_button", "Action Buttons", "button", "Management entry buttons"),
                ElementDescriptor("admin_metric_value", "Status Metrics", "body", "Dashboard counters and statistics")
            )
        )
    )

    fun findScreen(key: String): ScreenDescriptor? {
        return SCREENS.firstOrNull { it.key.equals(key, ignoreCase = true) }
    }

    fun allElements(): List<ElementDescriptor> {
        return SCREENS.flatMap { it.elements }
    }

    fun findElement(elementKey: String): ElementDescriptor? {
        return allElements().firstOrNull { it.key.equals(elementKey, ignoreCase = true) }
    }
}
