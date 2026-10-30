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
    @Test fun emptyFirstServerPageEstablishesBaselineWithoutUnreadOrFakeMotion() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val prefs = ctx.getSharedPreferences(NotificationStore.namespaceForUser(uid), Context.MODE_PRIVATE)
        val previouslyPresent = prefs.contains("key_last_broadcast_delivery_ts")
        val previous = prefs.getLong("key_last_broadcast_delivery_ts", 0L)
        try {
            prefs.edit().remove("key_last_broadcast_delivery_ts").commit()
            assertFalse(NotificationStore.hasEstablishedBroadcastBaseline(ctx))
            assertEquals(0, NotificationStore.importBroadcasts(ctx, emptyList()))
            assertTrue(NotificationStore.hasEstablishedBroadcastBaseline(ctx))
            assertEquals(0L, prefs.getLong("key_last_broadcast_delivery_ts", -1L))
        } finally {
            val edit = prefs.edit()
            if (previouslyPresent) edit.putLong("key_last_broadcast_delivery_ts", previous)
            else edit.remove("key_last_broadcast_delivery_ts")
            edit.commit()
        }
    }

    @Test fun multipleServerBroadcastsMergeWithLocalAndDoNotAutoAcknowledge() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        // Mirror the runtime's UID-specific namespace. Avoid modifying legacy
        // unrelated preferences during the test.
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val prefs = ctx.getSharedPreferences(NotificationStore.namespaceForUser(uid), Context.MODE_PRIVATE)
        val previousRaw = prefs.getString("key_notifications_json", null)
        val previousCursor = prefs.getLong("key_last_broadcast_delivery_ts", 0L)
        val hadPreviousCursor = prefs.contains("key_last_broadcast_delivery_ts")
        val previousSeen = prefs.getLong("key_last_seen_notif_ts", 0L)
        try {
            prefs.edit().remove("key_notifications_json")
                .remove("key_last_broadcast_delivery_ts").remove("key_last_seen_notif_ts").commit()
            NotificationStore.add(ctx, "Reminder fixture", "Local reminder remains")
            assertFalse(NotificationStore.hasEstablishedBroadcastBaseline(ctx))
            val batch = listOf(
                BroadcastMessage(id = "safe-fixture-b", title = "Broadcast two", message = "Test B", sentAt = 1700000002000L),
                BroadcastMessage(id = "safe-fixture-a", title = "Broadcast one", message = "Test A", sentAt = 1700000001000L)
            )
            assertEquals(2, NotificationStore.importBroadcasts(ctx, batch))
            assertTrue(NotificationStore.hasEstablishedBroadcastBaseline(ctx))
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
            // A truly new post after the stored baseline is eligible for
            // animation and unread badge even after the inbox was acknowledged.
            assertEquals(1, NotificationStore.importBroadcasts(ctx, listOf(
                BroadcastMessage(id = "safe-fixture-c", title = "New delivery",
                    message = "Test C", sentAt = 1700000003000L))))
            assertTrue(NotificationStore.hasUnread(ctx))
        } finally {
            val edit = prefs.edit()
            if (previousRaw == null) edit.remove("key_notifications_json")
            else edit.putString("key_notifications_json", previousRaw)
            if (hadPreviousCursor) edit.putLong("key_last_broadcast_delivery_ts", previousCursor)
            else edit.remove("key_last_broadcast_delivery_ts")
            edit.putLong("key_last_seen_notif_ts", previousSeen).commit()
        }
    }
}
