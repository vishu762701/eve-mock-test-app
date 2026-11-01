package com.eve.app.telegram.animation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/** Media-only transport: never attaches Firebase or Telegram credentials to catalogue URLs. */
internal object QrAnimationAssets {
    private const val MAX_BYTES = 4 * 1024 * 1024
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .followSslRedirects(false).build()

    suspend fun json(context: Context, source: String): String = withContext(Dispatchers.IO) {
        val bytes = when {
            source.startsWith("res:raw/") -> {
                val id = context.resources.getIdentifier(source.removePrefix("res:raw/"), "raw", context.packageName)
                require(id != 0) { "QR animation resource missing" }
                context.resources.openRawResource(id).use(::readBounded)
            }
            source.startsWith("https://") -> {
                client.newCall(Request.Builder().url(source).build()).execute().use { response ->
                    check(response.isSuccessful) { "QR animation HTTP ${response.code}" }
                    requireNotNull(response.body).byteStream().use(::readBounded)
                }
            }
            else -> error("Unsupported QR animation source")
        }
        val unpacked = if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
            GZIPInputStream(ByteArrayInputStream(bytes)).use(::readBounded)
        } else bytes
        unpacked.toString(Charsets.UTF_8)
    }

    private fun readBounded(stream: InputStream): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var count = stream.read(buffer)
        while (count != -1) {
            require(output.size() + count <= MAX_BYTES) { "QR animation is too large" }
            output.write(buffer, 0, count)
            count = stream.read(buffer)
        }
        return output.toByteArray()
    }
}
