package com.eve.app.uistudio

import com.eve.app.data.model.uistudio.*

/**
 * Central schema registry for EVE UI Studio / App Builder.
 * Defines supported screens, component primitives, validation rules,
 * child constraints, and native baseline templates.
 */
object UiStudioRegistry {

    data class ScreenDescriptor(
        val id: String,
        val displayName: String,
        val description: String,
        val defaultBackgroundColor: String
    )

    data class ComponentTypeDescriptor(
        val type: String,
        val displayName: String,
        val iconRes: String,
        val allowsChildren: Boolean,
        val isContainer: Boolean
    )

    // Registered real application screens
    val SUPPORTED_SCREENS = listOf(
        ScreenDescriptor("home", "Home Screen", "Main student dashboard, banners, search, and exam listings", "#000000"),
        ScreenDescriptor("test", "Test Screen", "Exam examination interface, timer, questions, and action grid", "#000000"),
        ScreenDescriptor("result", "Result Screen", "Score analytics, dashboard summaries, and review actions", "#000000"),
        ScreenDescriptor("profile", "Profile Screen", "Student account overview, statistics, and preferences", "#000000"),
        ScreenDescriptor("notifications", "Notifications Screen", "Notification bulletins and announcement history", "#000000"),
        ScreenDescriptor("syllabus", "Syllabus Screen", "Exam syllabi, topic hierarchy, and syllabus explorer", "#000000"),
        ScreenDescriptor("login", "Login Screen", "Authentication screen, email/password, and Google sign-in", "#000000"),
        ScreenDescriptor("admin", "Admin Dashboard", "Management tabs, exam creation, and administrative tools", "#000000")
    )

    // Registered UI component primitives
    val COMPONENT_TYPES = listOf(
        ComponentTypeDescriptor("card", "Material / Glass Card", "ic_card", true, true),
        ComponentTypeDescriptor("surface", "Translucent Surface", "ic_surface", true, true),
        ComponentTypeDescriptor("text", "Text / Header", "ic_text", false, false),
        ComponentTypeDescriptor("button", "Action Button", "ic_button", false, false),
        ComponentTypeDescriptor("badge", "Pill / Badge", "ic_badge", false, false),
        ComponentTypeDescriptor("image", "Image Element", "ic_image", false, false),
        ComponentTypeDescriptor("container", "Container / Box", "ic_container", true, true),
        ComponentTypeDescriptor("row", "Horizontal Row", "ic_row", true, true),
        ComponentTypeDescriptor("column", "Vertical Column", "ic_column", true, true),
        ComponentTypeDescriptor("spacer", "Layout Spacer", "ic_spacer", false, false),
        ComponentTypeDescriptor("divider", "Separator Line", "ic_divider", false, false),
        ComponentTypeDescriptor("input", "Text Input Field", "ic_input", false, false),
        ComponentTypeDescriptor("toggle", "Switch / Toggle", "ic_toggle", false, false),
        ComponentTypeDescriptor("banner", "Home Banner Surface", "ic_banner", true, true),
        ComponentTypeDescriptor("timer", "Timer Pill Capsule", "ic_timer", false, false),
        ComponentTypeDescriptor("action_grid", "2x2 Action Grid", "ic_grid", true, true)
    )

