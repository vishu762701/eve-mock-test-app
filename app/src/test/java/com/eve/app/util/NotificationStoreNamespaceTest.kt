package com.eve.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure non-Android storage identity tests; real SharedPreferences exercised in androidTest. */
class NotificationStoreNamespaceTest {
    @Test fun independentAccountsNeverShareCachedNotifications() {
        val alice = NotificationStore.namespaceForUser("uid-account-A")
        val bob = NotificationStore.namespaceForUser("uid-account-B")
        assertNotEquals(alice, bob)
        assertEquals(alice, NotificationStore.namespaceForUser("uid-account-A"))
        assertFalse(alice.contains("uid-account-A"))
        assertTrue(alice.startsWith("eve_notifications_u_v2_"))
    }

    @Test fun guestsHaveDistinctNamespaceAndLegacyOwnerIsNotAssumed() {
        val guest = NotificationStore.namespaceForUser(null)
        assertEquals(guest, NotificationStore.namespaceForUser(""))
        assertNotEquals(guest, NotificationStore.namespaceForUser("uid-account-A"))
        assertNotEquals("eve_prefs", guest)
    }
}
