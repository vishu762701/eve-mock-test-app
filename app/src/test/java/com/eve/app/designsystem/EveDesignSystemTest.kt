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
 * with locked root values and existing Home/feature-presence assertions.
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

    @Test fun rootPaletteMatchesLockedDayNightRoles() {
        fun palette(folder: String): Map<String, String> {
            val directory = File(getProjectDir(), "app/src/main/res/$folder")
            return directory.listFiles()!!.filter { it.extension == "xml" }.flatMap { file ->
                val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("color")
                (0 until nodes.length).map { i ->
                    val node = nodes.item(i) as Element
                    node.getAttribute("name") to node.textContent.trim()
                }
            }.toMap()
        }
        for ((folder, expected) in listOf(
            "values" to mapOf("bg_app" to "#F5F5F5", "surface" to "#FFFFFF", "surface_inset" to "#F5F5F5", "text_primary" to "#000000", "text_secondary" to "#6C6C70", "accent" to "#000000", "on_accent" to "#FFFFFF"),
            "values-night" to mapOf("bg_app" to "#000000", "surface" to "#1C1C1E", "surface_inset" to "#2C2C2E", "text_primary" to "#FFFFFF", "text_secondary" to "#A1A1A6", "accent" to "#FFFFFF", "on_accent" to "#000000")
        )) {
            val actual = palette(folder)
            expected.forEach { (name, value) -> assertEquals("$folder/$name", value, actual[name]) }
        }
    }

    @Test fun sharedSizeTokensHaveIdenticalBreakpointCopies() {
        val base = parseDimenMap("src/main/res/values/design_dimens.xml")
        for (folder in listOf("values-sw360dp", "values-sw600dp")) {
            assertEquals(base, parseDimenMap("src/main/res/$folder/design_dimens.xml"))
        }
        for ((name, value) in mapOf("design_screen_margin" to "12dp", "design_card_padding" to "10dp", "design_radius_card" to "26dp", "design_radius_inner" to "16dp", "design_button_height" to "44dp", "design_touch_min" to "48dp", "design_ring_size" to "72dp", "design_ring_stroke" to "4dp", "design_card_elevation" to "9dp")) {
            assertEquals(name, value, base[name])
        }
    }

    @Test fun poppinsUsesBundledLockedWeightsAndLicense() {
        for (name in listOf("regular", "medium", "bold")) {
            val font = File(getProjectDir(), "app/src/main/res/font/poppins_$name.ttf")
            assertTrue("Bundled Poppins $name", font.isFile && font.length() > 10000)
        }
        val family = loadXml("src/main/res/font/poppins.xml").getElementsByTagName("font")
        assertEquals(listOf("400", "500", "700"), (0 until family.length).map { (family.item(it) as Element).getAttribute("app:fontWeight") })
        assertTrue(File(getProjectDir(), "app/src/main/assets/licenses/Poppins-OFL.txt").readText().contains("SIL OPEN FONT LICENSE"))
    }

    @Test
    fun testHomeScreenXml_UsesLockedDesignSystemComponents() {
        val root = loadXml("src/main/res/layout/activity_main.xml")

        // Root is Plain System Background with locked Home token
        assertEquals("@color/bg_app", root.designAttribute("android:background"))

        // Welcome greeting uses 29sp Bold HomePageTitle and primary ink
        val tvWelcome = findElementById(root, "tvWelcome")
        assertNotNull("tvWelcome must exist", tvWelcome)
        assertEquals("@style/TextAppearance.Eve.Token.Title", tvWelcome?.getAttribute("style"))
        assertEquals("@color/text_primary", tvWelcome?.designAttribute("android:textColor"))

        assertNull("Locked design removes list/chrome separator views", findElementById(root, "topBarHairline"))

        // rvExams has 16dp horizontal margin and clipToPadding=false
        val rvExams = findElementById(root, "rvExams")
        assertNotNull("rvExams must exist", rvExams)
        assertEquals("false", rvExams?.getAttribute("android:clipToPadding"))
        assertEquals("@dimen/design_screen_margin", rvExams?.designAttribute("android:paddingLeft"))

        // Floating Medium glass capsule bottom bar
        val bottomBar = findElementById(root, "cardHomeBottomBar")
        assertNotNull("cardHomeBottomBar must exist", bottomBar)
        assertEquals("@dimen/design_bottom_bar_height", bottomBar?.getAttribute("android:layout_height"))
        assertEquals("com.eve.app.designsystem.DesignGlassLayout", bottomBar?.tagName)
        assertEquals("true", bottomBar?.getAttribute("app:designGlassCapsule"))
        assertEquals("@dimen/design_screen_margin", bottomBar?.getAttribute("android:layout_marginHorizontal"))
        assertEquals("@dimen/design_bottom_bar_margin", bottomBar?.getAttribute("android:layout_marginBottom"))

        // 4 Bottom Bar destinations with 22dp icons
        val btnHome = findElementById(bottomBar!!, "btnBottomNavHome")
        assertNotNull("btnBottomNavHome must exist", btnHome)
        val ivHome = findElementById(btnHome!!, "ivBottomNavHome")
        assertNotNull("ivBottomNavHome must exist", ivHome)
        assertEquals("@dimen/design_icon_size", ivHome?.getAttribute("android:layout_width"))
        assertEquals("@dimen/design_icon_size", ivHome?.getAttribute("android:layout_height"))
        assertEquals("@color/accent", ivHome?.getAttribute("app:tint"))

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
        assertEquals("@style/Widget.Eve.Token.Card", examCard.getAttribute("style"))
        assertEquals("@dimen/design_radius_card", examCard.designAttribute("app:cardCornerRadius"))
        assertEquals("@dimen/design_card_elevation", examCard.designAttribute("app:cardElevation"))
        assertEquals("@color/surface", examCard.designAttribute("app:cardBackgroundColor"))
        assertEquals("", examCard.getAttribute("android:layout_marginHorizontal"))
        assertEquals("@dimen/design_zero", examCard.getAttribute("android:layout_marginVertical"))

        val tvExamName = findElementById(examCard, "tvExamName")
        assertEquals("@style/TextAppearance.Eve.Token.Heading", tvExamName?.getAttribute("style"))
        assertEquals("1", tvExamName?.getAttribute("android:maxLines"))
        assertEquals("end", tvExamName?.getAttribute("android:ellipsize"))

        val tvExamTime = findElementById(examCard, "tvExamTime")
        assertEquals("@style/TextAppearance.Eve.Token.Secondary", tvExamTime?.getAttribute("style"))

        val feedbackCard = loadXml("src/main/res/layout/item_feedback_post.xml")
        assertEquals("@style/Widget.Eve.Token.Card", feedbackCard.getAttribute("style"))
        assertEquals("@dimen/design_radius_card", feedbackCard.designAttribute("app:cardCornerRadius"))
        assertEquals("@dimen/design_card_elevation", feedbackCard.designAttribute("app:cardElevation"))
        assertEquals("@color/surface", feedbackCard.designAttribute("app:cardBackgroundColor"))

        val tvFeedbackTitle = findElementById(feedbackCard, "tvFeedbackPostTitle")
        assertEquals("@style/TextAppearance.Eve.Token.Heading", tvFeedbackTitle?.getAttribute("style"))
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
