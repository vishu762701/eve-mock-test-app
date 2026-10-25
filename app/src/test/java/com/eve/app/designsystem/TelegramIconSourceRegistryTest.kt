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


    @Test fun highConfidenceImportsArePinnedToOriginalCoreArchive() {
        // Independently hashed against the attached core ZIP manifest.
        val root = listOf(File(".."), File("."))
            .first { File(it, "docs/telegram-icon-source-registry.tsv").isFile }
        val lines = File(root, "docs/telegram-icon-source-registry.tsv").readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
        val columns = lines.first().split('\t')
        val rows = lines.drop(1).map { columns.zip(it.split('\t')).toMap() }
        val pinned = mapOf(
            "ic_email" to ("res/drawable-hdpi/msg2_email.webp" to listOf("75a90559d5e1d1261fd0436a678bf38a44dfe26d02ea42bcdc39a60c51796dcc", "f73b3297abb9eaff2b740fd9843f3656bef49be0af43809d9b43d3b4309c0965", "384ba61fd5788964cd32c45cee4b23a8841e8c99155f80ba633e63a7ffab8ca7", "0803aedbfb09ae3b878cc899083aacb349c87b90f682d63c837794adbde89020")),
            "ic_phone" to ("res/drawable-hdpi/msg_calls.webp" to listOf("f581971063d2cbc10616a7ee48494671a642a275444a5fc6d73e843812e48c44", "591bd13035bc3d98dd23c61ed5f922d39252eed0d73eaff8a0521e960f3e40b7", "e92cd6ca808c95601f6a3949b846fdba9921920ef29a186823816b3f221c6062", "b655d03e9af28614cde3b7a1f334e549e17df557c539e774970f4622dce6a03a")),
            "ic_refresh" to ("res/drawable-hdpi/msg_retry.webp" to listOf("0a5c22342a6ab90e077764f77fe5a28bca2f2f1ef3bf3df1b9eb39a55e4411a7", "bf8fbd38d54b9e46e1fcb8c901630f605b212ceb15ed261810af467637730ec4", "f40e4ee301e0e055596c48fdbef429adff7f5435a296fda3d13bca2e0816dbe4", "5a50f380b40e7e2e8e17ed2697b6336e046ab167461587308c7cf0c4b9a78fea")),
            "ic_chevron_right" to ("res/drawable-hdpi/msg_arrowright.webp" to listOf("6d2b7477dc80eb6863827a023e15c6493270d6fd3417ed5f16cdc3256ad7d234", "6502f564b74f42a59d63c71b5c52d026656ed8534821346697d78bbfdf1203f5", "fcdf18618b729ec6fcb9b8e0e2b5d361befeb2ab70856e3d2e0221102712b8c3", "4fb8db09614888ff6a2c747966afaf02fe2009dba9146b1f623c999d403b00c2")),
            "ic_history_rounded" to ("res/drawable-hdpi/msg_recent.webp" to listOf("6197b1132671816f12ed39cfbaba44c93388979d98e48f80d43f7a68db98822e", "f14ecda8de3585862da42c5d3100b65dc3839d403032ceef26ae7cd1442216fa", "80b9cc3c9687832b7ae6ad2d49bdf0706795b58b97416d87ee5a208d45c108b8", "a80b5f882566991a9d8ff3c13e5dc695c7cd16f5eb354db70d1706b43fe0d666"))
        )
        for ((icon, original) in pinned) {
            val row = rows.single { it.getValue("eve_icon") == icon }
            assertEquals("Expected exact Telegram import for $icon",
                "OFFICIAL_TELEGRAM_GPL_ART_IMPORTED", row.getValue("source_fidelity_status"))
            assertEquals("Wrong original counterpart for $icon",
                original.first, row.getValue("telegram_zip_asset"))
            for ((index, density) in listOf("mdpi", "hdpi", "xhdpi", "xxhdpi").withIndex()) {
                assertEquals("$icon/$density source SHA-256 changed",
                    original.second[index], row.getValue("sha256_$density"))
            }
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
