package com.eve.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExamImageHelperTest {
    @Test fun sourceDecodeIsBoundedAtTargetScale() {
        assertEquals(1, ExamImageHelper.decodeSampleSize(256, 256, 256))
        assertEquals(8, ExamImageHelper.decodeSampleSize(4096, 3072, 256))
        assertTrue(ExamImageHelper.decodeSampleSize(Int.MAX_VALUE, Int.MAX_VALUE, 256) > 1)
    }

    @Test fun validInputsStillProduceContractualJpegDimensions() {
        val context: android.app.Application = RuntimeEnvironment.getApplication()
        val image = Bitmap.createBitmap(1280, 960, Bitmap.Config.ARGB_8888)
        val file = File.createTempFile("eve-upload-", ".png", context.cacheDir)
        try {
            file.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val uri = Uri.fromFile(file)
            val icon = ExamImageHelper.uriToBase64(context, uri)
            val banner = ExamImageHelper.uriToBannerBase64(context, uri)
            assertNotNull(icon)
            assertNotNull(banner)
            fun decoded(encoded: String?): Bitmap {
                assertTrue(encoded!!.startsWith("data:image/jpeg;base64,"))
                val data = Base64.decode(encoded.substringAfter("base64,"), Base64.DEFAULT)
                return BitmapFactory.decodeByteArray(data, 0, data.size)
            }
            val iconBitmap = decoded(icon)
            val bannerBitmap = decoded(banner)
            assertTrue(maxOf(iconBitmap.width, iconBitmap.height) <= 256)
            assertTrue(maxOf(bannerBitmap.width, bannerBitmap.height) <= 1024)
            iconBitmap.recycle()
            bannerBitmap.recycle()
        } finally {
            image.recycle()
            file.delete()
        }
    }

    @Test fun genuineMediaSignaturesAreRecognizedWithoutTrustingExtensions() {
        assertTrue(ExamImageHelper.hasImageSignature(byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()
        )))
        assertTrue(ExamImageHelper.hasImageSignature(byteArrayOf(
            137.toByte(), 80, 78, 71, 13, 10, 26, 10
        )))
        assertTrue(ExamImageHelper.hasImageSignature(
            "RIFF0000WEBP".toByteArray(Charsets.US_ASCII)
        ))
        assertTrue(!ExamImageHelper.hasImageSignature(
            "not an image".toByteArray(Charsets.UTF_8)
        ))
    }

    @Test fun missingAndMalformedContentIsNotSuccessful() {
        val context: android.app.Application = RuntimeEnvironment.getApplication()
        val absent = Uri.fromFile(File(context.cacheDir, "eve-absent-upload.png"))
        assertNull(ExamImageHelper.uriToBase64(context, absent))
        val bad = File.createTempFile("eve-bad-upload-", ".png", context.cacheDir)
        try {
            bad.writeText("not an image")
            assertNull(ExamImageHelper.uriToBase64(context, Uri.fromFile(bad)))
        } finally {
            bad.delete()
        }
    }
}
