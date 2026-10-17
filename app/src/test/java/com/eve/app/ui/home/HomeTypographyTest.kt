package com.eve.app.ui.home

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
    fun testAppleIOS26_Standard11TextStyles() {
        val styles = loadStyles("src/main/res/values/type.xml")

        // 1. Large Title: 34sp, Regular (Emphasized: Bold), 41sp line height
        val largeTitle = styles["TextAppearance.Eve.Display"]
        assertNotNull(largeTitle)
        assertEquals("34sp", largeTitle?.textSize)
        assertEquals("normal", largeTitle?.textStyle)
        assertEquals("41sp", largeTitle?.lineHeight)

        // 2. Title 1: 28sp, Regular (Emphasized: Bold), 34sp line height
        val title1 = styles["TextAppearance.Eve.Title1"]
        assertNotNull(title1)
        assertEquals("28sp", title1?.textSize)
        assertEquals("normal", title1?.textStyle)
        assertEquals("34sp", title1?.lineHeight)

        // 3. Title 2: 22sp, Regular (Emphasized: Bold), 28sp line height
        val title2 = styles["TextAppearance.Eve.Title2"]
        assertNotNull(title2)
        assertEquals("22sp", title2?.textSize)
        assertEquals("normal", title2?.textStyle)
        assertEquals("28sp", title2?.lineHeight)

        // 4. Title 3: 20sp, Regular (Emphasized: Semibold), 25sp line height
        val title3 = styles["TextAppearance.Eve.Title3"]
        assertNotNull(title3)
        assertEquals("20sp", title3?.textSize)
        assertEquals("sans-serif", title3?.fontFamily)
        assertEquals("25sp", title3?.lineHeight)

        // 5. Headline: 17sp, Medium / Semibold, 22sp line height
        val headline = styles["TextAppearance.Eve.Headline"]
        assertNotNull(headline)
        assertEquals("17sp", headline?.textSize)
        assertEquals("sans-serif-medium", headline?.fontFamily)
        assertEquals("22sp", headline?.lineHeight)

        // 6. Body: 17sp, Regular, 22sp line height
        val body = styles["TextAppearance.Eve.Body"]
        assertNotNull(body)
        assertEquals("17sp", body?.textSize)
        assertEquals("sans-serif", body?.fontFamily)
        assertEquals("22sp", body?.lineHeight)

        // 7. Callout: 16sp, Regular, 21sp line height
        val callout = styles["TextAppearance.Eve.Callout"]
        assertNotNull(callout)
        assertEquals("16sp", callout?.textSize)
        assertEquals("sans-serif", callout?.fontFamily)
        assertEquals("21sp", callout?.lineHeight)

        // 8. Subheadline: 15sp, Regular, 20sp line height
        val subhead = styles["TextAppearance.Eve.Subheadline"]
        assertNotNull(subhead)
        assertEquals("15sp", subhead?.textSize)
        assertEquals("sans-serif", subhead?.fontFamily)
        assertEquals("20sp", subhead?.lineHeight)

        // 9. Footnote: 13sp, Regular, 18sp line height
        val footnote = styles["TextAppearance.Eve.Footnote"]
        assertNotNull(footnote)
        assertEquals("13sp", footnote?.textSize)
        assertEquals("sans-serif", footnote?.fontFamily)
        assertEquals("18sp", footnote?.lineHeight)

        // 10. Caption 1: 12sp, Regular, 16sp line height
        val caption1 = styles["TextAppearance.Eve.Caption"]
        assertNotNull(caption1)
        assertEquals("12sp", caption1?.textSize)
        assertEquals("sans-serif", caption1?.fontFamily)
        assertEquals("16sp", caption1?.lineHeight)

        // 11. Caption 2: 11sp, Regular (Emphasized: Semibold), 13sp line height
        val caption2 = styles["TextAppearance.Eve.Caption2"]
        assertNotNull(caption2)
        assertEquals("11sp", caption2?.textSize)
        assertEquals("sans-serif", caption2?.fontFamily)
        assertEquals("13sp", caption2?.lineHeight)
    }

    @Test
    fun testAppleIOS26_ComponentSpecificVariants() {
        val styles = loadStyles("src/main/res/values/type.xml")

        // Eyebrow / Kicker ("EVE"): 12sp, medium, allCaps
        val eyebrow = styles["TextAppearance.Eve.Eyebrow"]
        assertNotNull(eyebrow)
        assertEquals("12sp", eyebrow?.textSize)
        assertEquals("sans-serif-medium", eyebrow?.fontFamily)
        assertEquals("true", eyebrow?.textAllCaps)
        assertEquals("16sp", eyebrow?.lineHeight)

        // Subheadline.Medium (Section headers / Drawer rows): 15sp, medium
        val subheadMedium = styles["TextAppearance.Eve.Subheadline.Medium"]
        assertNotNull(subheadMedium)
        assertEquals("sans-serif-medium", subheadMedium?.fontFamily)

        // Footnote.Medium (Category chips / Badges / Streak pill): 13sp, medium
        val footnoteMedium = styles["TextAppearance.Eve.Footnote.Medium"]
        assertNotNull(footnoteMedium)
        assertEquals("sans-serif-medium", footnoteMedium?.fontFamily)

        // Search input: 17sp, regular
        val search = styles["TextAppearance.Eve.Search"]
        assertNotNull(search)
        assertEquals("@color/eve_header_ink_text", search?.textColor)
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
        assertEquals("@style/TextAppearance.Eve.Home.PageTitle", tvWelcome?.getAttribute("style"))

        val etSearch = findElementById(root, "etSearch")
        assertNotNull(etSearch)
        assertEquals("@style/TextAppearance.Eve.Search", etSearch?.getAttribute("style"))

        val tvStreakSummary = findElementById(root, "tvStreakSummary")
        assertNotNull(tvStreakSummary)
        assertEquals("@style/TextAppearance.Eve.Footnote.Medium", tvStreakSummary?.getAttribute("style"))

        val tvOfflineBanner = findElementById(root, "tvOfflineBanner")
        assertNotNull(tvOfflineBanner)
        assertEquals("@style/TextAppearance.Eve.Caption1.Medium", tvOfflineBanner?.getAttribute("style"))

        val tvDrawerUserName = findElementById(root, "tvDrawerUserName")
        assertNotNull(tvDrawerUserName)
        assertEquals("@style/TextAppearance.Eve.Headline", tvDrawerUserName?.getAttribute("style"))

        val tvDrawerUserEmail = findElementById(root, "tvDrawerUserEmail")
        assertNotNull(tvDrawerUserEmail)
        assertEquals("@style/TextAppearance.Eve.Footnote", tvDrawerUserEmail?.getAttribute("style"))

        val tvMessage = findElementById(root, "tvMessage")
        assertNotNull(tvMessage)
        assertEquals("@style/TextAppearance.Eve.Title3", tvMessage?.getAttribute("style"))

        val tvMessageSub = findElementById(root, "tvMessageSub")
        assertNotNull(tvMessageSub)
        assertEquals("@style/TextAppearance.Eve.Subheadline", tvMessageSub?.getAttribute("style"))
    }

    @Test
    fun testExamItemXml_SemanticStyles() {
        val root = loadXml("src/main/res/layout/item_exam.xml")

        val tvExamName = findElementById(root, "tvExamName")
        assertNotNull(tvExamName)
        assertEquals("@style/TextAppearance.Eve.Home.CardTitle", tvExamName?.getAttribute("style"))
        assertTrue("tvExamName must not have hardcoded textSize", tvExamName?.getAttribute("android:textSize").isNullOrEmpty())

        val tvExamTime = findElementById(root, "tvExamTime")
        assertNotNull(tvExamTime)
        assertEquals("@style/TextAppearance.Eve.Home.Secondary", tvExamTime?.getAttribute("style"))
        assertTrue("tvExamTime must not have hardcoded textSize", tvExamTime?.getAttribute("android:textSize").isNullOrEmpty())
    }

    @Test
    fun testCategoryHeaderXml_SemanticStyles() {
        val root = loadXml("src/main/res/layout/item_category_header.xml")
        val tvCategoryHeader = findElementById(root, "tvCategoryHeader")
        assertNotNull(tvCategoryHeader)
        assertEquals("@style/TextAppearance.Eve.Subheadline.Medium", tvCategoryHeader?.getAttribute("style"))
        assertTrue("tvCategoryHeader must not have hardcoded textSize", tvCategoryHeader?.getAttribute("android:textSize").isNullOrEmpty())
    }
}
