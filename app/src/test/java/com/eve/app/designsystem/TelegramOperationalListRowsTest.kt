package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Telegram DialogCell (DialogCell.java:1252-1255, Theme.java:7812,7897)
 * maps 17sp/16sp/12sp EVE list roles without changing payment/status/exam semantics.
 */
class TelegramOperationalListRowsTest {
    private val expected = mapOf(
        "item_feedback_message.xml" to mapOf("tvSenderName" to "ListTitle", "tvSenderEmail" to "ListPreview",
            "tvMessageBody" to "ListPreview", "tvTimestamp" to "ListTime"),
        "item_post_reply.xml" to mapOf("tvReplyAuthor" to "ListTitle", "tvReplyText" to "ListPreview", "tvReplyTime" to "ListTime"),
        "item_sent_broadcast.xml" to mapOf("tvBroadcastTitle" to "ListTitle",
            "tvBroadcastMessage" to "ListPreview", "tvBroadcastTime" to "ListTime"),
        "item_admin_premium_user.xml" to mapOf("tvUserDisplayName" to "ListTitle", "tvUserEmail" to "ListPreview"),
        "item_admin_email.xml" to mapOf("tvEmail" to "ListTitle"),
        "item_audit_log.xml" to mapOf("tvDescription" to "ListTitle",
            "tvAdminEmail" to "ListPreview", "tvTimestamp" to "ListTime"),
        "item_exam_test.xml" to mapOf("tvTitle" to "ListTitle", "tvSubtitle" to "ListPreview"),
        "item_generated_test.xml" to mapOf("tvExamName" to "ListTitle", "tvTestDetails" to "ListPreview"),
        "item_admin_transaction.xml" to mapOf("tvUserEmail" to "ListPreview", "tvOrderDate" to "ListTime")
    )

    @Test fun rowTextUsesVerifiedTelegramRolesAndProtectsLegacyOverrides() {
        val base = listOf(File("src/main/res/layout"), File("app/src/main/res/layout")).first { it.isDirectory }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val androidNs = "http://schemas.android.com/apk/res/android"
        for ((filename, roles) in expected) {
            val root = factory.newDocumentBuilder().parse(File(base, filename)).documentElement
            val nodes = root.getElementsByTagName("TextView")
            val byId = (0 until nodes.length).map { nodes.item(it) as Element }.associateBy {
                it.getAttributeNS(androidNs, "id").substringAfterLast("/")
            }
            for ((id, role) in roles) {
                val view = byId[id] ?: error("Missing $filename#$id")
                assertEquals("$filename#$id typography", "@style/TextAppearance.Eve.Telegram.$role",
                    view.getAttribute("style"))
                assertEquals("$filename#$id must not be overridden by legacy typography",
                    "eve_telegram_type", view.getAttributeNS(androidNs, "tag"))
            }
        }
    }

    @Test fun adminStateAndExamControlsAreUntouched() {
        val base = listOf(File("src/main/res/layout"), File("app/src/main/res/layout")).first { it.isDirectory }
        val xml = File(base, "item_admin_transaction.xml").readText()
        assertTrue(xml.contains("android:id=\"@+id/tvOrderAmount\""))
        assertTrue(xml.contains("android:id=\"@+id/tvOrderStatus\""))
        val sent = File(base, "item_sent_broadcast.xml").readText()
        assertTrue(sent.contains("android:id=\"@+id/tvBroadcastTarget\""))
        val test = File(base, "item_exam_test.xml").readText()
        assertTrue(test.contains("android:id=\"@+id/tvStatus\""))
    }
}
