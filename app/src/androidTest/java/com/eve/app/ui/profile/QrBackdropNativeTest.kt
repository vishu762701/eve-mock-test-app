package com.eve.app.ui.profile

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.telegram.qr.QrBackdropNative
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Executes the packaged Android JNI renderer, independently of the host bitmap bridge. */
@RunWith(AndroidJUnit4::class)
class QrBackdropNativeTest {
    @Test fun packagedNativeRendererPreservesGradientEndpointsAndWaveBlend() {
        val image = Bitmap.createBitmap(17, 17, Bitmap.Config.ARGB_8888)
        val ring = Bitmap.createBitmap(17, 17, Bitmap.Config.ARGB_8888)
        val geometry = floatArrayOf(0f, 0f, 0f, 1f, 0f, 1f / 16f, .5f)
        try {
            QrBackdropNative.generate(image, null, 1f, 1, geometry,
                intArrayOf(Color.RED, Color.GREEN, Color.BLUE), null)
            assertEquals(Color.RED, image.getPixel(0, 0))
            assertEquals(Color.GREEN, image.getPixel(8, 0))
            assertEquals(Color.BLUE, image.getPixel(16, 0))
            val wave = floatArrayOf(8f, 8f, 4f, 2f, 3f, 3.5f, 4f, 4.5f, 5f, 0f, .2f, .5f, 1f, .4f, 0f)
            QrBackdropNative.generate(image, ring, 1f, 2, geometry + geometry,
                intArrayOf(Color.RED, Color.RED, Color.RED, Color.BLUE, Color.BLUE, Color.BLUE, Color.WHITE), wave)
            assertEquals(Color.BLUE, image.getPixel(8, 8))
            assertEquals(Color.RED, image.getPixel(0, 0))
            assertEquals(Color.rgb(128, 0, 128), image.getPixel(12, 8))
            assertEquals(185.0, Color.alpha(ring.getPixel(12, 8)).toDouble(), 1.0)
        } finally {
            image.recycle()
            ring.recycle()
        }
    }
}
