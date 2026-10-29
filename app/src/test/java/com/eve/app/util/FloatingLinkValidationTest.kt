package com.eve.app.util

import com.eve.app.data.repository.FloatingLinkRepository
import com.eve.app.data.repository.FloatingLinkData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingLinkValidationTest {

    @Test
    fun testValidHttpsUrls() {
        assertTrue(FloatingLinkRepository.isValidHttpsUrl("https://t.me/eve_community"))
        assertTrue(FloatingLinkRepository.isValidHttpsUrl("https://example.com/path?query=1&b=2"))
        assertTrue(FloatingLinkRepository.isValidHttpsUrl("https://sub.domain.org:8443/chat"))
    }

    @Test
    fun testRejectsInsecureOrForbiddenSchemes() {
        assertFalse(FloatingLinkRepository.isValidHttpsUrl("http://insecure.com"))
        assertFalse(FloatingLinkRepository.isValidHttpsUrl("javascript:alert(1)"))
        assertFalse(FloatingLinkRepository.isValidHttpsUrl("intent://#Intent;scheme=android;end"))
        assertFalse(FloatingLinkRepository.isValidHttpsUrl("file:///android_asset/something"))
        assertFalse(FloatingLinkRepository.isValidHttpsUrl("ftp://ftp.server.com"))
    }

    @Test
    fun testRejectsEmbeddedCredentials() {
        assertFalse(FloatingLinkRepository.isValidHttpsUrl("https://admin:password@evil.com/leak"))
        assertFalse(FloatingLinkRepository.isValidHttpsUrl("https://user@legit.com/page"))
    }

    @Test
    fun testRejectsExcessiveLength() {
        val longUrl = "https://example.com/" + "a".repeat(500)
        assertFalse(FloatingLinkRepository.isValidHttpsUrl(longUrl))
    }

    @Test
    fun testRejectsEmptyOrBlank() {
        assertFalse(FloatingLinkRepository.isValidHttpsUrl(""))
        assertFalse(FloatingLinkRepository.isValidHttpsUrl("   "))
    }

    @Test
    fun testFloatingLinkDataModel() {
        val data = FloatingLinkData(
            url = "https://t.me/eve_community",
            x = 0.85f,
            y = 0.90f
        )
        assertEquals("https://t.me/eve_community", data.url)
        assertEquals(0.85f, data.x ?: 0f, 0.001f)
        assertEquals(0.90f, data.y ?: 0f, 0.001f)
    }

    @Test
    fun testNormalizedCoordinatesClamping() {
        val clampNorm = { v: Float -> v.coerceIn(0f, 1f) }
        assertEquals(0f, clampNorm(-0.5f), 0.001f)
        assertEquals(1f, clampNorm(1.5f), 0.001f)
        assertEquals(0.5f, clampNorm(0.5f), 0.001f)
    }

    @Test
    fun testViewportCoordinateMapping() {
        val parentW = 1080
        val parentH = 2400
        val cardW = 180
        val cardH = 180
        val marginPx = 48

        val minX = marginPx.toFloat()
        val maxX = (parentW - cardW - marginPx).toFloat()
        val minY = marginPx.toFloat()
        val maxY = (parentH - cardH - marginPx).toFloat()

        fun mapCoord(norm: Float, min: Float, max: Float) = min + norm.coerceIn(0f, 1f) * (max - min)

        assertEquals(minX, mapCoord(0f, minX, maxX), 0.001f)
        assertEquals(maxX, mapCoord(1f, minX, maxX), 0.001f)
        assertEquals((minX + maxX) / 2f, mapCoord(0.5f, minX, maxX), 0.001f)
    }
}
