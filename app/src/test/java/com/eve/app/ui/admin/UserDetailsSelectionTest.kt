package com.eve.app.ui.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDetailsSelectionTest {
    private val request = UserDetailsSelection(
        ownerUid = "admin-a", userId = "student-a", page = 2, query = "student", epoch = 9L
    )

    @Test fun currentOwnerRowAndRequestMayPublish() {
        assertTrue(request.matches("admin-a", "student-a", 2, "student", 9L))
    }

    @Test fun rapidSelectionOfAnotherUserInvalidatesEarlierResponse() {
        assertFalse(request.matches("admin-a", "student-b", 2, "student", 10L))
        assertFalse(request.matches("admin-a", "student-b", 2, "student", 9L))
        // Selecting the same row again must still reject an earlier response.
        assertFalse(request.matches("admin-a", "student-a", 2, "student", 10L))
    }

    @Test fun accountSwitchAndPrivilegeRevalidationInvalidateResults() {
        assertFalse(request.matches("admin-b", "student-a", 2, "student", 9L))
        assertFalse(request.matches(null, "student-a", 2, "student", 9L))
        assertFalse(request.matches("admin-a", null, 2, "student", 9L))
    }

    @Test fun pageSearchAndLifecycleChangeInvalidateResults() {
        assertFalse(request.matches("admin-a", "student-a", 3, "student", 9L))
        assertFalse(request.matches("admin-a", "student-a", 2, "different", 9L))
        assertFalse(request.matches("admin-a", "student-a", 2, "student", 10L))
    }

    @Test fun invalidIdentityNeverQualifiesEvenWithMatchingValues() {
        assertFalse(UserDetailsSelection("", "student-a", 2, "student", 9L)
            .matches("", "student-a", 2, "student", 9L))
        assertFalse(UserDetailsSelection("admin-a", "", 2, "student", 9L)
            .matches("admin-a", "", 2, "student", 9L))
    }
}
