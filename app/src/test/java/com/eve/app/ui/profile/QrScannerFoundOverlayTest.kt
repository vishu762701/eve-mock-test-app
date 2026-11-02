package com.eve.app.ui.profile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class QrScannerFoundOverlayTest {
    @Test fun realOverlayOnlyDrawsValidFourPointFoundCode() {
        val overlay = QrScannerFrameOverlay(RuntimeEnvironment.getApplication())
        overlay.layout(0, 0, 240, 320)
        assertFalse(overlay.showFound(floatArrayOf(30f, 30f, 100f, 30f, 100f, 100f)))
        assertFalse(overlay.showFound(floatArrayOf(30f, 30f, 100f, 30f, 100f, 100f,
            Float.POSITIVE_INFINITY, 50f)))
        assertTrue(overlay.showFound(floatArrayOf(60f, 80f, 160f, 80f, 160f, 180f,
            60f, 180f)))
        val bitmap = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            repeat(16) { overlay.draw(canvas) }
            // The source yellow outline is inside the scanner frame; background
            // never replaces a successful decode with an invented shape.
            val hasSourceYellow = (0 until bitmap.height).any { y ->
                (0 until bitmap.width).any { x ->
                    val pixel = bitmap.getPixel(x, y)
                    Color.red(pixel) > 190 && Color.green(pixel) > 145 &&
                        Color.blue(pixel) < 90 && Color.alpha(pixel) > 30
                }
            }
            assertTrue("genuine four-point corners must render source yellow", hasSourceYellow)
            overlay.clearFound()
        } finally {
            bitmap.recycle()
        }
    }
}
