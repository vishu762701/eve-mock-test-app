package com.eve.app.designsystem

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Prevent regression where imported source icon names are registered but their
 * APK drawable density resources are omitted or replaced by stale vector art.
 * Exact source-image SHA-256 comparisons are enforced by sibling icon tests.
 */
class TelegramImportedIconAvailabilityTest {
    private fun locate(relative: String): File = listOf(
        File("../$relative"), File(relative)
    ).first { it.exists() }

    @Test fun everyImportedTelegramIconHasRealWebpDensitiesAndNoShadowVector() {
        val registry = locate("docs/telegram-icon-source-registry.tsv")
        val mainRes = locate("app/src/main/res")
        val records = registry.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("\t") }
            .drop(1).map { it.split('\t') }
        val imported = records.filter { it.size == 8 && it[7] == "OFFICIAL_TELEGRAM_GPL_ART_IMPORTED" }
        assertTrue("Telegram source imports lost", imported.size >= 16)
        for (entry in imported) {
            val name = entry[0]
            assertFalse("Conflicting old vector for $name", File(mainRes, "drawable/$name.xml").exists())
            for (density in listOf("mdpi", "hdpi", "xhdpi")) {
                val file = File(mainRes, "drawable-$density/$name.webp")
                assertTrue("$name missing $density source art", file.isFile)
                val bytes = file.readBytes()
                assertTrue("$name $density WebP too small", bytes.size >= 16)
                assertTrue("$name $density RIFF header invalid",
                    bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII) == "RIFF")
                assertTrue("$name $density WEBP header invalid",
                    bytes.copyOfRange(8, 12).toString(Charsets.US_ASCII) == "WEBP")
            }
        }
    }

    @Test fun sourceArtRetainsPublicLicenseNotices() {
        val license = locate("LICENSE").readText()
        val notices = locate("THIRD_PARTY_NOTICES.md").readText()
        assertTrue(license.contains("GNU GENERAL PUBLIC LICENSE"))
        assertTrue(license.contains("Version 3, 29 June 2007"))
        assertTrue(notices.contains("Telegram for Android"))
        assertTrue(notices.contains("GPL-2.0-or-later"))
    }
}
