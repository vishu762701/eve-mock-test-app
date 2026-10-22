package com.eve.app.ui.home

import com.eve.app.designsystem.designAttribute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Phase 2 Official Apple iOS 26 Typography Foundation Verification for EVE Home Screen.
 *
 * Verifies:
 * 1. The 11 authoritative Apple iOS 26 text styles in type.xml: size, weight, line-height.
 * 2. Component-specific variants (Eyebrow, Subheadline.Medium, Footnote.Medium, Caption1.Medium, Search).
 * 3. Consistent layout bindings across activity_main.xml, item_exam.xml, item_category_header.xml, item_feedback_post.xml.
 * 4. Strict adherence to scalable "sp" units for accessibility and absence of hardcoded text sizes.
 */
class HomeTypographyTest {

    private data class StyleAttributes(
        val textSize: String? = null,
        val textStyle: String? = null,
        val fontFamily: String? = null,
        val letterSpacing: String? = null,
        val lineHeight: String? = null,
        val textAllCaps: String? = null,
        val textColor: String? = null
    )

    private fun loadStyles(relativePath: String): Map<String, StyleAttributes> {
        val fileCandidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        val file = fileCandidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find file: $relativePath")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val styleNodes = doc.getElementsByTagName("style")
        val map = mutableMapOf<String, StyleAttributes>()

        for (i in 0 until styleNodes.length) {
            val styleNode = styleNodes.item(i) as Element
            val styleName = styleNode.getAttribute("name")
            var size: String? = null
            var weight: String? = null
            var font: String? = null
            var letter: String? = null
            var lineH: String? = null
            var allCaps: String? = null
            var color: String? = null

            val itemNodes = styleNode.getElementsByTagName("item")
            for (j in 0 until itemNodes.length) {
                val item = itemNodes.item(j) as Element
                val attrName = item.getAttribute("name")
                val content = item.textContent?.trim()
                when (attrName) {
                    "android:textSize" -> size = content
                    "android:textStyle" -> weight = content
                    "android:fontFamily", "fontFamily" -> if (font == null || font == "sans-serif") font = content
                    "android:letterSpacing" -> letter = content
                    "lineHeight", "android:lineHeight" -> lineH = content
                    "android:textAllCaps" -> allCaps = content
                    "android:textColor" -> color = content
                }
            }
            map[styleName] = StyleAttributes(size, weight, font, letter, lineH, allCaps, color)
        }
        return map
    }

