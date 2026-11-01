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
                    assertEquals("QR must decode after real Canvas rendering at " +
                        w + "x" + h + ", palette=" + palette,
                        link, decode(image!!))
                } finally {
                    image?.recycle()
                }
            }
        }
    }
}
