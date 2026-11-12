/*
 * Derived from Telegram Android AndroidUtilities.getImageOrientation
 * (`AndroidUtilities.java:6269-6320`) and PhotoViewer.applyCurrentEditMode
 * (`PhotoViewer.java:11346-11370`), Copyright Nikolai Kudashov / Telegram
 * contributors. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream

/**
 * Source-equivalent EXIF orientation handling for the profile-photo pipeline.
 *
 * Telegram maps the eight EXIF orientation values to `(rotation, invert)` and
 * then applies `postRotate` followed by a horizontal/vertical `postScale`
 * (`PhotoViewer.java:11350-11366`). EVE's profile photo save path previously
 * decoded the stream directly, which could persist a sideways camera photo
 * because `BitmapFactory` does not apply EXIF orientation.
 */
internal object ExifTransform {

    data class Orientation(val rotation: Int, val invert: Int) {
        val isIdentity: Boolean get() = rotation == 0 && invert == 0
    }

    /** EXIF spec values 1-8; anything else is treated as normal. */
    fun fromExifValue(value: Int): Orientation = when (value) {
        2 -> Orientation(0, 1)      // FLIP_HORIZONTAL
        3 -> Orientation(180, 0)    // ROTATE_180
        4 -> Orientation(0, 2)      // FLIP_VERTICAL
        5 -> Orientation(270, 2)    // TRANSPOSE
        6 -> Orientation(90, 0)     // ROTATE_90
        7 -> Orientation(270, 1)    // TRANSVERSE
        8 -> Orientation(270, 0)    // ROTATE_270
        else -> Orientation(0, 0)   // NORMAL / UNDEFINED
    }

    fun read(bytes: ByteArray): Orientation {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            fromExifValue(
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            )
        } catch (_: Exception) {
            // Non-JPEG content or a malformed EXIF block: keep the decoded
            // bitmap unchanged.
            Orientation(0, 0)
        }
    }

    /**
     * Matches Telegram's matrix order: rotate first, then mirror in display
     * coordinates. Bitmap.createBitmap returns the source unchanged when the
     * matrix is identity, so callers can compare identity before recycling.
     */
    fun apply(bitmap: Bitmap, orientation: Orientation): Bitmap {
        if (orientation.isIdentity) return bitmap
        val matrix = Matrix()
        matrix.postRotate(orientation.rotation.toFloat())
        when (orientation.invert) {
            1 -> matrix.postScale(-1f, 1f)
            2 -> matrix.postScale(1f, -1f)
        }
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }
}
