package com.eve.app.ui.admin

import com.eve.app.data.model.BroadcastMessage
import com.eve.app.data.remote.ApiResponse
import org.junit.Assert.*
import org.junit.Test

/** Production broadcast acknowledgment functions are exercised without live Worker mutations. */
class BroadcastConfirmationTest {
    private val sent = BroadcastMessage(
        id = "worker-generated-id",
        title = "Schedule",
        message = "New exam available",
        targetCategory = "All Users"
    )

    @Test fun confirmedInboxPostRequiresMatchingServerCreatedIdAndContent() {
        assertTrue(confirmedBroadcastCreated(
            ApiResponse(success = true, data = sent), "Schedule", "New exam available"
        ))
        assertFalse(confirmedBroadcastCreated(
            ApiResponse(success = true, data = sent.copy(id = "")), "Schedule", "New exam available"
        ))
        assertFalse(confirmedBroadcastCreated(
            ApiResponse(success = true, data = null), "Schedule", "New exam available"
        ))
        assertFalse(confirmedBroadcastCreated(
            ApiResponse(success = false, data = sent, error = "Denied"), "Schedule", "New exam available"
        ))
        assertFalse(confirmedBroadcastCreated(
            ApiResponse(success = true, data = sent), "Other title", "New exam available"
        ))
        assertFalse(confirmedBroadcastCreated(
            ApiResponse(success = true, data = sent.copy(targetCategory = "Private")),
            "Schedule", "New exam available"
        ))
    }

    @Test fun bulkDeleteRejectsImpossibleOrMalformedCounts() {
        val valid = ApiResponse(success = true,
            data = mapOf<String, Any>("count" to 2, "requested" to 3))
        assertEquals(2, confirmedBroadcastBulkCount(valid, 3))
        assertNull(confirmedBroadcastBulkCount(valid, 2))
        assertNull(confirmedBroadcastBulkCount(valid, 0))
        assertNull(confirmedBroadcastBulkCount(
            ApiResponse(success = true, data = mapOf<String, Any>(
                "count" to 4, "requested" to 3)), 3
        ))
        assertNull(confirmedBroadcastBulkCount(
            ApiResponse(success = true, data = mapOf<String, Any>(
                "count" to 1.5, "requested" to 3)), 3
        ))
        assertNull(confirmedBroadcastBulkCount(
            ApiResponse(success = true, data = emptyMap()), 3
        ))
        assertNull(confirmedBroadcastBulkCount(
            ApiResponse(success = false, data = valid.data, error = "Denied"), 3
        ))
    }

    @Test fun serverConfirmedPartialDeleteIsNotFalselyCalledFull() {
        val partial = ApiResponse(success = true,
            data = mapOf<String, Any>("count" to 1, "requested" to 3))
        assertEquals(1, confirmedBroadcastBulkCount(partial, 3))
        val full = ApiResponse(success = true,
            data = mapOf<String, Any>("count" to 3, "requested" to 3))
        assertEquals(3, confirmedBroadcastBulkCount(full, 3))
    }
}
