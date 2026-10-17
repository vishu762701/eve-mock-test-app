package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Automated verification test suite for the Eve Design System foundation
 * and its strict application to the Home screen ONLY.
 */
class EveDesignSystemTest {

    private fun getProjectDir(): File {
        val candidates = listOf(File("."), File("app"), File(".."))
        for (candidate in candidates) {
            val resDir = File(candidate, "app/src/main/res/values")
            if (resDir.exists() && resDir.isDirectory) return candidate
            val directResDir = File(candidate, "src/main/res/values")
            if (directResDir.exists() && directResDir.isDirectory) {
                return candidate.parentFile ?: File(".")
            }
        }
        return File(".")
    }

    private fun loadXml(relativePath: String): Element {
        val root = getProjectDir()
        val candidates = listOf(
            File(root, relativePath),
            File(root, "app/$relativePath"),
            File(relativePath),
            File("app/$relativePath")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find file: $relativePath")
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
    }

    private fun findElementById(node: Node, targetId: String): Element? {
        if (node is Element) {
            val id = node.getAttribute("android:id")
            if (id == targetId || id == "@+id/$targetId" || id.endsWith("/$targetId")) {
                return node
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            val found = findElementById(children.item(i), targetId)
            if (found != null) return found
        }
        return null
    }

    @Test
    fun testEveDesignSystemShapes_ConcentricGeometryRules() {
        // Parent card 26dp with inner padding 18dp -> 8dp concentric radius
        assertEquals(8f, EveDesignSystem.Shapes.concentricRadius(26f, 18f), 0.001f)

        // Parent card 26dp with inner padding 12dp -> 14dp concentric radius
        assertEquals(14f, EveDesignSystem.Shapes.concentricRadius(26f, 12f), 0.001f)

        // Parent sheet 34dp with inner padding 16dp -> 18dp concentric radius
        assertEquals(18f, EveDesignSystem.Shapes.concentricRadius(34f, 16f), 0.001f)

        // Clamping rule: minimum corner radius is 4dp
        assertEquals(4f, EveDesignSystem.Shapes.concentricRadius(10f, 8f), 0.001f)
        assertEquals(4f, EveDesignSystem.Shapes.concentricRadius(26f, 30f), 0.001f)

        // Capsule half-bounds
        assertEquals(24f, EveDesignSystem.Shapes.capsuleRadius(48f), 0.001f)
        assertEquals(20f, EveDesignSystem.Shapes.capsuleRadius(40f), 0.001f)
    }

    @Test
    fun testEveDesignSystemSpacing_4dpGridAndTouchTargets() {
        assertEquals(4, EveDesignSystem.Spacing.SPACE_4_DP)
        assertEquals(8, EveDesignSystem.Spacing.SPACE_8_DP)
        assertEquals(12, EveDesignSystem.Spacing.SPACE_12_DP)
        assertEquals(16, EveDesignSystem.Spacing.SPACE_16_DP)
        assertEquals(20, EveDesignSystem.Spacing.SPACE_20_DP)
        assertEquals(24, EveDesignSystem.Spacing.SPACE_24_DP)
        assertEquals(32, EveDesignSystem.Spacing.SPACE_32_DP)

        assertEquals(48, EveDesignSystem.Spacing.MIN_TOUCH_TARGET_DP)
        assertEquals(52, EveDesignSystem.Spacing.MIN_LIST_ROW_HEIGHT_DP)
        assertEquals(56, EveDesignSystem.Spacing.MIN_SEGMENTED_HEIGHT_DP)
    }

    @Test
    fun testEveDesignSystemMotion_DurationsAndInterpolators() {
        assertEquals(150L, EveDesignSystem.Motion.DURATION_QUICK_MS)
        assertEquals(220L, EveDesignSystem.Motion.DURATION_DISMISS_MS)
        assertEquals(300L, EveDesignSystem.Motion.DURATION_STANDARD_MS)
        assertEquals(280L, EveDesignSystem.Motion.DURATION_SPRING_MS)

        assertNotNull(EveDesignSystem.Motion.standardInterpolator)
        assertNotNull(EveDesignSystem.Motion.springInterpolator)
    }

    @Test
    fun testEveDesignSystemMaterials_LiquidGlassRegularSpec() {
        assertEquals("apple-regular", EveDesignSystem.Materials.SPEC_REGULAR_GLASS)
    }

    @Test
    fun testEveDesignSystemTypography_CanonicalRolesPresent() {
        assertTrue(EveDesignSystem.Typography.LargeTitle != 0)
        assertTrue(EveDesignSystem.Typography.Title1 != 0)
        assertTrue(EveDesignSystem.Typography.Title2 != 0)
        assertTrue(EveDesignSystem.Typography.Title3 != 0)
        assertTrue(EveDesignSystem.Typography.Headline != 0)
        assertTrue(EveDesignSystem.Typography.Body != 0)
        assertTrue(EveDesignSystem.Typography.Callout != 0)
        assertTrue(EveDesignSystem.Typography.Subheadline != 0)
        assertTrue(EveDesignSystem.Typography.SubheadlineMedium != 0)
        assertTrue(EveDesignSystem.Typography.Footnote != 0)
        assertTrue(EveDesignSystem.Typography.FootnoteMedium != 0)
        assertTrue(EveDesignSystem.Typography.Caption1 != 0)
        assertTrue(EveDesignSystem.Typography.Caption1Medium != 0)
        assertTrue(EveDesignSystem.Typography.Caption2 != 0)
        assertTrue(EveDesignSystem.Typography.Eyebrow != 0)
        assertTrue(EveDesignSystem.Typography.Search != 0)
    }

    @Test
    fun testHomeScreenXml_UsesApprovedDesignSystemComponents() {
        val root = loadXml("src/main/res/layout/activity_main.xml")

        // Root is Plain System Background
        assertEquals("@color/eve_system_background", root.getAttribute("android:background"))

        // Eyebrow and LargeTitle greeting
        val tvWelcome = findElementById(root, "tvWelcome")
        assertNotNull("tvWelcome must exist", tvWelcome)
        assertEquals("@style/TextAppearance.Eve.LargeTitle", tvWelcome?.getAttribute("style"))

        // Search field container has 48dp height and bg_search_field
        val searchContainer = findElementById(root, "layoutSearchFieldContainer")
        assertNotNull("layoutSearchFieldContainer must exist", searchContainer)
        assertEquals("48dp", searchContainer?.getAttribute("android:layout_height"))
        assertEquals("@drawable/bg_search_field", searchContainer?.getAttribute("android:background"))

        // Search edit text has Search text appearance
        val etSearch = findElementById(root, "etSearch")
        assertNotNull("etSearch must exist", etSearch)
        assertEquals("@style/TextAppearance.Eve.Search", etSearch?.getAttribute("style"))

        // Bare notification bell with transparent background
        val bell = findElementById(root, "btnNotification")
        assertNotNull("btnNotification must exist", bell)
        assertEquals("@android:color/transparent", bell?.getAttribute("android:background"))

        // Streak pill container
        val streakPill = findElementById(root, "layoutStreakPill")
        assertNotNull("layoutStreakPill must exist", streakPill)
        assertEquals("@drawable/bg_streak_pill", streakPill?.getAttribute("android:background"))

        // Admin button uses Neutral button style and selector surface
        val btnAdmin = findElementById(root, "btnAdmin")
        assertNotNull("btnAdmin must exist", btnAdmin)
        assertEquals("@style/Widget.Eve.Button.Neutral", btnAdmin?.getAttribute("style"))
        assertEquals("@color/selector_home_admin_surface", btnAdmin?.getAttribute("app:backgroundTint"))
        assertEquals("52dp", btnAdmin?.getAttribute("android:minHeight"))
    }

    @Test
    fun testHomeExamAndFeedbackCardXml_UsesContentCardStyle() {
        val examCard = loadXml("src/main/res/layout/item_exam.xml")
        assertEquals("@style/Widget.Eve.Card.Content", examCard.getAttribute("style"))

        val feedbackCard = loadXml("src/main/res/layout/item_feedback_post.xml")
        assertEquals("@style/Widget.Eve.Card.Content", feedbackCard.getAttribute("style"))
    }

    @Test
    fun testHomeAppearanceAndExamAdapter_SourceIntegrity() {
        val root = getProjectDir()
        fun readSrc(rel: String): String {
            val candidates = listOf(File(root, rel), File(root, "app/$rel"), File(rel), File("app/$rel"))
            return candidates.firstOrNull { it.exists() }?.readText()
                ?: error("Missing source file: $rel")
        }

        val homeAppearance = readSrc("src/main/java/com/eve/app/ui/home/HomeAppearance.kt")
        assertTrue(homeAppearance.contains("EveDesignSystem.Components.tintNotificationBell"))
        assertTrue(homeAppearance.contains("EveDesignSystem.Shapes.applyCardSquircle"))
        assertTrue(homeAppearance.contains("EveDesignSystem.Components.createCategoryChip"))

        val examAdapter = readSrc("src/main/java/com/eve/app/ui/home/ExamAdapter.kt")
        assertTrue(examAdapter.contains("EveDesignSystem.Colors.shapeBorder"))
        assertTrue(examAdapter.contains("EveDesignSystem.Colors.labelPrimary"))
        assertTrue(examAdapter.contains("EveDesignSystem.Colors.statusRightText"))
        assertTrue(examAdapter.contains("EveDesignSystem.Colors.labelSecondary"))
        assertTrue(examAdapter.contains("R.color.eve_secondary_system_background"))

        val mainActivity = readSrc("src/main/java/com/eve/app/ui/home/MainActivity.kt")
        assertTrue(mainActivity.contains("EveDesignSystem.Components.applyHomeBannerSurface"))
        assertTrue(mainActivity.contains("EveDesignSystem.Components.applyStreakPill"))
        assertTrue(mainActivity.contains("EveDesignSystem.Components.applyAdminButton"))
        assertTrue(mainActivity.contains("EveDesignSystem.Colors.secondarySystemBackground"))
        assertTrue(mainActivity.contains("EveDesignSystem.Shapes.cardRadius"))
    }
}
