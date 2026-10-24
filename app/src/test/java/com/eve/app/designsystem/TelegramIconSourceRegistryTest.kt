package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Registers every directly referenced EVE icon, without claiming Telegram art import. */
class TelegramIconSourceRegistryTest {
    private fun doc(name: String): String = listOf(
        File("../docs/$name"), File("docs/$name")
    ).first { it.isFile }.readText()

    @Test fun iconRegistryCoversEveryDirectLayoutReference() {
        val inventory = doc("telegram-full-layout-inventory.md")
        val registry = doc("telegram-icon-source-registry.tsv")
        val tick = 96.toChar()
        val xmlIcons = inventory.lineSequence()
            .filter { it.startsWith("| " + tick) && it.contains(".xml" + tick + " |") && it.contains("ic_") }
            .flatMap { line -> Regex("""\bic_[a-z0-9_]+\b""")
                .findAll(line).map { it.value }.asSequence() }
            .toSet()
        assertEquals(66, xmlIcons.size)
        val dynamicSources = listOf(
            "src/main/java/com/eve/app/designsystem/DesignIconStates.kt",
            "src/main/java/com/eve/app/ui/premium/PremiumBenefitRows.kt",
            "src/main/java/com/eve/app/util/ReminderScheduler.kt",
            "src/main/java/com/eve/app/ui/result/AnswerAdapter.kt",
            "src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt",
            "src/main/java/com/eve/app/designsystem/UiArcNumberField.kt",
            "src/main/java/com/eve/app/util/AppBulletin.kt",
            "src/main/java/com/eve/app/util/ThemeManager.kt",
            "src/main/java/com/eve/app/designsystem/UiArcMorphSelectView.kt",
            "src/main/java/com/eve/app/designsystem/UiArcSwipeRow.kt",
            "src/main/java/com/eve/app/util/NotificationHelper.kt"
        )
        val dynamicIcons = dynamicSources.flatMap { path ->
            val content = File(path).readText()
            Regex("""R\.drawable\.(ic_[a-z0-9_]+)""").findAll(content)
                .map { it.groupValues[1] }.toList()
        }.toSet()
        val expected = xmlIcons + dynamicIcons
        assertEquals(71, expected.size)
        val entries = registry.lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }.toList()
        assertEquals(8, entries.first().split('\t').size)
        val rows = entries.drop(1).map { it.split('\t') }
        assertTrue(rows.all { it.size == 8 })
        val ids = rows.map { it[0] }
        assertEquals(expected, ids.toSet())
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(2, rows.count { it[7] == "EVE_ORIGINAL_REDRAW_NOT_SOURCE_EXACT" })
        assertEquals(29, rows.count { it[7] == "OFFICIAL_TELEGRAM_GPL_ART_IMPORTED" })
        val sourceCandidates = mapOf(
            "ic_camera" to "ui/Components/ImageUpdater.java:278",
            "ic_copy" to "ui/Business/BusinessLinksActivity.java:395",
            "ic_download" to "ui/ChatActivity.java:10377",
            "ic_info" to "ui/ChatActivity.java:31892",
            "ic_link" to "ui/ChatActivity.java:40482",
            "ic_logout" to "ui/SettingsActivity.java:342",
            "ic_send" to "ui/Components/ShareAlert.java:2359"
        )
        val candidateRows = rows.filter { sourceCandidates.containsKey(it[0]) }
        assertEquals("Every ZIP-mapped candidate must be traceable", sourceCandidates.size, candidateRows.size)
        for (row in candidateRows) {
            assertEquals("Wrong source call-site for " + row[0], sourceCandidates[row[0]], row[2])
            assertTrue("Missing official Telegram density asset " + row[0],
                row[1].startsWith("res/drawable-hdpi/") && row[1].endsWith(".webp"))
            assertEquals("Original artwork must be tracked as GPL-derived", "OFFICIAL_TELEGRAM_GPL_ART_IMPORTED", row[7])
        }

        for (row in rows) {
            assertTrue("False Telegram import claim " + row[0],
                row[7] in setOf("UNMAPPED_PENDING_SOURCE_AUDIT", "CANDIDATE_NOT_IMPORTED", "SEMANTIC_MISMATCH",
                    "EVE_ORIGINAL_REDRAW_NOT_SOURCE_EXACT", "OFFICIAL_TELEGRAM_GPL_ART_IMPORTED"))
            assertTrue("Unverified source animation " + row[0],
                row[5].contains("UNKNOWN") || row[5].contains("NO_TELEGRAM"))
            assertTrue("Unverified exact themed art " + row[0], row[7] == "OFFICIAL_TELEGRAM_GPL_ART_IMPORTED" || row[6].contains("NOT_SOURCE_EXACT"))
        }
    }
}
