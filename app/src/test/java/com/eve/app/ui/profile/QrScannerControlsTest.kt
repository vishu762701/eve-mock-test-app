package com.eve.app.ui.profile

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.eve.app.R
import com.eve.app.telegram.qr.QrScannerControl
import com.eve.app.telegram.qr.QrScannerGlass
import com.eve.app.telegram.qr.QrScannerLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QrScannerControlsTest {
    @Test fun portraitUsesSourceGeometryAndCompactGroupFitsMeasuredSafeArea() {
        val layout = QrScannerLayout()
        assertTrue(layout.sample(400, 900, 1f, 24, 24, 26))
        assertEquals(264f, layout.frame.width(), .001f)
        assertEquals(430f, layout.frame.centerY(), .001f)
        assertEquals(layout.frame.bottom + 64, layout.flash.top, .001f)
        assertEquals(56f, layout.flash.width(), .001f)
        assertEquals(35f, layout.gallery.right.let { 200-it }, .001f)
        assertEquals(layout.frame.top - 36 - 26, layout.titleTop, .001f)
        assertEquals(29f, layout.close.centerX(), .001f)
        assertEquals(52f, layout.close.centerY(), .001f)
        for ((w, h) in listOf(780 to 360, 320 to 480, 1200 to 360)) {
            assertTrue(layout.sample(w, h, 1f, 24, 24, 26))
            assertTrue(layout.scale > 0f && layout.scale <= 1f)
            assertTrue(layout.titleTop >= layout.close.bottom)
            assertTrue(layout.titleTop + 26*layout.scale < layout.frame.top)
            assertTrue(layout.frame.bottom < layout.flash.top)
            assertTrue(layout.flash.bottom + 8*layout.scale <= h-24+.001f)
            assertTrue(layout.flash.left > layout.gallery.right)
        }
        assertFalse(layout.sample(0, 100, 1f, 0, 0, 26))
    }

    private fun control(): QrScannerControl {
        val context = RuntimeEnvironment.getApplication()
        return QrScannerControl(context, requireNotNull(context.getDrawable(R.drawable.eve_qr_flashlight)),
            true, 56, QrScannerGlass(28f, null) { false }).apply {
                layout(0, 0, 72, 72)
            }
    }

    private fun touch(view: View, action: Int, x: Float = 36f, y: Float = 36f) {
        val event = MotionEvent.obtain(0, 10, action, x, y, 0)
        try { assertTrue(view.onTouchEvent(event)) } finally { event.recycle() }
    }

    @Test fun sourcePressCancelsWhenDraggedAwayAndKeyboardClickStillWorks() {
        val control = control()
        var clicks = 0
        control.setOnClickListener { clicks++ }
        touch(control, MotionEvent.ACTION_DOWN)
        assertTrue(control.isPressed)
        touch(control, MotionEvent.ACTION_MOVE, 90f)
        touch(control, MotionEvent.ACTION_MOVE)
        touch(control, MotionEvent.ACTION_UP)
        assertFalse(control.isPressed)
        assertEquals(0, clicks)
        touch(control, MotionEvent.ACTION_DOWN)
        touch(control, MotionEvent.ACTION_UP, 90f)
        assertEquals("An outside UP cannot trigger a stale press", 0, clicks)
        touch(control, MotionEvent.ACTION_DOWN)
        touch(control, MotionEvent.ACTION_CANCEL)
        touch(control, MotionEvent.ACTION_DOWN)
        touch(control, MotionEvent.ACTION_UP)
        assertEquals(1, clicks)
        control.performClick()
        assertEquals(2, clicks)
        assertEquals(android.widget.Button::class.java.name, control.accessibilityClassName)
    }

    @Test fun confirmedTorchSelectionAddsSourceWhiteFillAndPressRecovers() {
        val control = control()
        val image = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888)
        fun pixels(): IntArray {
            image.eraseColor(Color.TRANSPARENT)
            control.draw(Canvas(image))
            return IntArray(72*72).also { image.getPixels(it, 0, 72, 0, 0, 72, 72) }
        }
        try {
            val off = pixels()
            control.isSelected = true
            repeat(24) { pixels() }
            val on = pixels()
            assertTrue(on.indices.count { Color.red(on[it]) > Color.red(off[it]) } > 100)
            touch(control, MotionEvent.ACTION_DOWN)
            repeat(24) { pixels() }
            assertTrue(pixels().count { Color.alpha(it)>0 } < on.count { Color.alpha(it)>0 })
            touch(control, MotionEvent.ACTION_CANCEL)
            repeat(24) { pixels() }
            assertArrayEquals(on, pixels())
        } finally { image.recycle() }
    }

    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()

    @Test fun actualScannerControlsAreAccessibleAndGalleryLaunchesRealContract() {
        val controller = Robolectric.buildActivity(EveQrLiveScannerActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            root.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, 400, 900)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
            generateSequence { shadowOf(activity).nextStartedActivity }.forEach {
                assertEquals("android.content.pm.action.REQUEST_PERMISSIONS", it.action)
            }
            val controls = descendants(root).filterIsInstance<QrScannerControl>()
            assertEquals(3, controls.size)
            assertTrue(controls.all { it.isClickable && it.isFocusable && it.width >= 48 && it.height >= 48 })
            controls.single { it.contentDescription == activity.getString(R.string.qr_scanner_gallery) }.performClick()
            val intent = shadowOf(activity).nextStartedActivity
            assertNotNull(intent)
            assertEquals(Intent.ACTION_GET_CONTENT, intent.action)
            assertEquals("image/*", intent.type)
            assertNull(shadowOf(activity).nextStartedActivity)
        } finally { controller.pause().stop().destroy() }
    }
}
