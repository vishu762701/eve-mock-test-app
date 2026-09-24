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

/**
 * Task 5: Helper for compressing, encoding, and displaying exam images.
 */
object ExamImageHelper {

    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return null

            val maxDim = maxOf(original.width, original.height)
            val scale = if (maxDim > 256) 256f / maxDim else 1f
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                original
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
            val bytes = out.toByteArray()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (_: Exception) {
            null
        }
    }

    fun loadExamImage(imageView: ImageView, imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_exam_placeholder)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.setPadding(10, 10, 10, 10)
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
                }
            } catch (_: Exception) { }
        }

        imageView.setPadding(0, 0, 0, 0)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.load(imageUrl) {
            placeholder(R.drawable.ic_exam_placeholder)
            error(R.drawable.ic_exam_placeholder)
        }
    }
}
