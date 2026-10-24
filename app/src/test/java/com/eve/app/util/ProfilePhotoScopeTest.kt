package com.eve.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePhotoScopeTest {
    @Test fun customAvatarUsesDistinctOpaquePathsPerAccount() {
        val a = ProfilePhotoManager.scopedFileName("account-a")
        val b = ProfilePhotoManager.scopedFileName("account-b")
        assertNotEquals(a, b)
        assertTrue(a.endsWith(".jpg"))
        assertTrue(a.startsWith("profile_photo_u_v2_"))
        assertFalse(a.contains("account-a"))
        assertNotEquals("profile_photo.jpg", a)
    }
}
