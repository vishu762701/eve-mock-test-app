package com.eve.app.ui.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NotificationInboxOwnerTest {
    @Test fun lateResponsesCannotBeAcceptedByDifferentOrSignedOutAccounts() {
        val original = NotificationInboxOwner("student-a")
        assertTrue(original.accepts("student-a"))
        assertFalse(original.accepts("student-b"))
        assertFalse(original.accepts(null))
        assertFalse(NotificationInboxOwner(null).accepts(null))
        assertFalse(NotificationInboxOwner("").accepts(""))
    }

    @Test fun productionHandlerChecksOwnerAfterAwaitAndBeforeAnyInboxWrites() {
        val activity = File("src/main/java/com/eve/app/ui/notifications/NotificationsActivity.kt").readText()
        val response = activity.substringAfter("val res = ApiClient.apiService.getBroadcasts(50)")
            .substringBefore("NotificationStore.importBroadcasts(this@NotificationsActivity")
        assertTrue(response.contains("if (!requestOwner.accepts(auth.currentUser?.uid))"))
        assertTrue(activity.contains("auth.addAuthStateListener(accountListener)"))
        assertTrue(activity.contains("auth.removeAuthStateListener(accountListener)"))
        assertTrue(activity.contains("savedInstanceState.getString(\"inbox_owner_uid\")"))
        assertTrue(activity.contains("outState.putString(\"inbox_owner_uid\", inboxOwner.uid)"))
        val fallback = activity.substringAfter("} catch (_: Exception) {")
        assertTrue(fallback.contains("if (!requestOwner.accepts(auth.currentUser?.uid))"))
    }
}
