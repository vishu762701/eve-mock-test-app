package com.eve.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Verify the shipped binaries and redistribution notice, not a platform fallback. */
class FontManagerTest {
    private fun mainDir(): File = listOf(File("src/main"), File("app/src/main"))
        .first { it.isDirectory }

    @Test
    fun bundledPoppinsHasTheThreeLockedWeights() {
        for ((name, weight) in listOf("regular" to 400, "medium" to 500, "bold" to 700)) {
            val bytes = File(mainDir(), "res/font/poppins_$name.ttf").readBytes()
            val font = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            assertEquals("TrueType sfnt version", 0x00010000, font.getInt(0))
            val tableCount = font.getShort(4).toInt() and 0xffff
            val os2 = (0 until tableCount).map { 12 + it * 16 }
                .first { String(bytes, it, 4, Charsets.US_ASCII) == "OS/2" }
            val tableOffset = font.getInt(os2 + 8)
            assertEquals("Poppins $name weight", weight, font.getShort(tableOffset + 4).toInt() and 0xffff)
            assertTrue("Font identifies Poppins", String(bytes, Charsets.ISO_8859_1).contains("Poppins"))
        }
    }

    @Test
    fun licenseShipsWithTheFonts() {
        val license = File(mainDir(), "assets/licenses/Poppins-OFL.txt").readText()
        assertTrue(license.contains("Copyright 2020 The Poppins Project Authors"))
        assertTrue(license.contains("SIL OPEN FONT LICENSE Version 1.1"))
    }
}
