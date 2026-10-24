package com.eve.app.ui.admin

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Bounded stream copy for PDF picks from an untrusted ContentProvider. */
internal object PdfSelectionReader {
    class FileTooLargeException : IOException("PDF exceeds the maximum upload size")
    class InvalidPdfException : IOException("Selected document is not a valid PDF file")

    /**
     * Lightweight format validation, not a substitute for a full PDF parser.
     * Valid PDF streams carry a %PDF-1.x / %PDF-2.0 header in the first 1024
     * bytes and a %%EOF trailer near the end (possibly after incremental updates).
     */
    fun requirePdfContent(bytes: ByteArray): ByteArray {
        if (bytes.size < 16) throw InvalidPdfException()
        val charset = Charsets.ISO_8859_1
        val prefix = String(bytes, 0, minOf(bytes.size, 1024), charset)
        val header = Regex("%PDF-[12]\\.[0-9]").find(prefix)
        if (header == null) throw InvalidPdfException()
        val tailStart = (bytes.size - 4096).coerceAtLeast(0)
        val trailer = String(bytes, tailStart, bytes.size - tailStart, charset)
        if (!trailer.contains("%%EOF")) throw InvalidPdfException()
        return bytes
    }

    fun readBounded(input: InputStream, limitBytes: Long): ByteArray {
        require(limitBytes > 0 && limitBytes <= Int.MAX_VALUE)
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(32 * 1024)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) throw IOException("PDF stream made no progress")
            total += count
            if (total > limitBytes) throw FileTooLargeException()
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
