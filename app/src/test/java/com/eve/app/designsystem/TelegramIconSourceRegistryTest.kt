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
        val expected = inventory.lineSequence()
            .filter { it.startsWith("| " + tick) && it.contains(".xml" + tick + " |") && it.contains("ic_") }
            .flatMap { line -> Regex("""\bic_[a-z0-9_]+\b""")
                .findAll(line).map { it.value }.asSequence() }
            .toSet()
        assertEquals(66, expected.size)
        val entries = registry.lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }.toList()
        assertEquals(8, entries.first().split('\t').size)
        val rows = entries.drop(1).map { it.split('\t') }
        assertTrue(rows.all { it.size == 8 })
        val ids = rows.map { it[0] }
        assertEquals(expected, ids.toSet())
        assertEquals(ids.size, ids.toSet().size)
        for (row in rows) {
            assertTrue("False Telegram import claim " + row[0],
                row[7] in setOf("EVE_UNCHANGED_NO_EXACT", "CANDIDATE_NOT_IMPORTED", "SEMANTIC_MISMATCH"))
            assertTrue("Unverified source animation " + row[0],
                row[5].contains("UNKNOWN") || row[5].contains("NO_TELEGRAM"))
            assertTrue("Unverified exact themed art " + row[0], row[6].contains("NOT_SOURCE_EXACT"))
        }
    }
}
