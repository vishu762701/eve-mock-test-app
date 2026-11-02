package com.eve.app.telegram.qr

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QrSceneRevealTest {
    private class Fixture {
        val context = RuntimeEnvironment.getApplication()
        val parent = FrameLayout(context)
        val layer = FrameLayout(context).also { parent.addView(it) }
        val control = View(context).also { layer.addView(it) }
        var cardScale = 0f
        var build = 0f
        var active = false
        val reveal = QrSceneReveal(layer, arrayOf(control)) { scale, value, moving ->
            cardScale = scale; build = value; active = moving
        }
    }

    @Test fun sourceSpringMovesWholeLayerAndSettlesWithoutResidualHardwareLayer() {
        val f = Fixture()
        f.reveal.open(true)
        assertEquals(0f, f.layer.alpha, 0f)
        assertTrue(f.layer.translationY > 0)
        assertEquals(.9f, f.cardScale, .0001f)
        assertEquals(.85f, f.control.scaleX, .0001f)
        f.reveal.setReveal(.5f)
        assertTrue(f.layer.alpha > 0)
        assertEquals(View.LAYER_TYPE_HARDWARE, f.layer.layerType)
        assertTrue(f.build in .8f..1f)
        assertTrue(Color.alpha((f.parent.background as ColorDrawable).color) > 0)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))
        assertEquals(0f, f.layer.translationY, 0f)
        assertEquals(1f, f.layer.alpha, 0f)
        assertEquals(1f, f.cardScale, 0f)
        assertEquals(1f, f.control.scaleX, 0f)
        assertEquals(View.LAYER_TYPE_NONE, f.layer.layerType)
        assertFalse(f.active)
        f.reveal.close()
    }

    @Test fun closeInterruptsOpeningWithoutJumpOrDuplicateCompletion() {
        val f = Fixture()
        f.reveal.open(true)
        f.reveal.setReveal(.12f)
        val y = f.layer.translationY
        val alpha = f.layer.alpha
        val scale = f.cardScale
        val control = f.control.scaleX
        var finished = 0
        f.reveal.close(true) { finished++ }
        f.reveal.close(true) { finished++ }
        assertEquals(y, f.layer.translationY, 0f)
        assertEquals(alpha, f.layer.alpha, .00001f)
        assertEquals(scale, f.cardScale, .00001f)
        assertEquals(control, f.control.scaleX, .00001f)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250))
        assertEquals(1, finished)
        assertEquals(0f, f.layer.alpha, 0f)
        f.reveal.close()
        assertEquals(View.LAYER_TYPE_NONE, f.layer.layerType)
    }

    @Test fun disposalCancelsFinishAndReducedMotionDoesNotStartAnAnimation() {
        val f = Fixture()
        f.reveal.open(false)
        assertEquals(1f, f.build, 0f)
        assertFalse(f.active)
        assertEquals(View.LAYER_TYPE_NONE, f.layer.layerType)
        var finished = 0
        f.reveal.close(true) { finished++ }
        f.reveal.close()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))
        assertEquals(0, finished)
        f.reveal.open(false)
        f.reveal.close(false) { finished++ }
        assertEquals(1, finished)
        assertEquals(View.LAYER_TYPE_NONE, f.layer.layerType)
        assertEquals(1f, f.build, 0f)
        f.reveal.close()
    }

    @Test fun sourceShadowHasColoredSoftEdgeAndCanBeRecreatedAfterDisposal() {
        val host = View(RuntimeEnvironment.getApplication())
        val shadow = QrCardShadow(host)
        val bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
        val card = RectF(60f, 60f, 180f, 180f)
        try {
            shadow.draw(Canvas(bitmap), card, Color.RED, true)
            val first = bitmap.getPixel(120, 185)
            assertTrue("Soft shadow extends below card", Color.alpha(first) in 1..16)
            assertTrue(Color.red(first) > Color.blue(first))
            shadow.close()
            bitmap.eraseColor(Color.TRANSPARENT)
            shadow.draw(Canvas(bitmap), card, Color.BLUE, true)
            val second = bitmap.getPixel(120, 185)
            assertTrue(Color.alpha(second) in 1..16)
            assertTrue(Color.blue(second) > Color.red(second))
        } finally {
            shadow.close()
            bitmap.recycle()
        }
    }
}
