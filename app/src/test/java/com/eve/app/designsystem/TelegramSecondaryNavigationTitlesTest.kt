package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Maintains source ActionBar title text metrics on secondary screens. */
class TelegramSecondaryNavigationTitlesTest {
    private val staticTitles = mapOf(
        "activity_activity_log.xml" to "Activity & Audit Log",
        "activity_bookmarks.xml" to "Bookmarked Questions",
        "activity_edit_about.xml" to "Manage App Content",
        "activity_feedback_messages.xml" to "Feedback Messages",
        "activity_flagged_questions.xml" to "Flagged Questions Inbox",
        "activity_payment_checkout.xml" to "Payment Checkout",
        "activity_mistakes.xml" to "Mistake Notebook",
        "activity_practice.xml" to "Topic-wise Practice",
        "activity_pyq.xml" to "Previous Year Questions"
    )
    private val runtimeTitles = mapOf(
        "activity_content_display.xml" to "tvHeaderTitle",
        "activity_exam_tests.xml" to "tvTitle"
    )

    @Test fun secondaryScreenActionBarTextIsSourceMatchedWithoutChangingDataText() {
        val dir = listOf(File("src/main/res/layout"), File("app/src/main/res/layout")).first { it.isDirectory }
        val f = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val ns = "http://schemas.android.com/apk/res/android"
        for (filename in staticTitles.keys + runtimeTitles.keys) {
            val doc = f.newDocumentBuilder().parse(File(dir, filename))
            val nodes = doc.getElementsByTagName("TextView")
            val selected = (0 until nodes.length).map { nodes.item(it) as Element }
                .filter { v -> staticTitles[filename]?.let { it == v.getAttributeNS(ns, "text") }
                    ?: (runtimeTitles[filename] == v.getAttributeNS(ns, "id").substringAfterLast("/")) }
            assertEquals("$filename navigation count", 1, selected.size)
            assertEquals("$filename Telegram navigation typography",
                "@style/TextAppearance.Eve.Telegram.NavigationTitle", selected.single().getAttribute("style"))
            assertEquals("$filename legacy overrides protected",
                "eve_telegram_type", selected.single().getAttributeNS(ns, "tag"))
        }
    }
}
