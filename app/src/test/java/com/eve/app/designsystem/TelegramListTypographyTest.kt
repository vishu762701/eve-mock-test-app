package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TelegramListTypographyTest {
    private fun read(p: String) = File("src/main/res/" + p).readText()

    @Test fun sourceListRolesHaveVerifiedTypography() {
        val xml = read("values/telegram_typography.xml")
        for ((role, sp, family) in listOf(
            Triple("ListTitle", 17, "@font/telegram_roboto_medium"),
            Triple("ListPreview", 16, "sans-serif"),
            Triple("ListTime", 12, "sans-serif")
        )) {
            val start = xml.indexOf("<style name=\"TextAppearance.Eve.Telegram." + role + "\"")
            assertTrue("Missing " + role, start >= 0)
            val end = xml.indexOf("</style>", start)
            val section = xml.substring(start, end)
            assertTrue("Wrong text size " + role, section.contains(">" + sp + "sp</item>"))
            assertTrue("Wrong family " + role, section.contains("<item name=\"fontFamily\">" + family + "</item>"))
            assertTrue(section.contains("@color/telegram_type_"))
        }
        assertTrue(read("values/telegram_colors.xml").contains("telegram_type_secondary"))
        assertTrue(read("values-night/telegram_colors.xml").contains("telegram_type_secondary"))
    }

    @Test fun examHistoryFeedbackAndAdapterUseSharedRoles() {
        val cases = mapOf(
            "item_exam.xml" to mapOf("tvExamName" to "ListTitle", "tvExamTime" to "ListPreview"),
            "item_history.xml" to mapOf("tvExamName" to "ListTitle", "tvMeta" to "ListTime"),
            "item_feedback_post.xml" to mapOf("tvFeedbackPostTitle" to "ListTitle",
                "tvFeedbackPostMessage" to "ListPreview", "tvFeedbackPostDate" to "ListTime"),
            "item_existing_exam_admin.xml" to mapOf("tvExamName" to "ListTitle")
        )
        for ((filename, views) in cases) {
            val xml = read("layout/" + filename)
            for ((id, role) in views) {
                val line = xml.lineSequence().first { it.contains("android:id=\"@+id/" + id + "\"") }
                assertTrue("Style mismatch in " + filename + " for " + id,
                    line.contains("style=\"@style/TextAppearance.Eve.Telegram." + role + "\""))
                assertTrue("Missing protected tag " + filename + " " + id,
                    line.contains("android:tag=\"eve_telegram_type\""))
            }
        }
        val binder = File("src/main/java/com/eve/app/ui/home/ExamAdapter.kt").readText()
        for (role in listOf("ListTitle", "ListPreview", "ListTime")) {
            assertTrue("Adapter reverts to old typography " + role,
                binder.contains("R.style.TextAppearance_Eve_Telegram_" + role))
        }
        assertTrue(read("layout/item_exam.xml").contains("android:maxLines=\"2\""))
    }
}
