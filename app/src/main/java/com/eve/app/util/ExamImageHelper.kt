package com.eve.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import coil.load
import com.eve.app.R
import java.io.ByteArrayOutputStream
import java.io.BufferedInputStream

/**
 * Task 5: Helper for compressing, encoding, and displaying exam images.
 */
object ExamImageHelper {

    /** Use power-of-two decode sampling to bound untrusted image memory before resizing. */
    internal fun decodeSampleSize(width: Int, height: Int, target: Int): Int {
        if (width <= 0 || height <= 0 || target <= 0) return 1
        var sample = 1
        val longest = maxOf(width.toLong(), height.toLong())
        while (sample < (1 shl 30) && longest / sample > target.toLong() * 2L) {
            sample = sample shl 1
        }
        return sample
    }

    /** Reject non-image document providers before consulting the framework decoder. */
    internal fun hasImageSignature(bytes: ByteArray, length: Int = bytes.size): Boolean {
        val count = minOf(length, bytes.size)
        fun matches(index: Int, values: IntArray): Boolean =
            index >= 0 && index + values.size <= count &&
                values.indices.all { (bytes[index + it].toInt() and 255) == values[it] }
        fun ascii(index: Int, value: String): Boolean =
            index >= 0 && index + value.length <= count &&
                value.indices.all { bytes[index + it].toInt() == value[it].code }
        return matches(0, intArrayOf(255, 216, 255)) || // JPEG
            matches(0, intArrayOf(137, 80, 78, 71, 13, 10, 26, 10)) || // PNG
            (ascii(0, "RIFF") && ascii(8, "WEBP")) ||
            ascii(0, "GIF87a") || ascii(0, "GIF89a") ||
            ascii(0, "BM") || // BMP
            matches(0, intArrayOf(73, 73, 42, 0)) || // TIFF little-endian
            matches(0, intArrayOf(77, 77, 0, 42)) || // TIFF big-endian
            matches(0, intArrayOf(0, 0, 0, 12, 106, 80, 32, 32, 13, 10, 135, 10)) || // JP2
            (ascii(4, "ftyp") && listOf(
                "heic", "heix", "hevc", "hevx", "mif1", "msf1", "avif", "avis"
            ).any { ascii(8, it) }) // HEIF / AVIF
    }

    private fun encodeSizedImage(context: Context, uri: Uri, target: Int, quality: Int): String? {
        return try {
            val resolver = context.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(chunk)
                    if (read <= 0) break
                    total += read
                    if (total > 15 * 1024 * 1024) return null
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            } ?: return null

            if (!hasImageSignature(bytes)) return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = decodeSampleSize(bounds.outWidth, bounds.outHeight, target)
            }
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
            try {
                val longest = maxOf(original.width, original.height)
                val scale = if (longest > target) target.toFloat() / longest else 1f
                val scaled = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        original,
                        (original.width * scale).toInt().coerceAtLeast(1),
                        (original.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else original
                try {
                    val output = ByteArrayOutputStream()
                    if (!scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)) return null
                    "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                } finally {
                    if (scaled !== original) scaled.recycle()
                }
            } finally {
                original.recycle()
            }
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            // Reject an exceptionally large/corrupt provider image instead of crashing the editor.
            null
        }
    }

    fun uriToBase64(context: Context, uri: Uri): String? =
        encodeSizedImage(context, uri, target = 256, quality = 70)

    fun uriToBannerBase64(context: Context, uri: Uri): String? =
        encodeSizedImage(context, uri, target = 1024, quality = 75)

    fun loadExamImage(imageView: ImageView, imageUrl: String?, onFailure: (() -> Unit)? = null) {
        if (imageUrl.isNullOrBlank()) {
            if (onFailure != null) {
                onFailure.invoke()
            } else {
                imageView.setImageResource(R.drawable.ic_exam_placeholder)
                imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                imageView.setPadding(10, 10, 10, 10)
            }
            return
        }

        if (imageUrl.startsWith("data:image")) {
            try {
                val b64 = imageUrl.substringAfter("base64,")
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    imageView.setPadding(0, 0, 0, 0)
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageView.setImageBitmap(bmp)
                    return
                } else {
                    onFailure?.invoke()
                    return
                }
            } catch (_: Exception) {
                onFailure?.invoke()
                return
            }
        }

        imageView.setPadding(0, 0, 0, 0)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.load(imageUrl) {
            placeholder(R.drawable.ic_exam_placeholder)
            error(R.drawable.ic_exam_placeholder)
            listener(
                onError = { _, _ -> onFailure?.invoke() }
            )
        }
    }

    fun loadBannerImage(imageView: ImageView, imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) {
            imageView.setImageDrawable(null)
            return
        }

        if (imageUrl.startsWith("data:image")) {
            try {
                val b64 = imageUrl.substringAfter("base64,")
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    imageView.adjustViewBounds = false
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageView.setImageBitmap(bmp)
                    return
                }
            } catch (_: Exception) { }
        }

        imageView.adjustViewBounds = false
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.load(imageUrl) {
            crossfade(true)
        }
    }
}
