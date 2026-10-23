package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class TelegramSettingsBookmarkSourceIconsTest {
    private val sourceSha = mapOf(
        "ic_settings" to mapOf("mdpi" to "2925dc314bb1f10136ac78876200582920fecf040b11522b2f5ea4720059cfcd", "hdpi" to "4c9fe16609aa3630d06321a76e06430690489ca031ccdc43acab18818fb32a83", "xhdpi" to "e735a8870d541b8f2345cae5b52edce665ade5f9b7c8a6f76e9f33bdb9f379e8", "xxhdpi" to "529c7688d9fba136d955208bdc57afc0ad36bed1619055a210f09c141ec738cb"),
        "ic_bookmark" to mapOf("mdpi" to "873ad24c7b1951de855a069a66e5e0f63500e24cbf8c3a4ab1099a182ae29f73", "hdpi" to "7453f638e2a396bfdaa04910f1a7cb6eb304d88cc11d44b9f8bd8de4f9dcac06", "xhdpi" to "09b8b18ba5f5f161945483ea13aec029bc4d47dc271e330720cdff3f397b4c2c", "xxhdpi" to "457066fd5447cd0389503b422a1eecee520cd17cbfd5b2c111d4a8c5f9b763c0")
    )
    @Test fun exactSourceArtworkForSettingsAndSavedDestination() {
        val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        for ((icon,densities) in sourceSha) {
            assertTrue("Retired vector should be deleted: $icon",!File(res,"drawable/$icon.xml").exists())
            for ((density,expected) in densities) {
                val file=File(res,"drawable-$density/$icon.webp")
                assertTrue("Official icon absent: $file",file.isFile && file.length()>0)
                val actual=MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                assertEquals("ZIP source mismatch: $file",expected,actual)
            }
        }
    }
}
