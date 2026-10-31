package com.eve.app.ui.profile

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class ProfileIdentityLayoutTest {
    @Test fun visibleIdentityIsUsernameWithoutDeletingLegacyBackendIdentifiers() {
        val xml = File("src/main/res/layout/activity_profile.xml").readText()
        assertFalse(xml.contains("cellEveId")); assertFalse(xml.contains("Copy EVE ID"))
        assertTrue(xml.contains("cellUsername")); assertTrue(xml.contains("Personal QR code")); assertTrue(xml.contains("btnProfileMenu"))
        val status = File("src/main/java/com/eve/app/ui/profile/ProfilePresenceStatus.kt").readText()
        assertFalse(status.contains("Eve ID:"))
        assertTrue(File("src/main/java/com/eve/app/data/remote/EveApiService.kt").readText().contains("val eveId: String? = null"))
        assertTrue(File("src/main/java/com/eve/app/ui/profile/UsernameCardActivity.kt").readText().contains("does not verify account ownership"))
    }
    @Test fun qrGlyphMatchesAuthenticatedPinnedTelegramBytes() {
        val bytes = File("src/main/res/drawable/telegram_profile_qr.xml").readBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals("ddaffe85034df6a6d52ff14f8dd9d307127d9486859146015bc66267c89c1255", digest)
        assertTrue(File("../docs/sources/profile-mission/source-map.json").readText().contains(digest))
    }
}
