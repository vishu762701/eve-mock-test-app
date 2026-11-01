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
}
