package com.eve.app.ui.admin.emoji

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Bounded reads work on every supported Android version, including API 24. */
internal object EmojiUploadReader {
    const val MAX_BYTES = 300 * 1024
    fun read(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_BYTES) { "Emoji must be at most 300KB." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray().also { require(it.isNotEmpty()) { "The selected file is empty." } }
    }
}
