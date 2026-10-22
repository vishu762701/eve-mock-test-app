package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Protects the honesty of the Telegram mission audit. This does not assert that all
 * styles or icons are migrated; it prevents silent omission of an existing XML layout.
 */
class TelegramInventoryCoverageTest {
    private fun layoutDirectory(): File = listOf(
        File("src/main/res/layout"),
        File("app/src/main/res/layout"),
        File("../app/src/main/res/layout")
    ).first { it.isDirectory }

    private fun inventoryFile(): File = listOf(
        File("../docs/telegram-full-layout-inventory.md"),
        File("docs/telegram-full-layout-inventory.md")
    ).first { it.isFile }

    @Test fun everyXmlScreenAndRowIsAccountedFor() {
        val layouts = layoutDirectory().listFiles { file -> file.extension == "xml" }!!
            .map { it.name }.toSet()
        assertTrue("Expected production UI inventory, got " + layouts.size, layouts.size >= 95)
        val doc = inventoryFile().readText()
        val recorded = doc.lineSequence()
            .filter { it.startsWith("| `") && it.contains(".xml` |") }
            .map { it.substringAfter("`").substringBefore("`") }
            .toSet()
        assertEquals("Add a source mapping and inventory row for each new layout", layouts, recorded)
    }

    @Test fun fontBranchDeviationMustRemainDisclosed() {
        val doc = inventoryFile().readText()
        assertTrue(doc.contains("SharedConfig.java:619"))
        assertTrue(doc.contains("default **false**"))
        assertTrue(doc.contains("source-exact bundled 500 font"))
    }
}
