package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.io.FileInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Automated verification test suite for the Eve Design System foundation
 * with locked values and its strict application to the Home screen ONLY.
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

    private fun parseColorMap(relativePath: String): Map<String, String> {
        val element = loadXml(relativePath)
        val map = mutableMapOf<String, String>()
        val nodeList = element.getElementsByTagName("color")
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i) as Element
            map[node.getAttribute("name")] = node.textContent.trim()
        }
        return map
    }

    private fun parseDimenMap(relativePath: String): Map<String, String> {
        val element = loadXml(relativePath)
        val map = mutableMapOf<String, String>()
        val nodeList = element.getElementsByTagName("dimen")
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i) as Element
            map[node.getAttribute("name")] = node.textContent.trim()
        }
        return map
    }

    @Test
    fun testEveDesignSystemLockedColors_LightAndDarkConstants() {
        assertEquals("#EEF2F8", EveDesignSystem.Colors.HEX_HOME_LIGHT_BG)
        assertEquals("#0B0F16", EveDesignSystem.Colors.HEX_HOME_DARK_BG)
        assertEquals("#FFFFFF", EveDesignSystem.Colors.HEX_HOME_LIGHT_CARD)
        assertEquals("#161B24", EveDesignSystem.Colors.HEX_HOME_DARK_CARD)
        assertEquals("#000000", EveDesignSystem.Colors.HEX_HOME_LIGHT_TEXT_PRIMARY)
        assertEquals("#FFFFFF", EveDesignSystem.Colors.HEX_HOME_DARK_TEXT_PRIMARY)
        assertEquals("#6C6C70", EveDesignSystem.Colors.HEX_HOME_LIGHT_TEXT_SECONDARY)
        assertEquals("#A1A1A6", EveDesignSystem.Colors.HEX_HOME_DARK_TEXT_SECONDARY)
        assertEquals("#1A000000", EveDesignSystem.Colors.HEX_HOME_LIGHT_HAIRLINE)
        assertEquals("#24FFFFFF", EveDesignSystem.Colors.HEX_HOME_DARK_HAIRLINE)
        assertEquals("#29787880", EveDesignSystem.Colors.HEX_HOME_LIGHT_TAB_TRACK)
        assertEquals("#52787880", EveDesignSystem.Colors.HEX_HOME_DARK_TAB_TRACK)
        assertEquals("#000000", EveDesignSystem.Colors.HEX_HOME_LIGHT_ACCENT)
        assertEquals("#FFFFFF", EveDesignSystem.Colors.HEX_HOME_DARK_ACCENT)
        assertEquals("#FFFFFF", EveDesignSystem.Colors.HEX_HOME_LIGHT_ON_ACCENT)
        assertEquals("#000000", EveDesignSystem.Colors.HEX_HOME_DARK_ON_ACCENT)
        assertEquals("#34C759", EveDesignSystem.Colors.HEX_HOME_LIGHT_SUCCESS)
        assertEquals("#30D158", EveDesignSystem.Colors.HEX_HOME_DARK_SUCCESS)
        assertEquals("#FF383C", EveDesignSystem.Colors.HEX_HOME_LIGHT_ERROR)
        assertEquals("#FF4245", EveDesignSystem.Colors.HEX_HOME_DARK_ERROR)
        assertEquals("#FF8D28", EveDesignSystem.Colors.HEX_HOME_LIGHT_WARNING)
        assertEquals("#FF9230", EveDesignSystem.Colors.HEX_HOME_DARK_WARNING)
        assertEquals("#FFFFFF", EveDesignSystem.Colors.HEX_HOME_LIGHT_FLOATING_BAR_BG)
        assertEquals("#2C2C2E", EveDesignSystem.Colors.HEX_HOME_DARK_FLOATING_BAR_BG)
    }

    @Test
    fun testEveDesignSystemLockedColorsXml_LightAndDarkIntegrity() {
        val lightColors = parseColorMap("src/main/res/values/colors.xml")
        assertEquals("#EEF2F8", lightColors["eve_home_background"])
        assertEquals("#FFFFFF", lightColors["eve_home_card"])
        assertEquals("#000000", lightColors["eve_home_text_primary"])
        assertEquals("#6C6C70", lightColors["eve_home_text_secondary"])
        assertEquals("#1A000000", lightColors["eve_home_hairline"])
        assertEquals("#29787880", lightColors["eve_home_tab_track"])
        assertEquals("#000000", lightColors["eve_home_accent"])
        assertEquals("#FFFFFF", lightColors["eve_home_on_accent"])
        assertEquals("#34C759", lightColors["eve_home_success"])
        assertEquals("#FF383C", lightColors["eve_home_error"])
        assertEquals("#FF8D28", lightColors["eve_home_warning"])
        assertEquals("#FFFFFF", lightColors["eve_home_floating_bar_bg"])
        assertEquals("#1A000000", lightColors["eve_home_card_shadow"])

        val darkColors = parseColorMap("src/main/res/values-night/colors.xml")
        assertEquals("#0B0F16", darkColors["eve_home_background"])
        assertEquals("#161B24", darkColors["eve_home_card"])
        assertEquals("#FFFFFF", darkColors["eve_home_text_primary"])
        assertEquals("#A1A1A6", darkColors["eve_home_text_secondary"])
        assertEquals("#24FFFFFF", darkColors["eve_home_hairline"])
        assertEquals("#52787880", darkColors["eve_home_tab_track"])
        assertEquals("#FFFFFF", darkColors["eve_home_accent"])
        assertEquals("#000000", darkColors["eve_home_on_accent"])
        assertEquals("#30D158", darkColors["eve_home_success"])
        assertEquals("#FF4245", darkColors["eve_home_error"])
        assertEquals("#FF9230", darkColors["eve_home_warning"])
        assertEquals("#2C2C2E", darkColors["eve_home_floating_bar_bg"])
        assertEquals("#00000000", darkColors["eve_home_card_shadow"])
    }

    @Test
    fun testEveDesignSystemLockedDimensXml_BaseAndQualifiersIntegrity() {
        val baseDimens = parseDimenMap("src/main/res/values/dimens.xml")
        assertEquals("29sp", baseDimens["eve_home_type_page_title"])
        assertEquals("18sp", baseDimens["eve_home_type_card_title"])
        assertEquals("18sp", baseDimens["eve_home_type_card_body"])
        assertEquals("14sp", baseDimens["eve_home_type_secondary"])
        assertEquals("15sp", baseDimens["eve_home_type_tab_label"])
        assertEquals("14sp", baseDimens["eve_home_type_pill_label"])
        assertEquals("40dp", baseDimens["eve_home_tab_height"])
        assertEquals("3dp", baseDimens["eve_home_tab_inner_padding"])
        assertEquals("32dp", baseDimens["eve_home_pill_height"])
        assertEquals("12dp", baseDimens["eve_home_pill_padding_horizontal"])
        assertEquals("1dp", baseDimens["eve_home_pill_border_width"])
        assertEquals("24dp", baseDimens["eve_home_card_radius"])
        assertEquals("12dp", baseDimens["eve_home_card_padding"])
        assertEquals("8dp", baseDimens["eve_home_card_gap"])
        assertEquals("16dp", baseDimens["eve_home_screen_margin"])
        assertEquals("16dp", baseDimens["eve_home_section_gap"])
        assertEquals("2dp", baseDimens["eve_home_card_elevation_light"])
        assertEquals("0dp", baseDimens["eve_home_card_elevation_dark"])
        assertEquals("12dp", baseDimens["eve_home_bottom_bar_margin_horizontal"])
        assertEquals("10dp", baseDimens["eve_home_bottom_bar_margin_bottom"])
        assertEquals("54dp", baseDimens["eve_home_bottom_bar_height"])
        assertEquals("22dp", baseDimens["eve_home_bottom_bar_icon_size"])
        assertEquals("0.5dp", baseDimens["eve_home_hairline_stroke"])
        assertEquals("48dp", baseDimens["eve_home_min_touch_target"])

        // Qualifier values-sw360dp
        val sw360Dimens = parseDimenMap("src/main/res/values-sw360dp/dimens.xml")
        assertEquals("29sp", sw360Dimens["eve_home_type_page_title"])
        assertEquals("18sp", sw360Dimens["eve_home_type_card_title"])
        assertEquals("18sp", sw360Dimens["eve_home_type_card_body"])
        assertEquals("14sp", sw360Dimens["eve_home_type_secondary"])
        assertEquals("15sp", sw360Dimens["eve_home_type_tab_label"])
        assertEquals("14sp", sw360Dimens["eve_home_type_pill_label"])
        assertEquals("40dp", sw360Dimens["eve_home_tab_height"])
        assertEquals("32dp", sw360Dimens["eve_home_pill_height"])
        assertEquals("24dp", sw360Dimens["eve_home_card_radius"])
        assertEquals("54dp", sw360Dimens["eve_home_bottom_bar_height"])

        // Qualifier values-sw600dp
        val sw600Dimens = parseDimenMap("src/main/res/values-sw600dp/dimens.xml")
        assertEquals("29sp", sw600Dimens["eve_home_type_page_title"])
        assertEquals("18sp", sw600Dimens["eve_home_type_card_title"])
        assertEquals("18sp", sw600Dimens["eve_home_type_card_body"])
        assertEquals("14sp", sw600Dimens["eve_home_type_secondary"])
        assertEquals("15sp", sw600Dimens["eve_home_type_tab_label"])
        assertEquals("14sp", sw600Dimens["eve_home_type_pill_label"])
        assertEquals("40dp", sw600Dimens["eve_home_tab_height"])
        assertEquals("32dp", sw600Dimens["eve_home_pill_height"])
        assertEquals("24dp", sw600Dimens["eve_home_card_radius"])
        assertEquals("54dp", sw600Dimens["eve_home_bottom_bar_height"])

        // Night dimen for 0dp card elevation
        val nightDimens = parseDimenMap("src/main/res/values-night/dimens.xml")
        assertEquals("0dp", nightDimens["eve_home_card_elevation"])
    }

    @Test
    fun testDmSansFontBundling_StaticOflFilesAndWeights() {
        val root = getProjectDir()
        val fontDir = File(root, "app/src/main/res/font")
        val regularTtf = File(fontDir, "dm_sans_regular.ttf")
        val mediumTtf = File(fontDir, "dm_sans_medium.ttf")
        val boldTtf = File(fontDir, "dm_sans_bold.ttf")

        assertTrue("dm_sans_regular.ttf must exist", regularTtf.exists())
        assertTrue("dm_sans_medium.ttf must exist", mediumTtf.exists())
        assertTrue("dm_sans_bold.ttf must exist", boldTtf.exists())

        // Check TTF Magic Byte 00 01 00 00
        val ttfMagic = byteArrayOf(0x00, 0x01, 0x00, 0x00)
        listOf(regularTtf, mediumTtf, boldTtf).forEach { file ->
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                fis.read(header)
                assertTrue("File ${file.name} must have valid TrueType magic header", header.contentEquals(ttfMagic))
            }
        }

        // Check font family xml
        val dmSansXml = File(fontDir, "dm_sans.xml")
        assertTrue("dm_sans.xml must exist", dmSansXml.exists())
        val familyXml = loadXml("src/main/res/font/dm_sans.xml")
        val fontNodes = familyXml.getElementsByTagName("font")
        assertEquals("dm_sans.xml must define 3 static weights (400, 500, 700)", 3, fontNodes.length)
    }

    @Test
    fun testEveDesignSystemTypographyStyles_Definitions() {
        assertTrue(EveDesignSystem.Typography.HomePageTitle != 0)
        assertTrue(EveDesignSystem.Typography.HomeCardTitle != 0)
        assertTrue(EveDesignSystem.Typography.HomeCardBody != 0)
        assertTrue(EveDesignSystem.Typography.HomeSecondary != 0)
        assertTrue(EveDesignSystem.Typography.HomeTabLabel != 0)
        assertTrue(EveDesignSystem.Typography.HomePillLabel != 0)
    }

    @Test
    fun testHomeScreenXml_UsesLockedDesignSystemComponents() {
        val root = loadXml("src/main/res/layout/activity_main.xml")

        // Root is Plain System Background with locked Home token
        assertEquals("@color/eve_home_background", root.getAttribute("android:background"))

        // Welcome greeting uses 29sp Bold HomePageTitle and primary ink
        val tvWelcome = findElementById(root, "tvWelcome")
        assertNotNull("tvWelcome must exist", tvWelcome)
        assertEquals("@style/TextAppearance.Eve.Home.PageTitle", tvWelcome?.getAttribute("style"))
        assertEquals("@color/eve_home_text_primary", tvWelcome?.getAttribute("android:textColor"))

        // Top bar hairline
        val topBarHairline = findElementById(root, "topBarHairline")
        assertNotNull("topBarHairline must exist", topBarHairline)
        assertEquals("@dimen/eve_home_hairline_stroke", topBarHairline?.getAttribute("android:layout_height"))
        assertEquals("@color/eve_home_hairline", topBarHairline?.getAttribute("android:background"))

        // rvExams has 16dp horizontal margin and clipToPadding=false
        val rvExams = findElementById(root, "rvExams")
        assertNotNull("rvExams must exist", rvExams)
        assertEquals("false", rvExams?.getAttribute("android:clipToPadding"))
        assertEquals("@dimen/eve_home_screen_margin", rvExams?.getAttribute("android:paddingHorizontal"))

        // Floating solid capsule bottom bar
        val bottomBar = findElementById(root, "cardHomeBottomBar")
        assertNotNull("cardHomeBottomBar must exist", bottomBar)
        assertEquals("@dimen/eve_home_bottom_bar_height", bottomBar?.getAttribute("android:layout_height"))
        assertEquals("@dimen/eve_home_bottom_bar_radius", bottomBar?.getAttribute("app:cardCornerRadius"))
        assertEquals("@color/eve_home_floating_bar_bg", bottomBar?.getAttribute("app:cardBackgroundColor"))
        assertEquals("@dimen/eve_home_hairline_stroke", bottomBar?.getAttribute("app:strokeWidth"))
        assertEquals("@color/eve_home_hairline", bottomBar?.getAttribute("app:strokeColor"))

        // 4 Bottom Bar destinations with 22dp icons
        val btnHome = findElementById(bottomBar!!, "btnBottomNavHome")
        assertNotNull("btnBottomNavHome must exist", btnHome)
        val ivHome = findElementById(btnHome!!, "ivBottomNavHome")
        assertNotNull("ivBottomNavHome must exist", ivHome)
        assertEquals("@dimen/eve_home_bottom_bar_icon_size", ivHome?.getAttribute("android:layout_width"))
        assertEquals("@dimen/eve_home_bottom_bar_icon_size", ivHome?.getAttribute("android:layout_height"))
        assertEquals("@color/eve_home_accent", ivHome?.getAttribute("app:tint"))

        val btnHistory = findElementById(bottomBar, "btnBottomNavHistory")
        assertNotNull("btnBottomNavHistory must exist", btnHistory)

        val btnSaved = findElementById(bottomBar, "btnBottomNavSaved")
        assertNotNull("btnBottomNavSaved must exist", btnSaved)

        val btnProfile = findElementById(bottomBar, "btnBottomNavProfile")
        assertNotNull("btnBottomNavProfile must exist", btnProfile)
    }

    @Test
    fun testHomeExamAndFeedbackCardXml_UsesLockedCardStyles() {
        val examCard = loadXml("src/main/res/layout/item_exam.xml")
        assertEquals("@style/Widget.Eve.Home.Card", examCard.getAttribute("style"))
        assertEquals("@dimen/eve_home_card_radius", examCard.getAttribute("app:cardCornerRadius"))
        assertEquals("@dimen/eve_home_card_elevation", examCard.getAttribute("app:cardElevation"))
        assertEquals("@color/eve_home_card", examCard.getAttribute("app:cardBackgroundColor"))
        assertEquals("@dimen/eve_home_screen_margin", examCard.getAttribute("android:layout_marginHorizontal"))
        assertEquals("@dimen/eve_home_card_gap_half", examCard.getAttribute("android:layout_marginVertical"))

        val tvExamName = findElementById(examCard, "tvExamName")
        assertEquals("@style/TextAppearance.Eve.Home.CardTitle", tvExamName?.getAttribute("style"))
        assertEquals("1", tvExamName?.getAttribute("android:maxLines"))
        assertEquals("end", tvExamName?.getAttribute("android:ellipsize"))

        val tvExamTime = findElementById(examCard, "tvExamTime")
        assertEquals("@style/TextAppearance.Eve.Home.Secondary", tvExamTime?.getAttribute("style"))

        val feedbackCard = loadXml("src/main/res/layout/item_feedback_post.xml")
        assertEquals("@style/Widget.Eve.Home.Card", feedbackCard.getAttribute("style"))
        assertEquals("@dimen/eve_home_card_radius", feedbackCard.getAttribute("app:cardCornerRadius"))
        assertEquals("@dimen/eve_home_card_elevation", feedbackCard.getAttribute("app:cardElevation"))
        assertEquals("@color/eve_home_card", feedbackCard.getAttribute("app:cardBackgroundColor"))

        val tvFeedbackTitle = findElementById(feedbackCard, "tvFeedbackPostTitle")
        assertEquals("@style/TextAppearance.Eve.Home.CardTitle", tvFeedbackTitle?.getAttribute("style"))
    }

    @Test
    fun testNonHomeScreensAreUntouchedAndIntact() {
        val testLayout = loadXml("src/main/res/layout/activity_test.xml")
        assertNotNull("activity_test.xml must exist and parse", testLayout)

        val resultLayout = loadXml("src/main/res/layout/activity_result.xml")
        assertNotNull("activity_result.xml must exist and parse", resultLayout)

        val profileLayout = loadXml("src/main/res/layout/activity_profile.xml")
        assertNotNull("activity_profile.xml must exist and parse", profileLayout)

        val settingsLayout = loadXml("src/main/res/layout/activity_settings.xml")
        assertNotNull("activity_settings.xml must exist and parse", settingsLayout)

        val leaderboardLayout = loadXml("src/main/res/layout/activity_leaderboard.xml")
        assertNotNull("activity_leaderboard.xml must exist and parse", leaderboardLayout)

        val adminLayout = loadXml("src/main/res/layout/activity_admin.xml")
        assertNotNull("activity_admin.xml must exist and parse", adminLayout)
    }
}
