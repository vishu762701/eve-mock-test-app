package com.eve.app.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import org.robolectric.RuntimeEnvironment
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real rendered bitmap scan, not a string/source assertion. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TelegramQrRasterTest {
    private fun decode(bitmap: Bitmap): String {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(
            RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))).text
    }

    @Test fun exportImmediatelyAfterRotationUsesCurrentGeometryAndProducesARealPng() {
        val link = "eve-mock-test://username/test_name"
        val view = TelegramQrSceneView(RuntimeEnvironment.getApplication(), "test_name", link)
        try {
            for ((w, h) in listOf(360 to 780, 780 to 360)) {
                view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, w, h)
                val image = requireNotNull(view.renderShareImage())
                try {
                    val output = java.io.ByteArrayOutputStream()
                    assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output))
                    val bytes = output.toByteArray()
                    assertArrayEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), bytes.copyOfRange(0, 8))
                    val decoded = requireNotNull(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    try {
                        assertEquals(w, decoded.width)
                        assertEquals(minOf(h, (w * 1.92f).toInt()), decoded.height)
                        assertEquals(link, decode(decoded))
                    } finally { decoded.recycle() }
                } finally { image.recycle() }
            }
        } finally { view.stopMotion() }
    }

    @Test fun exportDuringPickerKeepsLiveGeometryAndInkAndLongUsernameScans() {
        val username = "w".repeat(32)
        val link = "eve-mock-test://username/$username"
        val view = TelegramQrSceneView(RuntimeEnvironment.getApplication(), username, link)
        val boundsField = view.javaClass.getDeclaredField("bounds").apply { isAccessible = true }
        val inkField = view.javaClass.getDeclaredField("frameQrColor").apply { isAccessible = true }
        try {
            for ((w, h) in listOf(400 to 900, 780 to 360)) {
                view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, w, h)
                view.setCardViewport(60f, h - 60f)
                val live = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                try {
                    for (progress in listOf(0f, .7f, 1f, .2f, 0f)) {
                        view.setPickerPlacement(progress, h * .48f)
                        view.draw(android.graphics.Canvas(live))
                        val before = android.graphics.RectF(boundsField.get(view) as android.graphics.RectF)
                        val ink = inkField.getInt(view)
                        val exported = requireNotNull(view.renderShareImage())
                        try { assertEquals(link, decode(exported)) } finally { exported.recycle() }
                        assertEquals(before, boundsField.get(view))
                        assertEquals(ink, inkField.getInt(view))
                    }
                } finally { live.recycle() }
            }
        } finally { view.release() }
    }

    @Test fun sceneReleaseClosesBackdropAndReattachesSafely() {
        val context = RuntimeEnvironment.getApplication()
        val link = "eve-mock-test://username/test_name"
        val view = TelegramQrSceneView(context, "test_name", link)
        view.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(780, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 360, 780)
        view.release()
        val image = view.renderShareImage()
        assertNotNull(image)
        image?.recycle()
        view.release()
    }

    @Test fun renderedQrRemainsDecodableAcrossColorScenesAndDeviceWidths() {
        val context = RuntimeEnvironment.getApplication()
        val link = "eve-mock-test://username/test_name"
        for ((w, h) in listOf(420 to 900, 360 to 780)) {
            val view = TelegramQrSceneView(context, "test_name", link)
            view.systemTop = 24
            view.systemBottom = 24
            view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, w, h)
            for (palette in 0 until view.paletteCount) {
                view.setPalette(palette)
                val image = view.renderShareImage()
                assertNotNull(image)
                try {
                    assertEquals(w, image!!.width)
                    assertEquals(minOf(h, (w * 1.92f).toInt()), image.height)
                    assertEquals("QR must decode after real Canvas rendering at " +
                        w + "x" + h + ", palette=" + palette,
                        link, decode(image))
                } finally {
                    image?.recycle()
                }
            }
        }
    }
}
