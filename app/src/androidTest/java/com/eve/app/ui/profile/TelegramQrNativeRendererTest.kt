package com.eve.app.ui.profile

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.telegram.animation.TelegramAnimationRuntime
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.ui.Components.RLottieNative

/** Runs the actual bundled JNI and Rust frame renderer on the device/emulator. */
@RunWith(AndroidJUnit4::class)
class TelegramQrNativeRendererTest {
    @Test fun bundledAnimationRendersDifferentVisibleFramesAndRecycles() {
        TelegramAnimationRuntime.ensureLoaded()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val json = context.resources.openRawResource(R.raw.tg_qr_code_logo).bufferedReader().use { it.readText() }
        val animation = requireNotNull(RLottieNative.createFromRawJson(json))
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        try {
            assertTrue(animation.frameCount > 1)
            assertTrue(animation.fps in 1..60)
            val frames = listOf(0, animation.frameCount / 3, animation.frameCount * 2 / 3)
                .map { frame ->
                    animation.getFrame(frame, bitmap, true)
                    IntArray(128 * 128).also { bitmap.getPixels(it, 0, 128, 0, 0, 128, 128) }
                }
            assertTrue("Animation must draw visible pixels", frames.any { pixels -> pixels.any { it ushr 24 > 0 } })
            assertTrue("Native animation must change between frames", frames.drop(1).any { !frames[0].contentEquals(it) })
        } finally {
            animation.recycle()
            bitmap.recycle()
        }
        assertTrue(animation.isRecycled)
        try {
            animation.frameCount // Metadata remains readable, frame access is forbidden after release.
            val released = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            try {
                animation.getFrame(0, released, true)
                fail("Released native animation accepted a frame request")
            } catch (_: IllegalStateException) {
                // The real upstream lifecycle contract.
            } finally { released.recycle() }
        } finally { animation.recycle() }
    }

    @Test fun nativeDrawableReadinessAndFrozenExportFrameUseOwnedRealPixels() {
        // EVE synthetic solid fixture: unlike a genuine intro animation, its
        // first frame is deliberately opaque, so frame-readiness is observable.
        val json = """{"v":"5.7.4","fr":30,"ip":0,"op":60,"w":128,"h":128,"layers":[{"ddd":0,"ind":1,"ty":1,"nm":"test-solid","sr":1,"ks":{"o":{"a":0,"k":100},"r":{"a":0,"k":0},"p":{"a":0,"k":[64,64,0]},"a":{"a":0,"k":[64,64,0]},"s":{"a":0,"k":[100,100,100]}},"sw":128,"sh":128,"sc":"#ff0000","ip":0,"op":60,"st":0,"bm":0}]}"""
        val drawable = com.eve.app.telegram.animation.NativeQrDrawable.fromJson(json, 128)
        val image = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        try {
            drawable.setBounds(0, 0, 128, 128)
            drawable.start()
            assertTrue("Actual native frame available", drawable.prepareFrame())
            drawable.drawFrame(android.graphics.Canvas(image), false)
            val before = IntArray(128 * 128).also { image.getPixels(it, 0, 128, 0, 0, 128, 128) }
            assertTrue("Owned frame contains actual rendered pixels", before.any { it ushr 24 > 0 })
            val frameIndex = drawable.javaClass.getDeclaredField("renderedFrame").apply { isAccessible = true }
            val cached = frameIndex.getInt(drawable)
            android.os.SystemClock.sleep(100) // advance real playback time by three fixture frames
            image.eraseColor(android.graphics.Color.TRANSPARENT)
            // Export deliberately keeps the cached frame while playback remains running.
            drawable.drawFrame(android.graphics.Canvas(image), false)
            val after = IntArray(128 * 128).also { image.getPixels(it, 0, 128, 0, 0, 128, 128) }
            assertArrayEquals(before, after)
            assertEquals("Export did not advance the native frame index", cached, frameIndex.getInt(drawable))
            assertTrue(drawable.isRunning)
        } finally { drawable.close(); image.recycle() }
        assertFalse("Closed resources never claim a ready frame", drawable.prepareFrame())
        drawable.close()
    }

}
