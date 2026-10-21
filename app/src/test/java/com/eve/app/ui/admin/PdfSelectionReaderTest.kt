package com.eve.app.ui.admin

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test

class PdfSelectionReaderTest {
    @Test fun copiesExactPayloadAtTheSizeBoundary() {
        val payload = ByteArray(4097) { index -> (index % 251).toByte() }
        assertArrayEquals(payload, PdfSelectionReader.readBounded(ByteArrayInputStream(payload), 4097))
    }

    @Test fun rejectsOversizedPayloadBeforeRetainingIt() {
        try {
            PdfSelectionReader.readBounded(ByteArrayInputStream(ByteArray(100_000)), 1024)
            fail("Oversized upload must be rejected")
        } catch (_: PdfSelectionReader.FileTooLargeException) {
            // Strictly greater than the upload limit is rejected.
        }
    }
}
