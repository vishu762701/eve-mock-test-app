package com.eve.app.util

import com.eve.app.data.repository.FloatingLinkRepository
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
}
