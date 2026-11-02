package com.eve.app.ui.profile

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.os.Looper
import java.time.Duration

/** Actual attached source-clipped picker container; no network or production startup. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QrPickerSheetSurfaceTest {
    @Test fun sourcePickerClipStartsClosedAndSettlesWithRoundedInset() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val surface = QrPickerSheetSurface(activity.get())
        val fill = View(activity.get()).apply { setBackgroundColor(Color.RED) }
        surface.addView(fill, FrameLayout.LayoutParams(-1, -1))
        activity.get().setContentView(surface)
        val specW = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
        val specH = View.MeasureSpec.makeMeasureSpec(700, View.MeasureSpec.EXACTLY)
        surface.measure(specW, specH)
        surface.layout(0, 0, 400, 700)
        val image = Bitmap.createBitmap(400, 700, Bitmap.Config.ARGB_8888)
        try {
            surface.draw(Canvas(image))
            // The source island starts at the bottom and its content is invisible.
            assertEquals(Color.TRANSPARENT, image.getPixel(200, 350))
            surface.animateSourceOpen()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(520))
            image.eraseColor(Color.TRANSPARENT)
            surface.draw(Canvas(image))
            assertEquals(Color.RED, image.getPixel(200, 350))
            assertEquals(Color.TRANSPARENT, image.getPixel(0, 0))
            assertEquals(Color.TRANSPARENT, image.getPixel(4, 350))
            activity.get().setContentView(View(activity.get()))
        } finally {
            surface.dispose()
            image.recycle()
            activity.pause().stop().destroy()
        }
    }

    @Test fun closedSheetConsumesTouchBeforeControlCanBeActivated() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val surface = QrPickerSheetSurface(activity.get())
        var presses = 0
        surface.addView(View(activity.get()).apply {
            isClickable = true
            setOnClickListener { presses++ }
        }, FrameLayout.LayoutParams(400, 700))
        activity.get().setContentView(surface)
        surface.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(700, View.MeasureSpec.EXACTLY))
        surface.layout(0, 0, 400, 700)
        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 200f, 350f, 0)
        val up = MotionEvent.obtain(0, 10, MotionEvent.ACTION_UP, 200f, 350f, 0)
        try {
            assertTrue(surface.onInterceptTouchEvent(down))
            assertTrue(surface.dispatchTouchEvent(down))
            assertTrue(surface.dispatchTouchEvent(up))
            assertEquals(0, presses)
        } finally {
            down.recycle(); up.recycle()
            activity.get().setContentView(View(activity.get()))
            activity.pause().stop().destroy()
        }
    }

    @Test fun closingInterruptedOpeningFinishesOnlyOnce() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val surface = QrPickerSheetSurface(activity.get())
        surface.addView(View(activity.get()).apply { setBackgroundColor(Color.RED) },
            FrameLayout.LayoutParams(-1, -1))
        activity.get().setContentView(surface)
        surface.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(700, View.MeasureSpec.EXACTLY))
        surface.layout(0, 0, 400, 700)
        val image = Bitmap.createBitmap(400, 700, Bitmap.Config.ARGB_8888)
        try {
            var count = 0
            surface.animateSourceOpen()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(80))
            surface.animateSourceClose { count++ }
            val event = MotionEvent.obtain(0, 10, MotionEvent.ACTION_DOWN, 200f, 300f, 0)
            try { assertTrue(surface.onInterceptTouchEvent(event)) }
            finally { event.recycle() }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(440))
            assertEquals(1, count)
            image.eraseColor(Color.TRANSPARENT)
            surface.draw(Canvas(image))
            assertEquals(Color.TRANSPARENT, image.getPixel(200, 350))
        } finally {
            surface.dispose()
            activity.get().setContentView(View(activity.get()))
            image.recycle()
            activity.pause().stop().destroy()
        }
    }
}
