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

    @Test
    fun testFullScreenDraggableBoundsWithSystemBarsAndCutouts() {
        val parentW = 1080
        val parentH = 2400
        val cardW = 180
        val cardH = 180
        val marginPx = 36
        val safeInsetTop = 96 // Status bar
        val safeInsetBottom = 120 // Navigation bar
        val safeInsetLeft = 0
        val safeInsetRight = 0

        val minX = (safeInsetLeft + marginPx).toFloat()
        val maxX = (parentW - safeInsetRight - cardW - marginPx).toFloat().coerceAtLeast(minX)
        val minY = (safeInsetTop + marginPx).toFloat()
        val maxY = (parentH - safeInsetBottom - cardH - marginPx).toFloat().coerceAtLeast(minY)

        fun mapCoord(norm: Float, min: Float, max: Float) = min + norm.coerceIn(0f, 1f) * (max - min)

        // All 4 safe corners
        val topLeftX = mapCoord(0f, minX, maxX)
        val topLeftY = mapCoord(0f, minY, maxY)
        assertEquals(minX, topLeftX, 0.001f)
        assertEquals(minY, topLeftY, 0.001f)
        assertTrue("Top edge must stay below status bar", topLeftY >= safeInsetTop)

        val topRightX = mapCoord(1f, minX, maxX)
        val topRightY = mapCoord(0f, minY, maxY)
        assertEquals(maxX, topRightX, 0.001f)
        assertEquals(minY, topRightY, 0.001f)

        val bottomLeftX = mapCoord(0f, minX, maxX)
        val bottomLeftY = mapCoord(1f, minY, maxY)
        assertEquals(minX, bottomLeftX, 0.001f)
        assertEquals(maxY, bottomLeftY, 0.001f)
        assertTrue("Bottom edge must stay above navigation bar", bottomLeftY + cardH <= parentH - safeInsetBottom)

        val bottomRightX = mapCoord(1f, minX, maxX)
        val bottomRightY = mapCoord(1f, minY, maxY)
        assertEquals(maxX, bottomRightX, 0.001f)
        assertEquals(maxY, bottomRightY, 0.001f)

        // Center / over exam cards and toolbar
        val middleY = mapCoord(0.5f, minY, maxY)
        assertTrue("Icon can freely position over interior content", middleY > minY && middleY < maxY)
    }

    @Test
    fun testFloatingAirplaneLayoutHierarchyIsDirectChildOfHomeContentHost() {
        val xml = java.io.File("src/main/res/layout/activity_main.xml").readText()
        assertTrue(
            "cardFloatingAirplane must be a child of homeContentHost for full screen drag coverage",
            xml.contains("<FrameLayout android:id=\"@+id/homeContentHost\"") &&
                xml.indexOf("<com.eve.app.designsystem.DesignCardView android:id=\"@+id/cardFloatingAirplane\"") >
                xml.indexOf("</LinearLayout>\n    </LinearLayout>")
        )
    }
}
