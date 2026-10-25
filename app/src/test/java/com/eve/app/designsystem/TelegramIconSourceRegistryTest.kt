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
                if (expected != actual) {
                    System.err.println("Source icon digest mismatch: $asset expected=$expected actual=$actual")
                }
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


    @Test fun approvedMediumOriginalsRemainPinned() {
        val root = listOf(File(".."), File("."))
            .first { File(it, "docs/telegram-icon-source-registry.tsv").isFile }
        val lines = File(root, "docs/telegram-icon-source-registry.tsv").readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
        val columns = lines.first().split('\t')
        val registry = lines.drop(1).map { columns.zip(it.split('\t')).toMap() }
        val pinned = mapOf(
            "ic_broadcast" to ("res/drawable-hdpi/msg_channel.webp" to listOf("fd61bf8389bfe7951e359806ff85319c33a89416db8f52c53142035838aa3b56", "d812248c45c9436e150c9afb0d9be15ec2d20924b35e7307dee47b35270cc219", "3d166a373927296d29d13ffbcbbd926288273cbac11a89de6ea6f1a86608ec2a", "b66a756c5934877578e2e12cdabbc2722c01a8a3d2e04b03fb9060b887b365be")),
            "ic_chevron_down" to ("res/drawable-hdpi/msg_go_down.webp" to listOf("c78cf2d8be63a9276738700d75b13403f82976118b14e76010b6fc81443bcb8d", "f6951cf210c2546de4cdf208a6b9229fe87bdfd837021151e4d5a0e7438fc251", "8a3bb721aafb8223fca9923b973d1f6ebde487bf5e35afb84217d31efd716225", "5ab35b8c006881f5206c379d93dce7f48961575cb87854ea8e1d4d25e99ab22a")),
            "ic_pin" to ("res/drawable-hdpi/msg_pin.webp" to listOf("e8ef8bb01e95108ac9b7e38d15b933ca760a750b4d3b257e31692d978d788538", "aab11c0a64a29595738c1d4c8816b824f1994405470f3c34287fa573dd06bc1e", "d72e938b173f5f7094d84609b029d67a74c693a7a3c398885fec448027a80828", "f8fd0b6a5726647172d339c6e2d53a9f3b353eb905d2ac48d95b38bbe4bf3f8d")),
            "ic_robot" to ("res/drawable-hdpi/msg_bot.webp" to listOf("16f5bad176bb0733d411df93537fb2342f01176d460021db9d95b36821de1ea7", "505e55410ac99dfcf47d91e67f10f3ae5b9b3750b8cb66180a6421cbfaec04fd", "adc620e50df78f06dbade917a3a87c2488065876425ae7f556429411d1b665e4", "f21773ed549c7500c2c2577a2d25d034fb40e8bbf9d8c09bc36b7333afa1a545")),
            "ic_theme_sun" to ("res/drawable-hdpi/msg_brightness_high.webp" to listOf("37d188da5604ea934ea9db2cc5be03226fc81d62d182287faa198fddb254dbd8", "dbaaeddb4d1b2f8e9bebeba4f50f6c89ea870b084a14beef0e9f174680d01091", "7c01f92403026865eef6594a8ba41bf4048600f395b19a4a327d5202cf2f99c3", "f8805950ba92591beec9baeaaf85389a4c0961f660942af5af224a59ee38023a")),
            "ic_upload" to ("res/drawable-hdpi/msg_mini_upload.webp" to listOf("eb784a0ac917a1e6d9d5aaef1133e1f916e567ad3c9bba02a36e08ad54cc3c2b", "a3264c25708d46cd703e5bb005a33620f10cb558780ece40c4bba9d4b683dbb8", "48d0594e036162f2f0bb98a1d1f204da7f355cc009a845c51bfde53311c2ef7a", "ec6e203c3b358377426c1e2723746ea437ba490de32dca6791e3350a06ade794")),
            "ic_star_filled" to ("res/drawable-hdpi/ic_rating_star_filled.webp" to listOf("99c36bd064b14508dbb93424c0fafc01a8965cdb4da6296bc2fbc421870dc341", "01e40b2cd170a9ee68f6bcd1e26e392332e775a1d1708d5a0e9954dda4e6c1fd", "501faa12fe3373819c34f437aea76cddac88659daf6a1d250c7e0ea46b6e1157", "7eebdb0a87e27a85c48973048017953ebb1d9f66939f714d34c87228b9774869")),
            "ic_star_outline" to ("res/drawable-hdpi/ic_rating_star.webp" to listOf("34cefcee9d35cafd0ba8ec44200ff49bc41afd327f2f6db29151fe19b025229c", "b91738560fc3cd4cd54dfc11a48e6a3b06b444d57d00ce7019bd67eac249be25", "43720dab3945de7aab28755d0b24cb3d1d93c8a43b3b9cb7601b0677024b4269", "83f7161abddae21134666e0253b9b3972f2716266485d31b4b2cc4f5beb7caac")),
            "ic_minus" to ("res/drawable-hdpi/msg_remove.webp" to listOf("d546c82a1fd40dea69bc105461a56df93a62d1c36eed72210b06fa71e4531035", "9f5896049eda1f265bd6f83362ff1ba342c22b357a1f40837c316788da72a409", "3a57b6078a9d581d82c3cfd5e3b13d370ca755819a028662a360b61af101fa07", "23e8ded3ad33d89d7d5c27a8dbd2f4820491d046e257e387da27be3da386d893"))
        )
        for ((icon, source) in pinned) {
            val row = registry.single { it.getValue("eve_icon") == icon }
            assertEquals("Wrong Telegram counterpart for $icon", source.first, row.getValue("telegram_zip_asset"))
            assertEquals("Not imported: $icon", "OFFICIAL_TELEGRAM_GPL_ART_IMPORTED",
                row.getValue("source_fidelity_status"))
            for ((index, density) in listOf("mdpi", "hdpi", "xhdpi", "xxhdpi").withIndex()) {
                assertEquals("$icon/$density does not match original bytes",
                    source.second[index], row.getValue("sha256_$density"))
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
