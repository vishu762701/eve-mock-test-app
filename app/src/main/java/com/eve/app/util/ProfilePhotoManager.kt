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
import java.security.MessageDigest
import com.google.firebase.auth.FirebaseAuth

/**
 * Profile photo user ke apne device par hi save hoti hai (koi Firebase Storage/backend nahi
 * chahiye) — gallery se photo pick karke seedha app ki internal storage me copy kar dete hain.
 * Naya photo naya device par login karne par nahi dikhega (yeh purely local hai), tab tak
 * Google account wali photo (fallback) dikhti rahegi — dekho ProfileActivity/MainActivity.
 */
object ProfilePhotoManager {

    // The old ownerless profile_photo.jpg is intentionally not assigned to
    // whichever Firebase account signs in on this phone later.
    internal fun scopedFileName(uid: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "profile_photo_u_v2_$digest.jpg"
    }

    private fun currentFile(context: Context): File? =
        FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }
            ?.let { File(context.filesDir, scopedFileName(it)) }

    fun hasCustomPhoto(context: Context): Boolean = currentFile(context)?.exists() == true

    fun loadBitmap(context: Context): Bitmap? {
        val f = currentFile(context) ?: return null
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

            val destination = currentFile(context) ?: return false
            FileOutputStream(destination).use { out ->
                target.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun removePhoto(context: Context, ownerUid: String? = FirebaseAuth.getInstance().currentUser?.uid) {
        val uid = ownerUid?.takeIf { it.isNotBlank() } ?: return
        val f = File(context.filesDir, scopedFileName(uid))
        if (f.exists()) f.delete()
    }

    /**
     * Priority: locally saved custom photo -> Google account photo -> Telegram-style auto-generated gradient avatar.
     */
    fun applyTo(
        context: Context,
        imageView: ImageView,
        googlePhotoUrl: String?,
        placeholderBg: Int = R.drawable.design_avatar_placeholder,
        userName: String? = null,
        userId: String? = null
    ) {
        val local = loadBitmap(context)
        val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val effectiveName = userName ?: authUser?.displayName ?: "Student"
        val effectiveId = userId ?: authUser?.uid ?: authUser?.email ?: effectiveName

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
                val fallbackDrawable = AvatarDrawable.create(imageView.context, effectiveName, effectiveId)
                imageView.load(googlePhotoUrl) {
                    placeholder(fallbackDrawable)
                    error(fallbackDrawable)
                }
            }
            else -> {
                imageView.background = null
                imageView.setPadding(0, 0, 0, 0)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setImageDrawable(AvatarDrawable.create(imageView.context, effectiveName, effectiveId))
            }
        }
    }
}
