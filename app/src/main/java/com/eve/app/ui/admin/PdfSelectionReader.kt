package com.eve.app.ui.admin

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Bounded stream copy for PDF picks from an untrusted ContentProvider. */
internal object PdfSelectionReader {
    class FileTooLargeException : IOException("PDF exceeds the maximum upload size")

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
