package com.eve.app.designsystem

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

/**
 * Phase 1 Parity Unit Test.
 * Loads docs/parity/manifest.json and verifies that every symbol in TelegramParity
 * equals the authoritative extracted Telegram constant value.
 */
class TelegramParityTest {

    @Test
    fun verifyAllParitySymbolsMatchExtractedManifest() {
        val manifestFile = listOf(
            File("docs/parity/manifest.json"),
            File("../docs/parity/manifest.json"),
            File("../../docs/parity/manifest.json")
        ).firstOrNull { it.exists() }

        assertNotNull("docs/parity/manifest.json must exist", manifestFile)
        val jsonText = manifestFile!!.readText()
        val jsonArray = JSONArray(jsonText)

        val clazz = TelegramParity::class.java

        for (i in 0 until jsonArray.length()) {
            val entry = jsonArray.getJSONObject(i)
            val symbol = entry.getString("eve_symbol")
            val kind = entry.optString("kind", "")

            val field = clazz.getDeclaredField(symbol)
            field.isAccessible = true
            val actualValue = field.get(null)

            when (kind) {
                "ms" -> {
                    val expected = entry.getLong("value")
                    assertEquals("Mismatch for $symbol", expected, (actualValue as Number).toLong())
                }
                "alpha" -> {
                    val expected = entry.getDouble("value").toFloat()
                    assertEquals("Mismatch for $symbol", expected, (actualValue as Number).toFloat(), 0.001f)
                }
                "ratio" -> {
                    val expected = entry.getDouble("value")
                    if (actualValue is Int) {
                        assertEquals("Mismatch for $symbol", expected.toInt(), actualValue)
                    } else {
                        assertEquals("Mismatch for $symbol", expected.toFloat(), (actualValue as Number).toFloat(), 0.001f)
                    }
                }
                else -> {
                    val expected = entry.getInt("value")
                    assertEquals("Mismatch for $symbol", expected, (actualValue as Number).toInt())
                }
            }
        }
    }
}
