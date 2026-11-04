package com.eve.app.ui.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeneratedNowAckTest {
    private val ids = mapOf<String, Any>("testId" to "test-1", "jobId" to "job-1")

    @Test fun acceptsServerIntegerAndGsonDouble() {
        assertEquals(GeneratedNowAck("test-1", "job-1", 3),
            GeneratedNowAck.from(ids + ("questionCount" to 3), 3))
        assertEquals(GeneratedNowAck("test-1", "job-1", 3),
            GeneratedNowAck.from(ids + ("questionCount" to 3.0), 3))
    }

    @Test fun rejectsMissingWrongOrForgedPositiveAcknowledgments() {
        val invalid = listOf(
            ids, ids + ("questionCount" to 2), ids + ("questionCount" to "3"),
            ids + ("questionCount" to 3.5), ids + ("questionCount" to 0),
            ids + ("questionCount" to 201), ids + ("questionCount" to Double.NaN),
            ids + ("questionCount" to Double.POSITIVE_INFINITY),
            ids + ("testId" to ""), ids + ("jobId" to "")
        )
        for (reply in invalid) assertNull(GeneratedNowAck.from(reply, 3))
        assertNull(GeneratedNowAck.from(null, 3))
    }
}
