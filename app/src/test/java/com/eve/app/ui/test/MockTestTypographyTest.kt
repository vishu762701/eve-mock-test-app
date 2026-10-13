package com.eve.app.ui.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Phase 4 Official Apple iOS 26 Typography Verification for Mock Test Screen.
 *
 * Verifies:
 * 1. Zero hardcoded text sizes (android:textSize) across activity_test.xml,
 *    item_question.xml, and item_palette_circle.xml.
 * 2. Proper semantic typography style bindings according to Apple iOS 26 hierarchy:
 *    - Exam Title: Headline (17sp semibold)
 *    - Question Text: Callout (16sp regular, 1.5 lineSpacingMultiplier)
 *    - Question Timer Chip: Footnote.Medium (13sp medium)
 *    - Option Choices (A-D): Subheadline (14sp regular)
 *    - Palette Numbers: Caption1.Medium (12sp medium)
 *    - Bottom Action Buttons: Widget.Eve.ApprovedTestAction (13sp medium)
 *    - Bottom Navigation Buttons: Widget.Eve.ApprovedTestNavigation (14sp medium)
 * 3. Preservation of all test surface invariants:
 *    - questionScroll container
 *    - 1.5 line spacing multiplier
 *    - No BlurView
 *    - No btnLanguage in test header
 *    - Approved action and navigation widget styles
 */
class MockTestTypographyTest {

    private fun loadXml(relativePath: String): Element {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
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

    private fun collectAllElementsWithAttribute(node: Node, attrName: String, list: MutableList<Element> = mutableListOf()): List<Element> {
        if (node is Element) {
            if (node.hasAttribute(attrName)) {
                list.add(node)
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            collectAllElementsWithAttribute(children.item(i), attrName, list)
        }
        return list
    }

    @Test
    fun testZeroHardcodedTextSizesInMockTestLayouts() {
        val layoutFiles = listOf(
            "src/main/res/layout/activity_test.xml",
            "src/main/res/layout/item_question.xml",
            "src/main/res/layout/item_palette_circle.xml"
        )

        for (filePath in layoutFiles) {
            val root = loadXml(filePath)
            val elementsWithTextSize = collectAllElementsWithAttribute(root, "android:textSize")
            val violatingIds = elementsWithTextSize.map { el ->
                el.getAttribute("android:id").ifEmpty { "<${el.tagName}>" }
            }
            assertTrue(
                "Layout $filePath must contain 0 hardcoded android:textSize occurrences, but found: $violatingIds",
                elementsWithTextSize.isEmpty()
            )
        }
    }

    @Test
    fun testActivityTestTypographyBindings() {
        val root = loadXml("src/main/res/layout/activity_test.xml")

        val tvTestTitle = findElementById(root, "tvTestTitle")
        assertNotNull("tvTestTitle must exist", tvTestTitle)
        assertEquals("@style/TextAppearance.Eve.Test.Title", tvTestTitle?.getAttribute("style"))

        val tvOfflineBanner = findElementById(root, "tvOfflineBanner")
        assertNotNull("tvOfflineBanner must exist", tvOfflineBanner)
        assertEquals("@style/TextAppearance.Eve.Test.OfflineBanner", tvOfflineBanner?.getAttribute("style"))

        val tvMessage = findElementById(root, "tvMessage")
        assertNotNull("tvMessage must exist", tvMessage)
        assertEquals("@style/TextAppearance.Eve.Test.Message", tvMessage?.getAttribute("style"))

        val tvMessageSub = findElementById(root, "tvMessageSub")
        assertNotNull("tvMessageSub must exist", tvMessageSub)
        assertEquals("@style/TextAppearance.Eve.Test.MessageSub", tvMessageSub?.getAttribute("style"))

        val btnClear = findElementById(root, "btnClear")
        assertNotNull("btnClear must exist", btnClear)
        assertEquals("@style/Widget.Eve.ApprovedTestAction", btnClear?.getAttribute("style"))

        val btnMarkReview = findElementById(root, "btnMarkReview")
        assertNotNull("btnMarkReview must exist", btnMarkReview)
        assertEquals("@style/Widget.Eve.ApprovedTestAction", btnMarkReview?.getAttribute("style"))

        val btnPrev = findElementById(root, "btnPrev")
        assertNotNull("btnPrev must exist", btnPrev)
        assertEquals("@style/Widget.Eve.ApprovedTestNavigation", btnPrev?.getAttribute("style"))

        val btnNext = findElementById(root, "btnNext")
        assertNotNull("btnNext must exist", btnNext)
        assertEquals("@style/Widget.Eve.ApprovedTestNext", btnNext?.getAttribute("style"))
    }

    @Test
    fun testItemQuestionTypographyBindings() {
        val root = loadXml("src/main/res/layout/item_question.xml")

        val tvQuestion = findElementById(root, "tvQuestion")
        assertNotNull("tvQuestion must exist", tvQuestion)
        assertEquals("@style/TextAppearance.Eve.QuestionText", tvQuestion?.getAttribute("style"))
        assertEquals("1.5", tvQuestion?.getAttribute("android:lineSpacingMultiplier"))

        val tvQuestionTimer = findElementById(root, "tvQuestionTimer")
        assertNotNull("tvQuestionTimer must exist", tvQuestionTimer)
        assertEquals("@style/TextAppearance.Eve.Test.QuestionTimer", tvQuestionTimer?.getAttribute("style"))

        for (optionId in listOf("rbA", "rbB", "rbC", "rbD")) {
            val rb = findElementById(root, optionId)
            assertNotNull("$optionId must exist", rb)
            assertEquals(
                "$optionId must use TextAppearance.Eve.Test.Option",
                "@style/TextAppearance.Eve.Test.Option",
                rb?.getAttribute("style")
            )
        }
    }

    @Test
    fun testItemPaletteCircleTypographyBinding() {
        val root = loadXml("src/main/res/layout/item_palette_circle.xml")

        val tvCircleNumber = findElementById(root, "tvCircleNumber")
        assertNotNull("tvCircleNumber must exist", tvCircleNumber)
        assertEquals("@style/TextAppearance.Eve.Test.PaletteNumber", tvCircleNumber?.getAttribute("style"))
    }

    @Test
    fun testMockTestInvariantsPreserved() {
        val candidates = listOf(File("src/main/res"), File("app/src/main/res"))
        val res = candidates.first { it.isDirectory }
        val testContent = File(res, "layout/activity_test.xml").readText()
        val questionContent = File(res, "layout/item_question.xml").readText()

        assertFalse("activity_test.xml must not contain BlurView", testContent.contains("BlurView"))
        assertFalse("item_question.xml must not contain BlurView", questionContent.contains("BlurView"))
        assertFalse("btnLanguage must not exist in test header", testContent.contains("android:id=\"@+id/btnLanguage\""))
        assertTrue("questionScroll must exist", questionContent.contains("@+id/questionScroll"))
        assertTrue("android:lineSpacingMultiplier=\"1.5\" must remain", questionContent.contains("android:lineSpacingMultiplier=\"1.5\""))
        assertTrue("Widget.Eve.ApprovedTestAction must remain", testContent.contains("Widget.Eve.ApprovedTestAction"))
        assertTrue("Widget.Eve.ApprovedTestNavigation must remain", testContent.contains("Widget.Eve.ApprovedTestNavigation"))
        assertTrue("Widget.Eve.ApprovedTestNext must remain", testContent.contains("Widget.Eve.ApprovedTestNext"))
    }
}
