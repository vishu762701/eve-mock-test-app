package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSaveConfirmationTest {
    @Test fun matchingTextCannotConfirmAnotherAccountsSave() {
        assertFalse(ProfileSaveConfirmation.matches("alice", "bob", true, "OBC", "OBC", true))
        assertFalse(ProfileSaveConfirmation.matches("alice", null, true, "bio", "bio"))
        assertFalse(ProfileSaveConfirmation.matches(null, null, true, "bio", "bio"))
        assertFalse(ProfileSaveConfirmation.matches("", "", true, "bio", "bio"))
    }
    @Test fun successfulOwnerReadbackConfirmsOnlyTheExpectedValue() {
        assertTrue(ProfileSaveConfirmation.matches("alice", "alice", true, "OBC", "obc", true))
        assertTrue(ProfileSaveConfirmation.matches("alice", "alice", true, "", ""))
        assertFalse(ProfileSaveConfirmation.matches("alice", "alice", false, "bio", "bio"))
        assertFalse(ProfileSaveConfirmation.matches("alice", "alice", true, "bio", null))
        assertFalse(ProfileSaveConfirmation.matches("alice", "alice", true, "bio", "BIO"))
    }
}