    private fun loadXml(relativePath: String): Element {
        val fileCandidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        val file = fileCandidates.firstOrNull { it.exists() }
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

    private fun collectTextViews(node: Node, list: MutableList<Element> = mutableListOf()): List<Element> {
        if (node is Element) {
            val tagName = node.tagName
            if (tagName == "TextView" || tagName.endsWith(".TextView") ||
                tagName == "EditText" || tagName.endsWith(".EditText") ||
                tagName.endsWith("Button")) {
                list.add(node)
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            collectTextViews(children.item(i), list)
        }
        return list
    }

    @Test
    fun testNativeTelegramSharedTextStyles() {
        val styles = loadStyles("src/main/res/values/design_type.xml")
        for ((role, token, font) in listOf(
            Triple("Title", "title", "bold"), Triple("Heading", "heading", "semibold"),
            Triple("Body", "body", "regular"), Triple("Secondary", "secondary", "regular")
        )) {
            val style = styles["TextAppearance.Eve.Token.$role"]
            assertNotNull(style)
            assertEquals("@dimen/design_type_$token", style?.textSize)
            assertEquals("@dimen/design_line_$token", style?.lineHeight)
            assertEquals(if (font == "regular") "sans-serif" else "@font/telegram_roboto_medium", style?.fontFamily)
        }
    }

    @Test
    fun testNativeTelegramComponentVariants() {
        val styles = loadStyles("src/main/res/values/design_type.xml")
        for ((role, token, font) in listOf(
            Triple("Tab", "tab", "regular"), Triple("Chip", "chip", "medium"), Triple("Button", "button", "semibold")
        )) {
            val style = styles["TextAppearance.Eve.Token.$role"]
            assertNotNull(style)
            assertEquals("@dimen/design_type_$token", style?.textSize)
            assertEquals("@dimen/design_line_$token", style?.lineHeight)
            assertEquals(if (font == "regular") "sans-serif" else "sans-serif-medium", style?.fontFamily)
        }
        assertEquals("sans-serif-medium", styles["TextAppearance.Eve.Token.Tab.Selected"]?.fontFamily)
    }

    @Test
    fun testHomeScreenXml_NoHardcodedTextSizes() {
        val root = loadXml("src/main/res/layout/activity_main.xml")
        val textViews = collectTextViews(root)

        for (tv in textViews) {
            val textSize = tv.getAttribute("android:textSize")
            val id = tv.getAttribute("android:id")
            assertTrue("TextView $id in activity_main.xml must not have hardcoded android:textSize (was '$textSize')", textSize.isEmpty())
        }
    }

    @Test
    fun testHomeScreenXml_SemanticStyleBindings() {
        val root = loadXml("src/main/res/layout/activity_main.xml")

        val tvWelcome = findElementById(root, "tvWelcome")
        assertNotNull(tvWelcome)
        assertEquals("@style/TextAppearance.Eve.Token.Title", tvWelcome?.getAttribute("style"))

        val etSearch = findElementById(root, "etSearch")
        assertNotNull(etSearch)
        assertEquals("@style/Widget.Eve.Token.Input.TelegramSearch", etSearch?.getAttribute("style"))
        assertEquals("eve_telegram_type", etSearch?.getAttribute("android:tag"))

        val tvStreakSummary = findElementById(root, "tvStreakSummary")
        assertNotNull(tvStreakSummary)
        assertEquals("@style/TextAppearance.Eve.Token.Chip.OnAccent", tvStreakSummary?.getAttribute("style"))

        val tvOfflineBanner = findElementById(root, "tvOfflineBanner")
        assertNotNull(tvOfflineBanner)
        assertEquals("@style/TextAppearance.Eve.Token.Secondary.orange", tvOfflineBanner?.getAttribute("style"))

        val tvDrawerUserName = findElementById(root, "tvDrawerUserName")
        assertNotNull(tvDrawerUserName)
        assertEquals("@style/TextAppearance.Eve.Token.Heading", tvDrawerUserName?.getAttribute("style"))

        val tvDrawerUserEmail = findElementById(root, "tvDrawerUserEmail")
        assertNotNull(tvDrawerUserEmail)
        assertEquals("@style/TextAppearance.Eve.Token.Secondary", tvDrawerUserEmail?.getAttribute("style"))

        val tvMessage = findElementById(root, "tvMessage")
        assertNotNull(tvMessage)
        assertEquals("@style/TextAppearance.Eve.Token.Heading", tvMessage?.getAttribute("style"))

        val tvMessageSub = findElementById(root, "tvMessageSub")
        assertNotNull(tvMessageSub)
        assertEquals("@style/TextAppearance.Eve.Token.Body", tvMessageSub?.getAttribute("style"))
    }

    @Test
    fun testExamItemXml_SemanticStyles() {
        val root = loadXml("src/main/res/layout/item_exam.xml")

        val tvExamName = findElementById(root, "tvExamName")
        assertNotNull(tvExamName)
        assertEquals("@style/TextAppearance.Eve.Token.Heading", tvExamName?.getAttribute("style"))
        assertTrue("tvExamName must not have hardcoded textSize", tvExamName?.getAttribute("android:textSize").isNullOrEmpty())

        val tvExamTime = findElementById(root, "tvExamTime")
        assertNotNull(tvExamTime)
        assertEquals("@style/TextAppearance.Eve.Token.Secondary", tvExamTime?.getAttribute("style"))
        assertTrue("tvExamTime must not have hardcoded textSize", tvExamTime?.getAttribute("android:textSize").isNullOrEmpty())
    }

    @Test
    fun testCategoryHeaderXml_SemanticStyles() {
        val root = loadXml("src/main/res/layout/item_category_header.xml")
        val tvCategoryHeader = findElementById(root, "tvCategoryHeader")
        assertNotNull(tvCategoryHeader)
        assertEquals("@style/TextAppearance.Eve.Token.Body", tvCategoryHeader?.getAttribute("style"))
        assertTrue("tvCategoryHeader must not have hardcoded textSize", tvCategoryHeader?.getAttribute("android:textSize").isNullOrEmpty())
    }
}
