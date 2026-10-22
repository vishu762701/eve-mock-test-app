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
    @Test fun acceptsValidPdfHeaderAndTrailerWithoutChangingBytes() {
        val pdf = "%PDF-1.7\n1 0 obj\n<<>>\nendobj\ntrailer\n%%EOF".toByteArray()
        assertArrayEquals(pdf, PdfSelectionReader.requirePdfContent(pdf))
        val prefixed = ("\n\n" + String(pdf)).toByteArray()
        assertArrayEquals(prefixed, PdfSelectionReader.requirePdfContent(prefixed))
    }

    @Test fun rejectsTextRenamedPdfAndTruncatedPdf() {
        for (input in listOf(
            "Not a PDF but called document.pdf",
            "%PDF-1.7\nno trailer",
            "%%EOF\nno PDF header",
            ""
        )) {
            try {
                PdfSelectionReader.requirePdfContent(input.toByteArray())
                fail("Invalid PDF must not mark the upload stage complete")
            } catch (_: PdfSelectionReader.InvalidPdfException) {
                // Only documents with both bounded header and final trailer pass.
            }
        }
    }

}
