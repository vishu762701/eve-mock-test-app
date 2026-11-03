package com.eve.app.util

import android.content.Context
import java.lang.reflect.Modifier
import org.junit.Assert.assertTrue
import org.junit.Test

/** Same single store lock prevents add/import/ack from overwriting each other. */
class NotificationStoreWriteLockTest {
    @Test fun allReadModifyWritePathsUseTheSameObjectMonitor() {
        val signatures = listOf(
            "add" to arrayOf(Context::class.java, String::class.java, String::class.java),
            "importBroadcasts" to arrayOf(Context::class.java, List::class.java),
            "markAllRead" to arrayOf(Context::class.java),
            "clearDeletedUser" to arrayOf(Context::class.java, String::class.java)
        )
        for ((method, parameters) in signatures) {
            val declared = NotificationStore::class.java.getDeclaredMethod(method, *parameters)
            assertTrue("$method must serialize JSON cache writes through NotificationStore",
                Modifier.isSynchronized(declared.modifiers))
        }
    }
}
