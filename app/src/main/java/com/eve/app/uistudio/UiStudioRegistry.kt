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
        val defaultBackgroundColor: String,
        val runtimeActivity: String = "MainActivity",
        val editableRootId: String = "root_container",
        val supportedProperties: List<String> = listOf("backgroundColor", "opacity", "padding"),
        val supportedComponents: List<String> = listOf("*")
    )

    data class ComponentTypeDescriptor(
        val type: String,
        val displayName: String,
        val iconRes: String,
        val allowsChildren: Boolean,
        val isContainer: Boolean,
        val category: String = "General",
        val allowedParents: List<String> = listOf("*"),
        val allowedChildren: List<String> = emptyList(),
        val supportedProperties: List<String> = listOf("layout", "appearance"),
        val supportedStates: List<String> = listOf("default"),
        val supportedActions: List<String> = listOf("none"),
        val supportedAnimations: List<String> = listOf("none"),
        val previewBehavior: String = "render_standard",
        val runtimeBehavior: String = "apply_standard"
    )

    // Registered real application screens
    val SUPPORTED_SCREENS = listOf(
        ScreenDescriptor("home", "Home Screen", "Main student dashboard, banners, search, and exam listings", "#000000", "MainActivity", "root_home_container", listOf("backgroundColor", "opacity", "padding"), listOf("*")),
        ScreenDescriptor("test", "Test Screen", "Exam examination interface, timer, questions, and action grid", "#000000", "TestActivity", "root_test_container", listOf("backgroundColor", "opacity"), listOf("timer", "card", "text", "button", "action_grid")),
        ScreenDescriptor("result", "Result Screen", "Score analytics, dashboard summaries, and review actions", "#000000", "ResultActivity", "root_result_container", listOf("backgroundColor", "opacity"), listOf("*")),
        ScreenDescriptor("profile", "Profile Screen", "Student account overview, statistics, and preferences", "#000000", "ProfileActivity", "root_profile_container", listOf("backgroundColor", "opacity"), listOf("*")),
        ScreenDescriptor("notifications", "Notifications Screen", "Notification bulletins and announcement history", "#000000", "NotificationActivity", "root_notification_container", listOf("backgroundColor"), listOf("*")),
        ScreenDescriptor("syllabus", "Syllabus Screen", "Exam syllabi, topic hierarchy, and syllabus explorer", "#000000", "SyllabusActivity", "root_syllabus_container", listOf("backgroundColor"), listOf("*")),
        ScreenDescriptor("login", "Login Screen", "Authentication screen, email/password, and Google sign-in", "#000000", "LoginActivity", "root_login_container", listOf("backgroundColor"), listOf("*")),
        ScreenDescriptor("admin", "Admin Dashboard", "Management tabs, exam creation, and administrative tools", "#000000", "AdminActivity", "root_admin_container", listOf("backgroundColor"), listOf("*"))
    )

    // Registered UI component primitives
    val COMPONENT_TYPES = listOf(
        ComponentTypeDescriptor(
            type = "card",
            displayName = "Material / Glass Card",
            iconRes = "ic_card",
            allowsChildren = true,
            isContainer = true,
            category = "Surface",
            allowedParents = listOf("*"),
            allowedChildren = listOf("*"),
            supportedProperties = listOf("layout", "appearance", "material", "typography", "actions", "animation", "states"),
            supportedStates = listOf("default", "pressed", "disabled"),
            supportedActions = listOf("navigate", "open_url", "none"),
            supportedAnimations = listOf("fade_in", "slide_up", "scale_up", "none"),
            previewBehavior = "render_card",
            runtimeBehavior = "apply_card_material"
        ),
        ComponentTypeDescriptor(
            type = "surface",
            displayName = "Translucent Surface",
            iconRes = "ic_surface",
            allowsChildren = true,
            isContainer = true,
            category = "Surface",
            allowedParents = listOf("*"),
            allowedChildren = listOf("*"),
            supportedProperties = listOf("layout", "appearance", "material", "actions", "animation"),
            supportedStates = listOf("default"),
            supportedActions = listOf("none"),
            supportedAnimations = listOf("fade_in", "slide_up", "none"),
            previewBehavior = "render_surface",
            runtimeBehavior = "apply_surface_translucent"
        ),
        ComponentTypeDescriptor(
            type = "text",
            displayName = "Text / Header",
            iconRes = "ic_text",
            allowsChildren = false,
            isContainer = false,
            category = "Content",
            allowedParents = listOf("*"),
            allowedChildren = emptyList(),
            supportedProperties = listOf("layout", "typography", "content", "animation"),
            supportedStates = listOf("default"),
            supportedActions = listOf("none"),
            supportedAnimations = listOf("fade_in", "slide_up", "none"),
            previewBehavior = "render_text",
            runtimeBehavior = "apply_text_view"
        ),
        ComponentTypeDescriptor(
            type = "button",
            displayName = "Action Button",
            iconRes = "ic_button",
            allowsChildren = false,
            isContainer = false,
            category = "Action",
            allowedParents = listOf("*"),
            allowedChildren = emptyList(),
            supportedProperties = listOf("layout", "appearance", "typography", "content", "actions", "animation", "states"),
            supportedStates = listOf("default", "pressed", "disabled"),
            supportedActions = listOf("navigate", "open_url", "custom", "none"),
            supportedAnimations = listOf("scale_up", "fade_in", "none"),
            previewBehavior = "render_button",
            runtimeBehavior = "apply_material_button"
        ),
        ComponentTypeDescriptor(
            type = "badge",
            displayName = "Pill / Badge",
            iconRes = "ic_badge",
            allowsChildren = false,
            isContainer = false,
            category = "Display",
            allowedParents = listOf("*"),
            allowedChildren = emptyList(),
            supportedProperties = listOf("layout", "appearance", "typography", "content"),
            supportedStates = listOf("default"),
            supportedActions = listOf("none"),
            supportedAnimations = listOf("fade_in", "none"),
            previewBehavior = "render_badge",
            runtimeBehavior = "apply_badge_capsule"
        ),
        ComponentTypeDescriptor(
            type = "image",
            displayName = "Image Element",
            iconRes = "ic_image",
            allowsChildren = false,
            isContainer = false,
            category = "Media",
            allowedParents = listOf("*"),
            allowedChildren = emptyList(),
            supportedProperties = listOf("layout", "appearance", "content", "actions", "animation"),
            supportedStates = listOf("default"),
            supportedActions = listOf("navigate", "open_url", "none"),
            supportedAnimations = listOf("fade_in", "scale_up", "none"),
            previewBehavior = "render_image",
            runtimeBehavior = "apply_image_view"
        ),
        ComponentTypeDescriptor(
            type = "container",
            displayName = "Container / Box",
            iconRes = "ic_container",
            allowsChildren = true,
            isContainer = true,
            category = "Layout",
            allowedParents = listOf("*"),
            allowedChildren = listOf("*"),
            supportedProperties = listOf("layout", "appearance", "animation"),
            supportedStates = listOf("default"),
            supportedActions = listOf("none"),
            supportedAnimations = listOf("fade_in", "none"),
            previewBehavior = "render_container",
            runtimeBehavior = "apply_view_group"
        ),
        ComponentTypeDescriptor(
            type = "row",
            displayName = "Horizontal Row",
            iconRes = "ic_row",
            allowsChildren = true,
            isContainer = true,
            category = "Layout",
            allowedParents = listOf("*"),
            allowedChildren = listOf("*"),
            supportedProperties = listOf("layout", "appearance"),
            supportedStates = listOf("default"),
            supportedActions = listOf("none"),
            supportedAnimations = listOf("none"),
            previewBehavior = "render_linear_horizontal",
            runtimeBehavior = "apply_linear_horizontal"
        ),
        ComponentTypeDescriptor(
            type = "column",
            displayName = "Vertical Column",
            iconRes = "ic_column",
            allowsChildren = true,
            isContainer = true,
            category = "Layout",
            allowedParents = listOf("*"),
            allowedChildren = listOf("*"),
            supportedProperties = listOf("layout", "appearance"),
            supportedStates = listOf("default"),
            supportedActions = listOf("none"),
            supportedAnimations = listOf("none"),
            previewBehavior = "render_linear_vertical",
            runtimeBehavior = "apply_linear_vertical"
        ),
        ComponentTypeDescriptor(
            type = "spacer",
            displayName = "Layout Spacer",
            iconRes = "ic_spacer",
            allowsChildren = false,
            isContainer = false,
            category = "Layout",
            allowedParents = listOf("*"),
            allowedChildren = emptyList(),
            supportedProperties = listOf("layout"),
            supportedStates = listOf("default"),
            supportedActions = listOf("none"),
            supportedAnimations = listOf("none"),
            previewBehavior = "render_space",
            runtimeBehavior = "apply_space_view"
        ),
        ComponentTypeDescriptor(
            type = "divider",
            displayName = "Separator Line",
            iconRes = "ic_divider",
            allowsChildren = false,
            isContainer = false,
            category = "Layout",
            allowedParents = listOf("*"),
            allowedChildren = emptyList(),
            supportedProperties = listOf("layout", "appearance"),
            supportedStates = listOf("default"),
            supportedActions = listOf("none"),
            supportedAnimations = listOf("none"),
            previewBehavior = "render_divider",
            runtimeBehavior = "apply_divider_view"
        ),
        ComponentTypeDescriptor(
            type = "input",
            displayName = "Text Input Field",
            iconRes = "ic_input",
            allowsChildren = false,
            isContainer = false,
            category = "Form",
            allowedParents = listOf("*"),
            allowedChildren = emptyList(),
            supportedProperties = listOf("layout", "appearance", "typography", "content", "states"),
            supportedStates = listOf("default", "focused", "disabled"),
            supportedActions = listOf("none"),
            supportedAnimations = listOf("fade_in", "none"),
            previewBehavior = "render_input",
            runtimeBehavior = "apply_edit_text"
        ),
        ComponentTypeDescriptor(
            type = "toggle",
            displayName = "Switch / Toggle",
            iconRes = "ic_toggle",
            allowsChildren = false,
            isContainer = false,
            category = "Form",
            allowedParents = listOf("*"),
            allowedChildren = emptyList(),
            supportedProperties = listOf("layout", "appearance", "content", "actions", "states"),
            supportedStates = listOf("default", "pressed", "disabled"),
            supportedActions = listOf("custom", "none"),
            supportedAnimations = listOf("none"),
            previewBehavior = "render_switch",
            runtimeBehavior = "apply_switch_material"
        ),
        ComponentTypeDescriptor(
            type = "banner",
            displayName = "Home Banner Surface",
            iconRes = "ic_banner",
            allowsChildren = true,
            isContainer = true,
            category = "Feature",
            allowedParents = listOf("home"),
            allowedChildren = listOf("card", "text", "button", "image"),
            supportedProperties = listOf("layout", "appearance", "content", "actions", "animation"),
            supportedStates = listOf("default", "pressed"),
            supportedActions = listOf("navigate", "open_url", "none"),
            supportedAnimations = listOf("fade_in", "slide_up", "none"),
            previewBehavior = "render_banner_surface",
            runtimeBehavior = "apply_banner_viewpager"
        ),
        ComponentTypeDescriptor(
            type = "timer",
            displayName = "Timer Pill Capsule",
            iconRes = "ic_timer",
            allowsChildren = false,
            isContainer = false,
            category = "Widget",
            allowedParents = listOf("test"),
            allowedChildren = emptyList(),
            supportedProperties = listOf("layout", "appearance", "typography", "animation"),
            supportedStates = listOf("default", "warning"),
            supportedActions = listOf("none"),
            supportedAnimations = listOf("fade_in", "scale_up", "none"),
            previewBehavior = "render_timer_capsule",
            runtimeBehavior = "apply_exam_timer"
        ),
        ComponentTypeDescriptor(
            type = "action_grid",
            displayName = "2x2 Action Grid",
            iconRes = "ic_grid",
            allowsChildren = true,
            isContainer = true,
            category = "Layout",
            allowedParents = listOf("test", "home"),
            allowedChildren = listOf("button", "card"),
            supportedProperties = listOf("layout", "appearance"),
            supportedStates = listOf("default"),
            supportedActions = listOf("none"),
            supportedAnimations = listOf("fade_in", "none"),
            previewBehavior = "render_grid_2x2",
            runtimeBehavior = "apply_action_grid"
        )
    )

    fun getComponentType(type: String): ComponentTypeDescriptor? {
        return COMPONENT_TYPES.find { it.type == type }
    }

    fun getScreen(id: String): ScreenDescriptor? {
        return SUPPORTED_SCREENS.find { it.id == id }
    }

    fun isChildAllowed(parentType: String, childType: String): Boolean {
        val parent = getComponentType(parentType) ?: return false
        if (!parent.allowsChildren) return false
        if (parent.allowedChildren.contains("*")) return true
        return parent.allowedChildren.contains(childType)
    }

    fun isPropertySupported(type: String, propertyCategory: String): Boolean {
        val comp = getComponentType(type) ?: return true
        return comp.supportedProperties.contains(propertyCategory)
    }

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
                ),
                "featured_exam_card" to ComponentConfig(
                    id = "featured_exam_card",
                    type = "card",
                    name = "Featured Exam Card",
                    isProtected = false,
                    order = 6,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 8, marginBottom = 8, paddingTop = 14, paddingBottom = 14, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 16, textStyle = "bold"),
                    content = ContentProperties(title = "RPSC RAS Prelims Full Mock 2026", subtitle = "150 Questions • 200 Marks • 180 Mins")
                ),
                "home_bottom_nav" to ComponentConfig(
                    id = "home_bottom_nav",
                    type = "container",
                    name = "Bottom Navigation Bar",
                    isProtected = true,
                    order = 7,
                    layout = LayoutProperties(height = "56", marginTop = 8),
                    appearance = AppearanceProperties(backgroundColor = "#0F172A", strokeWidth = 1, strokeColor = "#1E293B")
                )
            )
        )

        val testScreen = ScreenConfig(
            id = "test",
            name = "Test Screen",
            description = "Active examination session",
            backgroundColor = "#000000",
            components = linkedMapOf(
                "test_top_bar" to ComponentConfig(
                    id = "test_top_bar",
                    type = "container",
                    name = "Test Header Bar",
                    isProtected = true,
                    order = 1,
                    layout = LayoutProperties(height = "48", marginStart = 16, marginEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#000000"),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 15, textStyle = "bold"),
                    content = ContentProperties(title = "Paper 1 - General Studies")
                ),
                "timer_pill" to ComponentConfig(
                    id = "timer_pill",
                    type = "timer",
                    name = "Timer Pill",
                    isProtected = true,
                    order = 2,
                    layout = LayoutProperties(paddingTop = 6, paddingBottom = 6, paddingStart = 14, paddingEnd = 14),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#334155"),
                    material = MaterialProperties(blurRadius = 12, materialOpacity = 0.9f),
                    typography = TypographyProperties(textColor = "#38BDF8", textSize = 14, textStyle = "bold"),
                    content = ContentProperties(title = "45:00")
                ),
                "question_palette" to ComponentConfig(
                    id = "question_palette",
                    type = "row",
                    name = "Question Number Strip",
                    isProtected = false,
                    order = 3,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 4, marginBottom = 8)
                ),
                "question_card" to ComponentConfig(
                    id = "question_card",
                    type = "card",
                    name = "Question Card",
                    isProtected = true,
                    order = 4,
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
                    order = 5,
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
                    order = 6,
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
                "result_tabs" to ComponentConfig(
                    id = "result_tabs",
                    type = "container",
                    name = "Segmented Tabs (Overview/Review/Leaderboard)",
                    isProtected = true,
                    order = 3,
                    layout = LayoutProperties(height = "44", marginStart = 16, marginEnd = 16, marginTop = 8, marginBottom = 8),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 22, strokeWidth = 1, strokeColor = "#334155")
                ),
                "analytics_summary" to ComponentConfig(
                    id = "analytics_summary",
                    type = "card",
                    name = "Analytics Stat Pills",
                    isProtected = true,
                    order = 4,
                    layout = LayoutProperties(marginTop = 8, marginBottom = 8, marginStart = 16, marginEnd = 16, paddingTop = 12, paddingBottom = 12, paddingStart = 12, paddingEnd = 12),
                    appearance = AppearanceProperties(backgroundColor = "#0F172A", cornerRadius = 12, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#38BDF8", textSize = 13, textStyle = "bold")
                ),
                "result_insight" to ComponentConfig(
                    id = "result_insight",
                    type = "card",
                    name = "Cutoff & Insight Card",
                    isProtected = false,
                    order = 5,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 8, marginBottom = 12, paddingTop = 14, paddingBottom = 14, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 14, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#34C759", textSize = 14, textStyle = "bold"),
                    content = ContentProperties(title = "Cutoff Cleared: Qualified for Mains")
                ),
                "result_bottom_bar" to ComponentConfig(
                    id = "result_bottom_bar",
                    type = "button",
                    name = "Bottom Action Pill",
                    isProtected = true,
                    order = 6,
                    layout = LayoutProperties(height = "48", marginStart = 16, marginEnd = 16, marginTop = 8, marginBottom = 16, paddingTop = 12, paddingBottom = 12),
                    appearance = AppearanceProperties(backgroundColor = "#007AFF", cornerRadius = 24),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 15, textStyle = "bold", textAlign = "center"),
                    content = ContentProperties(title = "Review All Questions")
                )
            )
        )

        val profileScreen = ScreenConfig(
            id = "profile",
            name = "Profile Screen",
            description = "Student profile and account settings",
            backgroundColor = "#000000",
            components = linkedMapOf(
                "profile_header" to ComponentConfig(
                    id = "profile_header",
                    type = "container",
                    name = "Student Profile Header",
                    isProtected = true,
                    order = 1,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 12, marginBottom = 8),
                    appearance = AppearanceProperties(backgroundColor = "#000000"),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 18, textStyle = "bold"),
                    content = ContentProperties(title = "Vikram Sharma", subtitle = "ID: EV-849201")
                ),
                "profile_card" to ComponentConfig(
                    id = "profile_card",
                    type = "card",
                    name = "Profile Summary Card",
                    isProtected = true,
                    order = 2,
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
                    order = 3,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 8, marginBottom = 8)
                ),
                "profile_rows" to ComponentConfig(
                    id = "profile_rows",
                    type = "card",
                    name = "Account Menu Sections",
                    isProtected = false,
                    order = 4,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 8, marginBottom = 12, paddingTop = 12, paddingBottom = 12, paddingStart = 14, paddingEnd = 14),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#F8FAFC", textSize = 14),
                    content = ContentProperties(title = "Preferences & Vault", subtitle = "Bookmarks • Mistakes • Dark Theme")
                ),
                "btn_logout" to ComponentConfig(
                    id = "btn_logout",
                    type = "button",
                    name = "Sign Out Button",
                    isProtected = true,
                    order = 5,
                    layout = LayoutProperties(height = "46", marginStart = 16, marginEnd = 16, marginTop = 8, marginBottom = 16, paddingTop = 12, paddingBottom = 12),
                    appearance = AppearanceProperties(backgroundColor = "#2A1215", cornerRadius = 23, strokeWidth = 1, strokeColor = "#FF3B30"),
                    typography = TypographyProperties(textColor = "#FF453A", textSize = 14, textStyle = "bold", textAlign = "center"),
                    content = ContentProperties(title = "Sign Out")
                )
            )
        )

        val notificationsScreen = ScreenConfig(
            id = "notifications",
            name = "Notifications Screen",
            description = "Bulletins and announcement feed",
            backgroundColor = "#000000",
            components = linkedMapOf(
                "notifications_header" to ComponentConfig(
                    id = "notifications_header",
                    type = "container",
                    name = "Notifications Header",
                    isProtected = true,
                    order = 1,
                    layout = LayoutProperties(marginStart = 16, marginEnd = 16, marginTop = 12, marginBottom = 8),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 20, textStyle = "bold"),
                    content = ContentProperties(title = "Notifications", subtitle = "Mark All Read")
                ),
                "notification_card" to ComponentConfig(
                    id = "notification_card",
                    type = "card",
                    name = "Notification Feed Item",
                    isProtected = false,
                    order = 2,
                    layout = LayoutProperties(marginTop = 6, marginBottom = 6, marginStart = 16, marginEnd = 16, paddingTop = 14, paddingBottom = 14, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 14, strokeWidth = 1, strokeColor = "#334155"),
                    material = MaterialProperties(blurRadius = 8, materialOpacity = 0.9f),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 15, textStyle = "bold"),
                    content = ContentProperties(title = "Exam Update Notification")
                ),
                "notification_card_2" to ComponentConfig(
                    id = "notification_card_2",
                    type = "card",
                    name = "Series Update Bulletin",
                    isProtected = false,
                    order = 3,
                    layout = LayoutProperties(marginTop = 6, marginBottom = 6, marginStart = 16, marginEnd = 16, paddingTop = 14, paddingBottom = 14, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 14, strokeWidth = 1, strokeColor = "#334155"),
                    material = MaterialProperties(blurRadius = 8, materialOpacity = 0.9f),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 15, textStyle = "bold"),
                    content = ContentProperties(title = "New test series added for Current Affairs", subtitle = "2 hours ago")
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
                ),
                "syllabus_selector" to ComponentConfig(
                    id = "syllabus_selector",
                    type = "badge",
                    name = "Exam Selector Pill",
                    isProtected = false,
                    order = 2,
                    layout = LayoutProperties(marginStart = 16, marginTop = 4, marginBottom = 8, paddingTop = 6, paddingBottom = 6, paddingStart = 12, paddingEnd = 12),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#38BDF8"),
                    typography = TypographyProperties(textColor = "#38BDF8", textSize = 12, textStyle = "bold"),
                    content = ContentProperties(title = "RPSC RAS 2026")
                ),
                "syllabus_card" to ComponentConfig(
                    id = "syllabus_card",
                    type = "card",
                    name = "Subject Progress Card",
                    isProtected = false,
                    order = 3,
                    layout = LayoutProperties(marginTop = 8, marginBottom = 8, marginStart = 16, marginEnd = 16, paddingTop = 14, paddingBottom = 14, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 15, textStyle = "bold"),
                    content = ContentProperties(title = "General Science & Technology", subtitle = "85% syllabus covered • 12 Topics")
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
                "login_inputs" to ComponentConfig(
                    id = "login_inputs",
                    type = "card",
                    name = "Credentials Card (Email & Password)",
                    isProtected = false,
                    order = 2,
                    layout = LayoutProperties(marginTop = 8, marginBottom = 12, marginStart = 16, marginEnd = 16, paddingTop = 16, paddingBottom = 16, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 14),
                    content = ContentProperties(title = "Account Details", subtitle = "student@example.com")
                ),
                "btn_login" to ComponentConfig(
                    id = "btn_login",
                    type = "button",
                    name = "Sign In Action Button",
                    isProtected = true,
                    order = 3,
                    layout = LayoutProperties(height = "50", marginTop = 12, marginStart = 16, marginEnd = 16, paddingTop = 14, paddingBottom = 14),
                    appearance = AppearanceProperties(backgroundColor = "#007AFF", cornerRadius = 14),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 16, textStyle = "bold", textAlign = "center"),
                    content = ContentProperties(title = "Sign In")
                ),
                "login_google" to ComponentConfig(
                    id = "login_google",
                    type = "button",
                    name = "Google Sign In Button",
                    isProtected = false,
                    order = 4,
                    layout = LayoutProperties(height = "48", marginTop = 8, marginStart = 16, marginEnd = 16, paddingTop = 12, paddingBottom = 12),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 14, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 15, textStyle = "bold", textAlign = "center"),
                    content = ContentProperties(title = "Continue with Google")
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
                ),
                "admin_modules" to ComponentConfig(
                    id = "admin_modules",
                    type = "card",
                    name = "Administration Modules Grid",
                    isProtected = false,
                    order = 2,
                    layout = LayoutProperties(marginTop = 12, marginBottom = 12, marginStart = 16, marginEnd = 16, paddingTop = 16, paddingBottom = 16, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#1E293B", cornerRadius = 16, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#FFFFFF", textSize = 16, textStyle = "bold"),
                    content = ContentProperties(title = "Operations Center", subtitle = "Manage Tests • Banners • UI Studio • Users")
                ),
                "admin_health" to ComponentConfig(
                    id = "admin_health",
                    type = "card",
                    name = "Telemetry & System Status",
                    isProtected = false,
                    order = 3,
                    layout = LayoutProperties(marginTop = 8, marginBottom = 16, marginStart = 16, marginEnd = 16, paddingTop = 14, paddingBottom = 14, paddingStart = 16, paddingEnd = 16),
                    appearance = AppearanceProperties(backgroundColor = "#0F172A", cornerRadius = 14, strokeWidth = 1, strokeColor = "#334155"),
                    typography = TypographyProperties(textColor = "#34C759", textSize = 13, textStyle = "bold"),
                    content = ContentProperties(title = "Cloudflare D1: Healthy (11 Migrations)", subtitle = "Active Test Workers Online")
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
