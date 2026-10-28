package com.eve.app.designsystem

import com.eve.app.designsystem.designAttribute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
 * with semantic resource and existing Home/feature-presence assertions.
 */
class EveDesignSystemTest {

    private fun getProjectDir(): File {
        val candidates = listOf(File("."), File("app"), File(".."))
        for (candidate in candidates) {
            val resDir = File(candidate, "app/src/main/res/values")
            if (resDir.exists() && resDir.isDirectory) return candidate
            val directResDir = File(candidate, "src/main/res/values")
            if (directResDir.exists() && directResDir.isDirectory) {
                return candidate.canonicalFile.parentFile ?: File(".")
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

    /** Test semantic roles, not historic fixed hex values. */
    @Test fun semanticPaletteRolesAreValidInBothThemes() {
        for (folder in listOf("values", "values-night")) {
            val palette = parseColorMap("src/main/res/$folder/design_colors.xml")
            for (role in listOf("bg_app", "surface", "surface_inset", "text_primary",
                "text_secondary", "accent", "on_accent")) {
                val value = palette[role]
                assertNotNull("$folder/$role missing", value)
                assertTrue("$folder/$role invalid color: $value",
                    value!!.matches(Regex("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$")))
            }
        }
    }

    /** Screen-size qualifiers may intentionally diverge; tokens stay valid. */
    @Test fun responsiveDimensionsRemainValidAcrossBreakpoints() {
        val base = parseDimenMap("src/main/res/values/design_dimens.xml")
        for (name in listOf("design_screen_margin", "design_card_padding",
            "design_radius_card", "design_touch_min")) {
            assertNotNull("Missing shared token: $name", base[name])
        }
        for (folder in listOf("values", "values-sw360dp", "values-sw600dp")) {
            val dims = parseDimenMap("src/main/res/$folder/design_dimens.xml")
            for ((name, value) in dims) {
                assertTrue("$folder/$name invalid dimension: $value",
                    value.matches(Regex("^-?[0-9]+(\\.[0-9]+)?(dp|sp|px|pt|in|mm)$")))
            }
            val minimum = dims["design_touch_min"] ?: base["design_touch_min"]!!
            assertTrue("$folder touch target must be >=48dp",
                minimum.removeSuffix("dp").toFloat() >= 48f)
        }
    }

    @Test fun interUsesBundledLockedWeightsAndLicense() {
        for (name in listOf("regular", "medium", "semibold", "bold")) {
            val font = File(getProjectDir(), "app/src/main/res/font/inter_$name.ttf")
            assertTrue("Bundled Inter $name", font.isFile && font.length() > 10000)
        }
        val family = loadXml("src/main/res/font/inter.xml").getElementsByTagName("font")
        assertEquals(listOf("400", "500", "600", "700"), (0 until family.length).map { (family.item(it) as Element).getAttribute("app:fontWeight") })
        assertTrue(File(getProjectDir(), "app/src/main/assets/licenses/Inter-OFL.txt").readText().contains("SIL OPEN FONT LICENSE"))
    }

    @Test
    fun testHomeScreenXml_UsesLockedDesignSystemComponents() {
        val root = loadXml("src/main/res/layout/activity_main.xml")

        // Root is Plain System Background with locked Home token
        assertEquals("@color/bg_app", root.designAttribute("android:background"))

        // Source-traced Telegram ActionBar-derived 20sp / Roboto Medium title ink.
        val tvWelcome = findElementById(root, "tvWelcome")
        assertNotNull("tvWelcome must exist", tvWelcome)
        assertEquals("@style/TextAppearance.Eve.Token.Title", tvWelcome?.getAttribute("style"))
        assertEquals("@color/telegram_type_primary", tvWelcome?.designAttribute("android:textColor"))

        assertNull("Locked design removes list/chrome separator views", findElementById(root, "topBarHairline"))

        // rvExams has 16dp horizontal margin and clipToPadding=false
        val rvExams = findElementById(root, "rvExams")
        assertNotNull("rvExams must exist", rvExams)
        assertEquals("false", rvExams?.getAttribute("android:clipToPadding"))
        assertEquals("@dimen/design_screen_margin", rvExams?.designAttribute("android:paddingLeft"))

        // Home uses a decor-attached floating pill, not legacy embedded bottom buttons.
        assertNull(findElementById(root, "cardHomeBottomBar"))
        assertNull(findElementById(root, "btnBottomNavHistory"))
        assertNull(findElementById(root, "btnBottomNavSaved"))
        assertNull(findElementById(root, "layoutDrawerHistory"))
        assertNull(findElementById(root, "layoutDrawerSaved"))
        assertNull(findElementById(root, "layoutDrawerAbout"))
        listOf("layoutDrawerProfile", "layoutDrawerPremium", "layoutDrawerPerformance",
            "layoutDrawerLeaderboard", "layoutDrawerSyllabus", "layoutDrawerSettings")
            .forEach { assertNotNull("Missing drawer row: $it", findElementById(root, it)) }
        val navHost = findElementById(root, "homeContentHost")
        val overlay = findElementById(root, "homeNavOverlay")
        val drawer = findElementById(root, "navDrawerPanel")
        assertNotNull("Home content host must exist", navHost)
        assertNotNull("Floating navigation overlay must be inside content", overlay)
        assertTrue("Pill host must be within DrawerLayout content", overlay?.parentNode === navHost)
        assertTrue("Drawer panel must be sibling above content", drawer?.parentNode === root && navHost?.parentNode === root)
        for (id in listOf("tvDrawerUserName", "tvDrawerUserEmail", "tvDrawerPremiumStatus")) {
            val header = findElementById(root, id)
            assertEquals("match_parent", header?.designAttribute("android:layout_width"))
            assertEquals("end", header?.designAttribute("android:ellipsize"))
        }
    }



    /** Scope the owner's pure-black Night override to the Home drawer, not shared surfaces. */
    @Test
    fun homeDrawerBackgroundHasIsolatedBlackNightOverride() {
        val day = parseColorMap("src/main/res/values/design_colors.xml")
        val night = parseColorMap("src/main/res/values-night/design_colors.xml")
        assertEquals("@color/surface", day["home_drawer_background"])
        assertEquals("#FF000000", night["home_drawer_background"])
        assertEquals("#181819", night["surface"])
        val root = loadXml("src/main/res/layout/activity_main.xml")
        val panel = findElementById(root, "navDrawerPanel")
        assertEquals("@drawable/design_home_drawer_panel", panel?.designAttribute("android:background"))
        val shape = loadXml("src/main/res/drawable/design_home_drawer_panel.xml")
        val solid = shape.getElementsByTagName("solid").item(0) as Element
        assertEquals("@color/home_drawer_background", solid.getAttribute("android:color"))
        val source = File(getProjectDir(), "app/src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        assertTrue("Drawer runtime fill must use the dedicated owner role",
            source.contains("ContextCompat.getColor(this, R.color.home_drawer_background)"))
    }

    /** ChipGroup must never toggle the active Home category off on a repeat tap. */
    @Test
    fun homeCategoryRequiresExactlyOneSelectedChip() {
        val root = loadXml("src/main/res/layout/activity_main.xml")
        val group = findElementById(root, "chipGroupCategory")
        assertNotNull("Home category filter must exist", group)
        assertEquals("true", group?.designAttribute("app:singleSelection"))
        assertEquals("true", group?.designAttribute("app:selectionRequired"))
    }

    @Test
    fun homeOverflowKeepsAllSevenScrollableAccessibleRows() {
        val root = loadXml("src/main/res/layout/popup_telegram_menu.xml")
        val scroll = findElementById(root, "menuScroll")
        assertNotNull("Overflow must scroll inside capped viewport", scroll)
        assertEquals("vertical", scroll?.designAttribute("android:scrollbars"))
        assertEquals("insideOverlay", scroll?.designAttribute("android:scrollbarStyle"))
        for (id in listOf("cardTheme", "menuRowHistory", "menuRowBookmarks",
            "menuRowMistakes", "menuRowTopic", "menuRowPyq", "menuRowLogout")) {
            val row = findElementById(root, id)
            assertNotNull("Missing action $id", row)
            assertEquals("48dp", row?.designAttribute("android:minHeight"))
            assertEquals("wrap_content", row?.designAttribute("android:layout_height"))
        }
    }

    @Test
    fun testHomeExamAndFeedbackCardXml_UsesLockedCardStyles() {
        val examCard = loadXml("src/main/res/layout/item_exam.xml")
        assertEquals("@style/Widget.Eve.Token.Card", examCard.getAttribute("style"))
        assertEquals("@dimen/design_radius_card", examCard.designAttribute("app:cardCornerRadius"))
        assertEquals("@dimen/design_card_elevation", examCard.designAttribute("app:cardElevation"))
        assertEquals("@color/surface", examCard.designAttribute("app:cardBackgroundColor"))
        assertEquals("", examCard.getAttribute("android:layout_marginHorizontal"))
        assertEquals("@dimen/design_zero", examCard.getAttribute("android:layout_marginVertical"))

        val tvExamName = findElementById(examCard, "tvExamName")
        assertEquals("@style/TextAppearance.Eve.Telegram.ListTitle", tvExamName?.getAttribute("style"))
        assertEquals("2", tvExamName?.getAttribute("android:maxLines"))
        assertEquals("end", tvExamName?.getAttribute("android:ellipsize"))

        val tvExamTime = findElementById(examCard, "tvExamTime")
        assertEquals("@style/TextAppearance.Eve.Telegram.ListPreview", tvExamTime?.getAttribute("style"))

        val feedbackCard = loadXml("src/main/res/layout/item_feedback_post.xml")
        assertEquals("@style/Widget.Eve.Token.Card", feedbackCard.getAttribute("style"))
        assertEquals("@dimen/design_radius_card", feedbackCard.designAttribute("app:cardCornerRadius"))
        assertEquals("@dimen/design_card_elevation", feedbackCard.designAttribute("app:cardElevation"))
        assertEquals("@color/surface", feedbackCard.designAttribute("app:cardBackgroundColor"))

        val tvFeedbackTitle = findElementById(feedbackCard, "tvFeedbackPostTitle")
        assertEquals("@style/TextAppearance.Eve.Telegram.ListTitle", tvFeedbackTitle?.getAttribute("style"))
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
        val about = findElementById(settingsLayout, "rowAbout")
        assertNotNull("Settings must retain real About navigation", about)
        assertEquals("@dimen/design_bottom_bar_height", about?.designAttribute("android:minHeight"))

        val leaderboardLayout = loadXml("src/main/res/layout/activity_leaderboard.xml")
        assertNotNull("activity_leaderboard.xml must exist and parse", leaderboardLayout)

        val adminLayout = loadXml("src/main/res/layout/activity_admin.xml")
        assertNotNull("activity_admin.xml must exist and parse", adminLayout)
    }
}
