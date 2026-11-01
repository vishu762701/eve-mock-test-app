package com.eve.app.ui.profile

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** EVE storage adapter for QrShareActivity.java:255-292; preserves actual gallery publication. */
internal object QrGalleryWriter {
    fun save(context: Context, bitmap: Bitmap, username: String): Uri {
        require(ProfileQrPayload.normalize(username) != null)
        val name = "EVE-${username}-${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EVE")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            try {
                requireNotNull(resolver.openOutputStream(uri)).use {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
                check(resolver.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null) == 1)
                return uri
            } catch (failure: Throwable) {
                resolver.delete(uri, null, null)
                throw failure
            }
        }
        @Suppress("DEPRECATION")
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "EVE")
        check(directory.isDirectory || directory.mkdirs())
        val file = File(directory, name)
        try {
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } catch (failure: Throwable) {
            file.delete()
            throw failure
        }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
        return Uri.fromFile(file)
    }
}
