package com.eve.app.telegram.qr

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Actual original C++ pixel renderer and Java scene state; no network or fake raster. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QrBackdropSceneTest {
    private fun image(w: Int = 17, h: Int = 17) = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    private val linear = floatArrayOf(0f, 0f, 0f, 1f, 0f, 1f / 16f, .5f)

    @Test fun originalNativeThreeStopLinearAndRadialRasterHaveExpectedPixels() {
        val bitmap = image()
        val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE)
        QrBackdropNative.generate(bitmap, null, 1f, 1, linear, colors, null)
        assertEquals(Color.RED, bitmap.getPixel(0, 0))
        assertEquals(Color.GREEN, bitmap.getPixel(8, 0))
        assertEquals(Color.BLUE, bitmap.getPixel(16, 0))
        val radial = floatArrayOf(1f, 8f, 8f, 0f, 0f, 1f / 8f, .5f)
        QrBackdropNative.generate(bitmap, null, 1f, 1, radial, colors, null)
        assertEquals(Color.RED, bitmap.getPixel(8, 8))
        assertEquals(Color.GREEN, bitmap.getPixel(12, 8))
        assertEquals(Color.BLUE, bitmap.getPixel(16, 8))
        bitmap.recycle()
    }

    @Test fun originalNativeWaveAntialiasAndPremultipliedRingAreRendered() {
        val bitmap = image()
        val ring = image()
        val geometry = linear + linear
        val colors = intArrayOf(Color.RED, Color.RED, Color.RED, Color.BLUE, Color.BLUE, Color.BLUE, Color.WHITE)
        val wave = floatArrayOf(8f, 8f, 4f, 2f, 3f, 3.5f, 4f, 4.5f, 5f, 0f, .2f, .5f, 1f, .4f, 0f)
        QrBackdropNative.generate(bitmap, ring, 1f, 2, geometry, colors, wave)
        assertEquals(Color.BLUE, bitmap.getPixel(8, 8))
        assertEquals(Color.RED, bitmap.getPixel(0, 0))
        assertEquals(Color.rgb(128, 0, 128), bitmap.getPixel(12, 8))
        assertEquals(185.0, Color.alpha(ring.getPixel(12, 8)).toDouble(), 1.0)
        assertEquals(0, Color.alpha(ring.getPixel(0, 0)))
        bitmap.recycle(); ring.recycle()
    }

    @Test fun malformedNativeBoundaryInputsNeverMutateTheBitmap() {
        val bitmap = image().apply { eraseColor(Color.MAGENTA) }
        for (bad in listOf(floatArrayOf(), floatArrayOf(Float.NaN, 0f, 0f, 1f, 0f, 1f, .5f))) {
            assertThrows(IllegalArgumentException::class.java) {
                QrBackdropNative.generate(bitmap, null, 1f, 1, bad, intArrayOf(1, 2, 3), null)
            }
            assertEquals(Color.MAGENTA, bitmap.getPixel(0, 0))
        }
        bitmap.recycle()
    }

    @Test fun separateGlassControlSamplesTheSceneAtItsScreenPosition() {
        val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().visible()
        val root = android.widget.FrameLayout(activity.get())
        val host = View(activity.get())
        val control = View(activity.get())
        root.addView(host, android.widget.FrameLayout.LayoutParams(360, 780))
        root.addView(control, android.widget.FrameLayout.LayoutParams(40, 40).apply {
            leftMargin = 80; topMargin = 100
        })
        activity.get().setContentView(root)
        root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(780, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 360, 780)
        val scene = QrBackdropScene(host)
        val full = image(360, 780)
        val sample = image(40, 40)
        try {
            scene.setPalette(2, 180f, 350f, 0, false)
            scene.setGeometry(360, 780, 350f)
            scene.draw(Canvas(full), { }, false, 0f)
            assertTrue(scene.drawControlBackdrop(Canvas(sample), control))
            for ((x, y) in listOf(5 to 5, 20 to 20, 35 to 35)) {
                val expected = full.getPixel(x + 80, y + 100)
                val actual = sample.getPixel(x, y)
                assertEquals(expected, actual)
            }
            scene.close()
            assertFalse(scene.drawControlBackdrop(Canvas(sample), control))
        } finally {
            scene.close(); full.recycle(); sample.recycle()
            activity.pause().stop().destroy()
        }
    }

    @Test fun interruptedWaveUsesPreviousTargetAndSnapshotDoesNotConsumeTheScene() {
        val host = View(RuntimeEnvironment.getApplication())
        val scene = QrBackdropScene(host)
        scene.setGeometry(360, 780, 350f)
        val none = QrBackdropScene.RunnablePattern { }
        val bitmap = image(360, 780)
        scene.setPalette(1, 180f, 350f, 0, false)
        scene.setPalette(2, 180f, 350f, 0, true)
        scene.step(100, true)
        assertTrue(scene.isAnimating)
        val snapshot = scene.snapshot()
        snapshot.setGeometry(360, 780, 350f)
        snapshot.draw(Canvas(bitmap), none, false, 0f)
        snapshot.close()
        assertTrue("Export cache must not finish the live wave", scene.isAnimating)
        scene.setPalette(3, 180f, 350f, 100, true)
        scene.step(200, true)
        scene.draw(Canvas(bitmap), none, false, 0f)
        val far = bitmap.getPixel(350, 760)
        val reference = QrBackdropScene(host)
        reference.setPalette(2, 180f, 350f, 0, false)
        reference.setGeometry(360, 780, 350f)
        reference.draw(Canvas(bitmap), none, false, 0f)
        assertEquals("The source interruption base is the previous target", bitmap.getPixel(350, 760), far)
        scene.step(201, false)
        assertFalse("Reduced motion settles without running a wave", scene.isAnimating)
        assertEquals(QrPalettes.ink(3), scene.qrColor())
        scene.close()
        assertFalse(scene.drawControlBackdrop(Canvas(bitmap), host))
        reference.close()
        bitmap.recycle()
    }
}
