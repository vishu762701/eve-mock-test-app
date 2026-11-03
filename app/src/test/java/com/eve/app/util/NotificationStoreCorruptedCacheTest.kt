package com.eve.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationStoreCorruptedCacheTest {
    @Test fun invalidJsonAndWrongTopLevelShapeAreSafe() {
        assertTrue(NotificationStore.decodeStoredEntries(null).isEmpty())
        assertTrue(NotificationStore.decodeStoredEntries("{not valid").isEmpty())
        assertTrue(NotificationStore.decodeStoredEntries("{}").isEmpty())
        assertTrue(NotificationStore.decodeStoredEntries("[null,12,\"raw\",{}]").isEmpty())
    }

    @Test fun validEntriesSurviveMalformedNeighborsWithCorrectUnreadAndOrder() {
        val raw = """
            [
              null,
              {"title":"First","body":"Message A","timestamp":1700000002000,"read":false,
               "emoji_url":"https://example.test/a.webp","emoji_id":"emoji-A"},
              {"title":"Missing body","timestamp":1700000001900},
              {"title":null,"body":"No title","timestamp":1700000001800},
              {"title":"Wrong time","body":"Wrong","timestamp":{}},
              {"title":"Negative","body":"Ignored","timestamp":-1},
              {"title":"Second","body":"Message B","timestamp":"1700000001000","read":true},
              24
            ]
        """.trimIndent()
        val entries = NotificationStore.decodeStoredEntries(raw)
        assertEquals(2, entries.size)
        assertEquals("First", entries[0].title)
        assertEquals("Message A", entries[0].body)
        assertEquals(1700000002000L, entries[0].timestampMillis)
        assertFalse(entries[0].read)
        assertEquals("https://example.test/a.webp", entries[0].emoji?.url)
        assertEquals("Second", entries[1].title)
        assertEquals(1700000001000L, entries[1].timestampMillis)
        assertTrue(entries[1].read)
        assertNull(entries[1].emoji)
    }

    @Test fun malformedEmojiIsIgnoredWithoutDiscardingNotification() {
        val payload = """[{"title":"Exam","body":"Notice","timestamp":9,"emoji_url":null},
            {"title":"Exam 2","body":"Notice","timestamp":10,"emoji_url":5}]"""
        val rows = NotificationStore.decodeStoredEntries(payload)
        assertEquals(2, rows.size)
        assertNull(rows[0].emoji)
        assertNull(rows[1].emoji)
    }

    @Test fun allMutationPathsUseSameSanitizedSourceWithoutRemovingValidMetadata() {
        val source = File("src/main/java/com/eve/app/util/NotificationStore.kt").readText()
        assertTrue(source.contains("fun getAll(context: Context): List<StoredNotification> ="))
        assertTrue(source.contains("decodeStoredEntries(notificationPrefs(context).getString(KEY_NOTIFICATIONS, null))"))
        assertTrue(source.contains("if (decodeStoredEntry(row) != null) valid.put(row)"))
        assertTrue(source.contains("val array = readArray(prefs)"))
        assertTrue(source.contains("val old = readArray(prefs)"))
        assertTrue(source.contains("obj.put(\"read\", true)"))
    }
}
