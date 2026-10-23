package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class TelegramMoreSourceExactIconsTest {
    private val sourceSha = mapOf(
        "ic_close" to mapOf("mdpi" to "a1e61bda529d8b1bda80f5ac1b9c699654df2b41929d32fa58cf251a589b4254", "hdpi" to "9032086b82f1e3749684623dac292437b1103d62584c9bf5d2bc64a22aeddb09", "xhdpi" to "dcfe1e98da128ca402a9effd97e3c39078d0f73bdd06246f68db91ec0d343c21", "xxhdpi" to "ffa64614089ddf3e99cfb91c0b4c2e3db9685ea9ecd119e85fe5a3adc40c51ee"),
        "ic_share" to mapOf("mdpi" to "41a6ccb0401fbb20bb820a113130ceec9661ec7062909ddd77e87e6a1cb5f15c", "hdpi" to "53da4ea3b3592d7cb16264e312d39da1a07faecb5d496a2863499eee9d1f490e", "xhdpi" to "7544455a2adb66462452fc722604765a9de02811f4ba5e4a6d2860e878220287", "xxhdpi" to "cdd2ac1d504a374132d60a377f0cd78c5e9d262a711a9f4cbb9b5e8b59e6b51a"),
        "ic_edit" to mapOf("mdpi" to "6a0b85bf5c849f5ece7c2bf3934e9455f5fb188d1093ce5b103e4b6b661fd9a3", "hdpi" to "0b491ccfd63d77cfa65aab470105223498b5b4637f192d9a90258a3570ba2f82", "xhdpi" to "acdfea3ba1aa9b46a843749550e00c59d7535f94ce86e8f4868540ebc69178cb", "xxhdpi" to "578a46c3af456c027491e8069a1ef97e0ec4027ccfcf72157c4593617e86ef53")
    )
    @Test fun imageAssetsMatchOfficialZipExactly() {
        val res = listOf(File("src/main/res"),File("app/src/main/res")).first { it.isDirectory }
        for ((icon, densities) in sourceSha) {
            assertTrue("Old vector must not shadow $icon",!File(res,"drawable/$icon.xml").exists())
            for ((density,digest) in densities) {
                val file=File(res,"drawable-$density/$icon.webp")
                assertTrue("Missing original density asset $file",file.isFile && file.length()>0)
                val actual=MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                assertEquals("Not the audited ZIP bytes: $file",digest,actual)
            }
        }
    }
}
