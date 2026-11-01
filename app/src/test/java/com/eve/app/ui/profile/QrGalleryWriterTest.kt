package com.eve.app.ui.profile

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver

/** Real PNG output through ContentResolver; no remote service or user gallery is used. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QrGalleryWriterTest {
    private class GalleryProvider : ContentProvider() {
        val row: Uri = Uri.parse("content://media/external/images/media/17")
        var inserted: ContentValues? = null
        var published = false
        var deleted = false
        override fun onCreate() = true
        override fun getType(uri: Uri) = "image/png"
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri {
            inserted = ContentValues(requireNotNull(values))
            return row
        }
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
            check(uri == row)
            published = values?.getAsInteger(MediaStore.Images.Media.IS_PENDING) == 0
            return 1
        }
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            check(uri == row)
            deleted = true
            return 1
        }
    }

    @Test fun pngBecomesPublicOnlyAfterSuccessfulWrite() {
        val app = RuntimeEnvironment.getApplication()
        val provider = GalleryProvider()
        provider.attachInfo(app, ProviderInfo().apply {
            authority = "media"
            exported = true
            applicationInfo = app.applicationInfo
        })
        ShadowContentResolver.registerProviderInternal("media", provider)
        val bytes = ByteArrayOutputStream()
        shadowOf(app.contentResolver).registerOutputStream(provider.row, bytes)
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        try {
            assertEquals(provider.row, QrGalleryWriter.save(app, bitmap, "test_name"))
            assertEquals(1, provider.inserted!!.getAsInteger(MediaStore.Images.Media.IS_PENDING))
            assertTrue(provider.published)
            assertFalse(provider.deleted)
            assertArrayEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a),
                bytes.toByteArray().take(8).toByteArray())
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun failedWriteDeletesPendingRowWithoutPublishingIt() {
        val app = RuntimeEnvironment.getApplication()
        val provider = GalleryProvider()
        provider.attachInfo(app, ProviderInfo().apply {
            authority = "media"
            exported = true
            applicationInfo = app.applicationInfo
        })
        ShadowContentResolver.registerProviderInternal("media", provider)
        shadowOf(app.contentResolver).registerOutputStream(provider.row, object : OutputStream() {
            override fun write(value: Int) { throw IOException("Fixture storage is full") }
        })
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        try {
            val failure = runCatching { QrGalleryWriter.save(app, bitmap, "test_name") }.exceptionOrNull()
            assertNotNull("Storage failure must reach the UI error handler", failure)
            assertTrue(failure is IOException || failure is IllegalStateException)
            assertTrue(provider.deleted)
            assertFalse(provider.published)
        } finally { bitmap.recycle() }
    }
}
