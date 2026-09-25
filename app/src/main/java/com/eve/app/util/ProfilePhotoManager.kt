package com.eve.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import coil.load
import com.eve.app.R
import java.io.File
import java.io.FileOutputStream

/**
 * Profile photo user ke apne device par hi save hoti hai (koi Firebase Storage/backend nahi
 * chahiye) — gallery se photo pick karke seedha app ki internal storage me copy kar dete hain.
 * Naya photo naya device par login karne par nahi dikhega (yeh purely local hai), tab tak
 * Google account wali photo (fallback) dikhti rahegi — dekho ProfileActivity/MainActivity.
 */
object ProfilePhotoManager {

    private const val FILE_NAME = "profile_photo.jpg"

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun hasCustomPhoto(context: Context): Boolean = file(context).exists()

    fun loadBitmap(context: Context): Bitmap? {
        val f = file(context)
        if (!f.exists()) return null
        return try {
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    /** Gallery se mile Uri ko center-crop karke (square) internal storage me save karta hai. */
    fun savePhoto(context: Context, uri: Uri): Boolean {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return false
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return false

            val size = minOf(original.width, original.height)
            val x = (original.width - size) / 2
            val y = (original.height - size) / 2
            val cropped = Bitmap.createBitmap(original, x, y, size, size)

            // Bahut badi image ho to memory bachane ke liye ek reasonable size par scale kar do
            val target = if (size > 512) Bitmap.createScaledBitmap(cropped, 512, 512, true) else cropped

            FileOutputStream(file(context)).use { out ->
                target.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun removePhoto(context: Context) {
        val f = file(context)
        if (f.exists()) f.delete()
    }

    /**
     * Priority: locally saved custom photo -> Google account ki photo (agar hai) -> placeholder icon.
     * [placeholderBg] screen ke hisaab se alag rakha hai (header ka background primary color hai
     * isliye wahan translucent circle sahi lagta hai, Profile screen white bg par solid primary circle).
     */
    fun applyTo(context: Context, imageView: ImageView, googlePhotoUrl: String?, placeholderBg: Int) {
        val local = loadBitmap(context)
        when {
            local != null -> {
                imageView.background = null
                imageView.setPadding(0, 0, 0, 0)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setImageBitmap(local)
            }
            !googlePhotoUrl.isNullOrBlank() -> {
                imageView.background = null
                imageView.setPadding(0, 0, 0, 0)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.load(googlePhotoUrl) {
                    placeholder(R.drawable.ic_person)
                    error(R.drawable.ic_person)
                }
            }
            else -> {
                val iconRes = if (imageView.id == R.id.ivProfile) R.drawable.ic_user_profile else R.drawable.ic_person
                imageView.colorFilter = null
                imageView.imageTintList = null
                imageView.setImageResource(iconRes)
                imageView.setBackgroundResource(placeholderBg)
                imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                val pad = (imageView.layoutParams?.width ?: 96).coerceAtLeast(40) / 5
                imageView.setPadding(pad, pad, pad, pad)
            }
        }
    }
}
