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

    internal fun fileForUid(context: Context, uid: String?): File? =
        uid?.takeIf { it.isNotBlank() }?.let { File(context.filesDir, scopedFileName(it)) }

    fun hasCustomPhoto(
        context: Context,
        ownerUid: String? = FirebaseAuth.getInstance().currentUser?.uid
    ): Boolean = fileForUid(context, ownerUid)?.exists() == true

    fun loadBitmap(
        context: Context,
        ownerUid: String? = FirebaseAuth.getInstance().currentUser?.uid
    ): Bitmap? {
        val f = fileForUid(context, ownerUid) ?: return null
        if (!f.exists()) return null
        return try {
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decodes a selected photo for the source crop surface: EXIF orientation is
     * baked in, the decode is sampled, and the longest side is bounded so a
     * 48MP camera image cannot exhaust the heap before cropping.
     */
    fun decodeForCrop(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2048
    ): Bitmap? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            val orientation = ExifTransform.read(bytes)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: return null
            val oriented = ExifTransform.apply(decoded, orientation)
            if (oriented !== decoded) decoded.recycle()

            val longest = maxOf(oriented.width, oriented.height)
            if (longest <= maxDimension) {
                oriented
            } else {
                val scale = maxDimension.toFloat() / longest
                val width = (oriented.width * scale).toInt().coerceAtLeast(1)
                val height = (oriented.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(oriented, width, height, true)
                if (scaled !== oriented) oriented.recycle()
                scaled
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Adopts the crop surface's already-square, up-to-800px JPEG without a
     * second generation-loss encode. Anything unexpected falls back to the
     * source-centered [savePhoto] pipeline.
     */
    fun saveCroppedPhoto(
        context: Context,
        uri: Uri,
        ownerUid: String? = FirebaseAuth.getInstance().currentUser?.uid
    ): Boolean {
        val uid = ownerUid?.takeIf { it.isNotBlank() } ?: return false
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return false
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
            val square = bounds.outWidth == bounds.outHeight
            val bounded = maxOf(bounds.outWidth, bounds.outHeight) <= 800
            if (!square || !bounded) return savePhoto(context, uri, uid)

            val destination = File(context.filesDir, scopedFileName(uid))
            val temp = File(destination.parentFile, destination.name + ".tmp")
            try {
                FileOutputStream(temp).use { out -> out.write(bytes) }
            } catch (_: Exception) {
                temp.delete()
                return false
            }
            if (!temp.renameTo(destination)) {
                temp.delete()
                return false
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Gallery/camera Uri ko EXIF orientation ke saath center-crop karke square
     * internal storage me save karta hai. Telegram ka avatar export
     * `ImageUpdater.java:845-855` ke `800x800`, quality 80 se matched hai;
     * orientation mapping `AndroidUtilities.java:6269-6320` aur matrix order
     * `PhotoViewer.java:11350-11366` se hai.
     */
    fun savePhoto(
        context: Context,
        uri: Uri,
        ownerUid: String? = FirebaseAuth.getInstance().currentUser?.uid
    ): Boolean {
        val uid = ownerUid?.takeIf { it.isNotBlank() } ?: return false
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return false
            val orientation = ExifTransform.read(bytes)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: return false

            val oriented = ExifTransform.apply(original, orientation)
            val size = minOf(oriented.width, oriented.height)
            if (size <= 0) {
                if (oriented !== original) oriented.recycle()
                original.recycle()
                return false
            }
            val x = (oriented.width - size) / 2
            val y = (oriented.height - size) / 2
            val cropped = Bitmap.createBitmap(oriented, x, y, size, size)

            // Telegram scales the selected avatar to 800x800 at quality 80.
            val target = if (size > 800) {
                Bitmap.createScaledBitmap(cropped, 800, 800, true)
            } else {
                cropped
            }

            val destination = File(context.filesDir, scopedFileName(uid))
            val temp = File(destination.parentFile, destination.name + ".tmp")
            val compressed = try {
                FileOutputStream(temp).use { out ->
                    target.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
            } catch (_: Exception) {
                temp.delete()
                return false
            } finally {
                // These bitmaps are owned by this decode only. Identity checks
                // avoid recycling a bitmap returned unchanged by crop/scale.
                if (target !== cropped && target !== oriented && target !== original) {
                    target.recycle()
                }
                if (cropped !== oriented && cropped !== original) cropped.recycle()
                if (oriented !== original) oriented.recycle()
                original.recycle()
            }
            if (!compressed) {
                temp.delete()
                return false
            }
            // Never truncate the last verified avatar: publish the completed
            // JPEG with one same-directory rename.
            if (!temp.renameTo(destination)) {
                temp.delete()
                return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Telegram's avatar loader decodes to a bounded 800px target rather than a
     * full camera bitmap. Keep at least [target] pixels on the shorter side so
     * the square crop is not upscaled; larger decodes are scaled down after.
     */
    internal fun sampleSizeFor(width: Int, height: Int, target: Int = 800): Int {
        if (width <= 0 || height <= 0 || target <= 0) return 1
        var sample = 1
        val shorterSide = minOf(width, height)
        while (shorterSide / (sample * 2) >= target) {
            sample *= 2
        }
        return sample
    }

    fun removePhoto(context: Context, ownerUid: String? = FirebaseAuth.getInstance().currentUser?.uid) {
        val uid = ownerUid?.takeIf { it.isNotBlank() } ?: return
        val f = File(context.filesDir, scopedFileName(uid))
        if (f.exists()) f.delete()
        File(f.parentFile, f.name + ".tmp").delete()
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
        val local = loadBitmap(context, userId)
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
