package com.eve.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminIdentityGuardTest {
    @Test fun freshAuthorizationAllowsOnlySameSignedInIdentity() {
        assertTrue(AdminIdentityGuard.allowed(true, "firebase-uid", "admin@example.com",
            "firebase-uid", "ADMIN@example.com"))
        assertFalse(AdminIdentityGuard.allowed(false, "firebase-uid", "admin@example.com",
            "firebase-uid", "admin@example.com"))
        assertFalse(AdminIdentityGuard.allowed(true, "firebase-uid", "admin@example.com",
            "other-uid", "admin@example.com"))
        assertFalse(AdminIdentityGuard.allowed(true, "firebase-uid", "admin@example.com",
            "firebase-uid", "imposter@example.com"))
        assertFalse(AdminIdentityGuard.allowed(true, "firebase-uid", "admin@example.com",
            null, null))
        assertFalse(AdminIdentityGuard.allowed(true, "", "admin@example.com", "", "admin@example.com"))
    }
}
