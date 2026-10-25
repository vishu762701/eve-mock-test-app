package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Reads the committed registry and bundled res files; never reads or fetches reference ZIPs. */
class TelegramIconSourceRegistryTest {
    @Test fun bundledIconsMatchRegistrySha256() {
        val root = listOf(File(".."), File("."))
            .first { File(it, "docs/telegram-icon-source-registry.tsv").isFile }
        val records = File(root, "docs/telegram-icon-source-registry.tsv").readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
        val header = records.first().split('\t')
        assertEquals("Duplicate registry columns", header.size, header.toSet().size)
        val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi")
        val required = listOf("eve_icon", "telegram_zip_asset", "source_fidelity_status") +
            densities.map { "sha256_$it" }
        assertTrue("Missing registry columns", header.containsAll(required))
        val rows = records.drop(1).map { line ->
            val values = line.split('\t')
            assertEquals("Malformed registry row", header.size, values.size)
            header.zip(values).toMap()
        }
        assertEquals("Duplicate icon IDs", rows.size, rows.map { it.getValue("eve_icon") }.toSet().size)
        val imported = rows.filter { it.getValue("source_fidelity_status") == "OFFICIAL_TELEGRAM_GPL_ART_IMPORTED" }
        assertTrue("Registry must contain imported assets", imported.isNotEmpty())
        val resources = File(root, "app/src/main/res")
        for (row in imported) {
            val icon = row.getValue("eve_icon")
            assertTrue("Invalid icon ID: $icon", icon.matches(Regex("[a-z][a-z0-9_]*")))
            assertTrue("Missing official source path for $icon", row.getValue("telegram_zip_asset").startsWith("res/"))
            assertFalse("Conflicting vector for $icon", File(resources, "drawable/$icon.xml").exists())
            val expectedPaths = mutableSetOf<String>()
            for (density in densities) {
                val asset = File(resources, "drawable-$density/$icon.webp")
                val expected = row.getValue("sha256_$density")
                if (expected == "NOT FOUND") {
                    assertFalse("Unexpected density without upstream source: $asset", asset.exists())
                    continue
                }
                assertTrue("Invalid SHA-256 for $icon/$density", expected.matches(Regex("[0-9a-f]{64}")))
                assertTrue("Missing source asset: $asset", asset.isFile)
                val actual = MessageDigest.getInstance("SHA-256").digest(asset.readBytes())
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                assertEquals("$asset SHA-256 mismatch", expected, actual)
                expectedPaths.add(asset.relativeTo(resources).invariantSeparatorsPath)
            }
            assertTrue("No hashed density for $icon", expectedPaths.isNotEmpty())
            val actualPaths = resources.listFiles()!!.filter { it.isDirectory && it.name.startsWith("drawable") }
                .flatMap { directory -> directory.listFiles()!!.filter { it.nameWithoutExtension == icon } }
                .map { it.relativeTo(resources).invariantSeparatorsPath }.toSet()
            assertEquals("Unregistered or missing density for $icon", expectedPaths, actualPaths)
        }
    }

    @Test fun homeNotificationFallbackUsesPinnedTelegramBell() {
        val root = listOf(File(".."), File("."))
            .first { File(it, "docs/telegram-icon-source-registry.tsv").isFile }
        val registry = File(root, "docs/telegram-icon-source-registry.tsv").readLines()
            .first { it.startsWith("ic_bell\t") }
        assertTrue(registry.contains("OFFICIAL_TELEGRAM_GPL_ART_IMPORTED"))
        assertTrue(registry.contains("res/drawable-hdpi/msg_notifications.webp"))
        val home = File(root, "app/src/main/res/layout/activity_main.xml").readText()
        assertTrue(home.contains("android:id=\"@+id/ivNotificationIdle\""))
        val fallback = Regex("<ImageView[^>]*android:id=\"@\\+id/ivNotificationIdle\"[^>]*/>")
            .find(home)?.value ?: throw AssertionError("Home bell fallback missing")
        assertTrue("Use the already hash-verified source bell", fallback.contains("android:src=\"@drawable/ic_bell\""))
        assertTrue("Preserve existing Lottie", home.contains("app:lottie_rawRes=\"@raw/notification_bell\""))
    }
}
