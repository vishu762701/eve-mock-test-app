package com.eve.app.ui.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class QuestionTitleHierarchyMission11Test {
    @Test fun questionGetsAHeadingRoleAndBoldTypeface() {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/layout/item_question.xml"))
        val views = document.getElementsByTagName("TextView")
        val question = (0 until views.length).map { views.item(it) as Element }
            .first { it.getAttribute("android:id") == "@+id/tvQuestion" }
        assertEquals("@style/TextAppearance.Eve.Token.Heading", question.getAttribute("style"))
        assertEquals("start", question.getAttribute("android:gravity"))
        val code = File("src/main/java/com/eve/app/ui/test/QuestionAdapter.kt").readText()
        assertTrue(code.contains("b.tvQuestion.typeface = com.eve.app.util.FontManager.boldTypeface("))
        assertTrue(code.contains("b.tvQuestion.text = questionText"))
        assertTrue("Option radios must not be promoted into question headings",
            !code.contains("b.rbA.typeface = com.eve.app.util.FontManager.boldTypeface("))
    }

    @Test fun testTitleIsBoldWithoutChangingTimerOrOtherButtons() {
        val code = File("src/main/java/com/eve/app/ui/test/TestActivity.kt").readText()
        assertTrue(code.contains("binding.tvTestTitle.typeface = com.eve.app.util.FontManager.boldTypeface(this)"))
        val xml = File("src/main/res/layout/activity_test.xml").readText()
        assertTrue(xml.contains("android:id=\"@+id/circularTimerView\""))
    }
}