    /**
     * Creates a new ComponentConfig with safe defaults for a given component primitive.
     */
    fun createDefaultComponent(
        id: String,
        type: String,
        name: String,
        parentId: String? = null,
        order: Int = 0
    ): ComponentConfig {
        return when (type) {
            "text" -> ComponentConfig(
                id = id,
                type = "text",
                name = name,
                parentId = parentId,
                order = order,
                layout = LayoutProperties(marginTop = 4, marginBottom = 4, marginStart = 8, marginEnd = 8),
                typography = TypographyProperties(textColor = "#FFFFFF", textSize = 15, textStyle = "normal"),
                content = ContentProperties(title = name)
            )
            "button" -> ComponentConfig(
                id = id,
                type = "button",
                name = name,
                parentId = parentId,
                order = order,
                layout = LayoutProperties(height = "48", marginTop = 8, marginBottom = 8, marginStart = 16, marginEnd = 16, paddingTop = 12, paddingBottom = 12),
                appearance = AppearanceProperties(backgroundColor = "#007AFF", cornerRadius = 14, strokeWidth = 0),
                typography = TypographyProperties(textColor = "#FFFFFF", textSize = 15, textStyle = "bold", textAlign = "center"),
                content = ContentProperties(title = name),
                actions = ActionProperties(actionType = "none")
            )
            "badge" -> ComponentConfig(
                id = id,
                type = "badge",
                name = name,
                parentId = parentId,
                order = order,
                layout = LayoutProperties(paddingTop = 6, paddingBottom = 6, paddingStart = 12, paddingEnd = 12),
                appearance = AppearanceProperties(backgroundColor = "#2A1F10", cornerRadius = 20, strokeWidth = 1, strokeColor = "#E69B00"),
                typography = TypographyProperties(textColor = "#F59E0B", textSize = 13, textStyle = "bold"),
                content = ContentProperties(title = name)
            )
            "image" -> ComponentConfig(
                id = id,
                type = "image",
                name = name,
                parentId = parentId,
                order = order,
                layout = LayoutProperties(width = "match_parent", height = "180", marginTop = 8, marginBottom = 8),
                appearance = AppearanceProperties(cornerRadius = 16, opacity = 1.0f)
            )
            "divider" -> ComponentConfig(
                id = id,
                type = "divider",
                name = name,
                parentId = parentId,
                order = order,
                layout = LayoutProperties(height = "1", marginTop = 8, marginBottom = 8),
                appearance = AppearanceProperties(backgroundColor = "#334155")
            )
            "spacer" -> ComponentConfig(
                id = id,
                type = "spacer",
                name = name,
                parentId = parentId,
                order = order,
                layout = LayoutProperties(height = "16")
            )
            else -> ComponentConfig(
                id = id,
                type = type,
                name = name,
                parentId = parentId,
                order = order,
                layout = LayoutProperties(marginTop = 8, marginBottom = 8, marginStart = 16, marginEnd = 16, paddingTop = 14, paddingBottom = 14, paddingStart = 14, paddingEnd = 14),
                appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#334155", elevation = 2, opacity = 1.0f),
                material = MaterialProperties(blurRadius = 0, materialOpacity = 1.0f),
                typography = TypographyProperties(textColor = "#FFFFFF", textSize = 16, textStyle = "bold"),
                content = ContentProperties(title = name)
            )
        }
    }

