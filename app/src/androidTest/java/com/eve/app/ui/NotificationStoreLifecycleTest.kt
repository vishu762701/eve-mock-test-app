package com.eve.app.ui

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.data.model.BroadcastMessage
import com.eve.app.util.NotificationStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real SharedPreferences merge/acknowledgement test; no Firebase, push, or live broadcast. */
@RunWith(AndroidJUnit4::class)
class NotificationStoreLifecycleTest {
    @Test fun multipleServerBroadcastsMergeWithLocalAndDoNotAutoAcknowledge() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        // Mirror the runtime's UID-specific namespace. Avoid modifying legacy
        // unrelated preferences during the test.
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val prefs = ctx.getSharedPreferences(NotificationStore.namespaceForUser(uid), Context.MODE_PRIVATE)
        val previousRaw = prefs.getString("key_notifications_json", null)
        val previousCursor = prefs.getLong("key_last_broadcast_delivery_ts", 0L)
        val previousSeen = prefs.getLong("key_last_seen_notif_ts", 0L)
        try {
            prefs.edit().remove("key_notifications_json")
                .remove("key_last_broadcast_delivery_ts").remove("key_last_seen_notif_ts").commit()
            NotificationStore.add(ctx, "Reminder fixture", "Local reminder remains")
            val batch = listOf(
                BroadcastMessage(id = "safe-fixture-b", title = "Broadcast two", message = "Test B", sentAt = 1700000002000L),
                BroadcastMessage(id = "safe-fixture-a", title = "Broadcast one", message = "Test A", sentAt = 1700000001000L)
            )
            assertEquals(2, NotificationStore.importBroadcasts(ctx, batch))
            assertEquals(0, NotificationStore.importBroadcasts(ctx, batch))
            val items = NotificationStore.getAll(ctx)
            assertEquals(3, items.size)
            assertEquals("Broadcast two", items[0].title)
            assertEquals("Broadcast one", items[1].title)
            assertTrue(items.any { it.title == "Reminder fixture" })
            assertTrue(NotificationStore.hasUnread(ctx))
            NotificationStore.markAllRead(ctx)
            assertFalse(NotificationStore.hasUnread(ctx))
            assertEquals(0, NotificationStore.importBroadcasts(ctx, batch))
            assertFalse(NotificationStore.hasUnread(ctx))
        } finally {
            val edit = prefs.edit()
            if (previousRaw == null) edit.remove("key_notifications_json")
            else edit.putString("key_notifications_json", previousRaw)
            edit.putLong("key_last_broadcast_delivery_ts", previousCursor)
                .putLong("key_last_seen_notif_ts", previousSeen).commit()
        }
    }
}