    /**
     * Builds the complete default template for all registered screens with true native styling tokens.
     */
    fun getCompleteDefaultTemplate(): UiStudioConfig {
        val homeScreen = ScreenConfig(
            id = "home",
            name = "Home Screen",
            description = "Main dashboard and student portal",
            backgroundColor = "#000000",
            components = linkedMapOf(
                "home_header" to ComponentConfig(
                    id = "home_header",
                    type = "container",
                    name = "Top Header Bar",
                    isProtected = true,
                    order = 1,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 12, marginBottom = 8),
                    appearance = AppearanceProperties(backgroundColor = "#000000"),
                    children = listOf("header_avatar", "header_title", "header_bell")
                ),
                "hero_banner" to ComponentConfig(
                    id = "hero_banner",
                    type = "banner",
                    name = "Home Banner Surface",
                    isProtected = true,
                    order = 2,
                    layout = LayoutProperties(marginTop = 12, marginBottom = 12, marginStart = 16, marginEnd = 16, paddingTop = 16, paddingBottom = 16, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 20, strokeWidth = 1, strokeColor = "#334155", elevation = 2, opacity = 1.0f),
                    material = MaterialProperties(blurRadius = 15, materialOpacity = 0.9f, tintColor = "#1E293B", tintOpacity = 0.8f),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 18, textStyle = "bold"),
                    content = ContentProperties(title = "EVE Daily Challenge", subtitle = "Test your skills with today's featured exam series")
                ),
                "streak_pill" to ComponentConfig(
                    id = "streak_pill",
                    type = "badge",
                    name = "Streak Pill",
                    isProtected = true,
                    order = 3,
                    layout = LayoutProperties(paddingTop = 6, paddingBottom = 6, paddingStart = 12, paddingEnd = 12),
                    appearance = AppearanceProperties(backgroundColor = "#2A1F10", cornerRadius = 20, strokeWidth = 1, strokeColor = "#E69B00"),
                    material = MaterialProperties(blurRadius = 8, materialOpacity = 0.85f),
                    typography = TypographyProperties(textColor = "#F59E0B", textSize = 13, textStyle = "bold"),
                    content = ContentProperties(title = "7 Day Streak")
                ),
                "find_test_panel" to ComponentConfig(
                    id = "find_test_panel",
                    type = "card",
                    name = "Find-Your-Next-Test Card",
                    isProtected = false,
                    order = 4,
                    layout = LayoutProperties(marginTop = 8, marginBottom = 8, marginStart = 16, marginEnd = 16, paddingTop = 14, paddingBottom = 14, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#334155"),
                    material = MaterialProperties(blurRadius = 10, materialOpacity = 0.9f),
                    typography = TypographyProperties(textColor = "#F8FAFC", textSize = 15),
                    content = ContentProperties(title = "Search & Filter Tests", hint = "Search exams, subjects, and topics...")
                ),
                "active_exams_header" to ComponentConfig(
                    id = "active_exams_header",
                    type = "text",
                    name = "Exams Section Header",
                    isProtected = false,
                    order = 5,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 16, marginBottom = 8),
                    typography = TypographyProperties(textColor = "#94A3B8", textSize = 13, textStyle = "bold"),
                    content = ContentProperties(title = "AVAILABLE EXAM SERIES")
                )
            )
        )

        val testScreen = ScreenConfig(
            id = "test",
            name = "Test Screen",
            description = "Active examination session",
            backgroundColor = "#000000",
            components = linkedMapOf(
                "timer_pill" to ComponentConfig(
                    id = "timer_pill",
                    type = "timer",
                    name = "Timer Pill",
                    isProtected = true,
                    order = 1,
                    layout = LayoutProperties(paddingTop = 6, paddingBottom = 6, paddingStart = 14, paddingEnd = 14),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#334155"),
                    material = MaterialProperties(blurRadius = 12, materialOpacity = 0.9f),
                    typography = TypographyProperties(textColor = "#38BDF8", textSize = 14, textStyle = "bold"),
                    content = ContentProperties(title = "45:00")
                ),
                "question_card" to ComponentConfig(
                    id = "question_card",
                    type = "card",
                    name = "Question Card",
                    isProtected = true,
                    order = 2,
                    layout = LayoutProperties(marginTop = 8, marginBottom = 12, marginStart = 16, marginEnd = 16, paddingTop = 16, paddingBottom = 16, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#334155", elevation = 1),
                    material = MaterialProperties(blurRadius = 8, materialOpacity = 0.95f),
                    typography = TypographyProperties(textColor = "#F8FAFC", textSize = 16),
                    content = ContentProperties(title = "Question prompt content...")
                ),
                "option_item" to ComponentConfig(
                    id = "option_item",
                    type = "card",
                    name = "Answer Option Capsule",
                    isProtected = true,
                    order = 3,
                    layout = LayoutProperties(marginTop = 6, marginBottom = 6, marginStart = 16, marginEnd = 16, paddingTop = 12, paddingBottom = 12, paddingStart = 14, paddingEnd = 14),
                    appearance = AppearanceProperties(backgroundColor = "#0F172A", cornerRadius = 12, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#E2E8F0", textSize = 14),
                    content = ContentProperties(title = "Option capsule label")
                ),
                "action_grid" to ComponentConfig(
                    id = "action_grid",
                    type = "action_grid",
                    name = "2x2 Action Button Grid",
                    isProtected = true,
                    order = 4,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 12, marginBottom = 12),
                    appearance = AppearanceProperties(cornerRadius = 14)
                )
            )
        )

        val resultScreen = ScreenConfig(
            id = "result",
            name = "Result Screen",
            description = "Exam performance and analytics overview",
            backgroundColor = "#000000",
            components = linkedMapOf(
                "result_header" to ComponentConfig(
                    id = "result_header",
                    type = "container",
                    name = "Result Header (Reattempt & Share)",
                    isProtected = true,
                    order = 1,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 12, marginBottom = 8)
                ),
                "score_card" to ComponentConfig(
                    id = "score_card",
                    type = "card",
                    name = "Score Hero Card",
                    isProtected = true,
                    order = 2,
                    layout = LayoutProperties(marginTop = 16, marginBottom = 16, marginStart = 16, marginEnd = 16, paddingTop = 20, paddingBottom = 20, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 18, strokeWidth = 1, strokeColor = "#334155"),
                    material = MaterialProperties(blurRadius = 14, materialOpacity = 0.9f),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 24, textStyle = "bold"),
                    content = ContentProperties(title = "Your Score: 85/100")
                ),
                "analytics_summary" to ComponentConfig(
                    id = "analytics_summary",
                    type = "card",
                    name = "Analytics Stat Pills",
                    isProtected = true,
                    order = 3,
                    layout = LayoutProperties(marginTop = 8, marginBottom = 8, marginStart = 16, marginEnd = 16, paddingTop = 12, paddingBottom = 12, paddingStart = 12, paddingEnd = 12),
                    appearance = AppearanceProperties(backgroundColor = "#0F172A", cornerRadius = 12, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#38BDF8", textSize = 13, textStyle = "bold")
                )
            )
        )

        val profileScreen = ScreenConfig(
            id = "profile",
            name = "Profile Screen",
            description = "Student profile and account settings",
            backgroundColor = "#000000",
            components = linkedMapOf(
                "profile_card" to ComponentConfig(
                    id = "profile_card",
                    type = "card",
                    name = "Profile Summary Card",
                    isProtected = true,
                    order = 1,
                    layout = LayoutProperties(marginTop = 16, marginBottom = 12, marginStart = 16, marginEnd = 16, paddingTop = 16, paddingBottom = 16, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#334155"),
                    material = MaterialProperties(blurRadius = 12, materialOpacity = 0.9f),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 18, textStyle = "bold"),
                    content = ContentProperties(title = "Student Profile")
                ),
                "profile_stats" to ComponentConfig(
                    id = "profile_stats",
                    type = "row",
                    name = "Stats Row",
                    isProtected = false,
                    order = 2,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 8, marginBottom = 8)
                )
            )
        )

        val notificationsScreen = ScreenConfig(
            id = "notifications",
            name = "Notifications Screen",
            description = "Bulletins and announcement feed",
            backgroundColor = "#000000",
            components = linkedMapOf(
                "notification_card" to ComponentConfig(
                    id = "notification_card",
                    type = "card",
                    name = "Notification Feed Item",
                    isProtected = false,
                    order = 1,
                    layout = LayoutProperties(marginTop = 6, marginBottom = 6, marginStart = 16, marginEnd = 16, paddingTop = 14, paddingBottom = 14, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 14, strokeWidth = 1, strokeColor = "#334155"),
                    material = MaterialProperties(blurRadius = 8, materialOpacity = 0.9f),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 15, textStyle = "bold"),
                    content = ContentProperties(title = "Exam Update Notification")
                )
            )
        )

        val syllabusScreen = ScreenConfig(
            id = "syllabus",
            name = "Syllabus Screen",
            description = "Exam topic outlines and syllabus content",
            backgroundColor = "#000000",
            components = linkedMapOf(
                "syllabus_header" to ComponentConfig(
                    id = "syllabus_header",
                    type = "text",
                    name = "Syllabus Topic Title",
                    isProtected = false,
                    order = 1,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 12, marginBottom = 6),
                    typography = TypographyProperties(textColor = "#38BDF8", textSize = 16, textStyle = "bold"),
                    content = ContentProperties(title = "Topic Breakdown")
                )
            )
        )

        val loginScreen = ScreenConfig(
            id = "login",
            name = "Login Screen",
            description = "User authentication and sign-in",
            backgroundColor = "#000000",
            components = linkedMapOf(
                "login_hero" to ComponentConfig(
                    id = "login_hero",
                    type = "card",
                    name = "Welcome Hero Card",
                    isProtected = true,
                    order = 1,
                    layout = LayoutProperties(marginTop = 24, marginBottom = 16, marginStart = 16, marginEnd = 16, paddingTop = 20, paddingBottom = 20, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 20, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 22, textStyle = "bold"),
                    content = ContentProperties(title = "Welcome to EVE")
                ),
                "btn_login" to ComponentConfig(
                    id = "btn_login",
                    type = "button",
                    name = "Sign In Action Button",
                    isProtected = true,
                    order = 2,
                    layout = LayoutProperties(height = "50", marginTop = 12, marginStart = 16, marginEnd = 16, paddingTop = 14, paddingBottom = 14),
                    appearance = AppearanceProperties(backgroundColor = "#007AFF", cornerRadius = 14),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 16, textStyle = "bold", textAlign = "center"),
                    content = ContentProperties(title = "Sign In")
                )
            )
        )

        val adminScreen = ScreenConfig(
            id = "admin",
            name = "Admin Dashboard",
            description = "Administrative control center",
            backgroundColor = "#000000",
            components = linkedMapOf(
                "admin_top_bar" to ComponentConfig(
                    id = "admin_top_bar",
                    type = "container",
                    name = "Admin Dashboard Header",
                    isProtected = true,
                    order = 1,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 12, marginBottom = 8)
                )
            )
        )

        return UiStudioConfig(
            schemaVersion = 1,
            configVersion = 1,
            version = 1,
            notes = "Native Master Design Baseline",
            status = "published",
            screens = mapOf(
                "home" to homeScreen,
                "test" to testScreen,
                "result" to resultScreen,
                "profile" to profileScreen,
                "notifications" to notificationsScreen,
                "syllabus" to syllabusScreen,
                "login" to loginScreen,
                "admin" to adminScreen
            )
        )
    }
}
