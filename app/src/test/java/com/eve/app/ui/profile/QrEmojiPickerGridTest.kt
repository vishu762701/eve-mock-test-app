package com.eve.app.ui.profile

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import com.eve.app.telegram.qr.QrEmojiCell
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

/** Actual attached RecyclerView and original Java renderer; no network or production startup. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class QrEmojiPickerGridTest {
    private class Frame : Drawable(), Animatable, AutoCloseable {
        var closed = false
        var running = false
        var draws = 0
        override fun draw(canvas: Canvas) { draws++ }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Drawable opacity") override fun getOpacity() = PixelFormat.TRANSLUCENT
        override fun start() { running = true }
        override fun stop() { running = false }
        override fun isRunning() = running
        override fun close() { closed = true; running = false }
    }
    private fun layout(view: View, height: Int = 800) {
        view.measure(View.MeasureSpec.makeMeasureSpec(420, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 420, height)
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun cells(view: View): List<QrEmojiCell> = when (view) {
        is QrEmojiCell -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { cells(view.getChildAt(it)) }
        else -> emptyList()
    }
    private fun choices() = (1..7).map {
        QrEmojiPattern.custom(it.toString(), "Emoji $it", "res:raw/tg_fire_on")
    }

    @Test fun actualCellsToggleSixPicksRenumberAndRecycleTheirFrames() {
        val host = Robolectric.buildActivity(Activity::class.java).setup().visible()
        var selected = emptyList<QrEmojiChoice>()
        val frames = mutableListOf<Frame>()
        val picker = QrEmojiPickerGrid(host.get(), { selected }, {
            selected = QrEmojiPattern.toggle(selected, it)
        }, { null }, { _, _ -> Frame().also(frames::add) })
        host.get().setContentView(picker)
        picker.setItems(choices())
        layout(picker)
        val cells = cells(picker)
        assertEquals(7, cells.size)
        assertEquals(7, frames.size)
        cells.forEach { assertTrue(it.performClick()) }
        assertEquals(choices().drop(1), selected)
        assertFalse(cells.first().isSelected)
        assertTrue(cells.last().contentDescription.toString().contains("selected 6 of 6"))
        // The original overlay paints and drives the actual source cells' selection scale.
        val image = Bitmap.createBitmap(420, 800, Bitmap.Config.ARGB_8888)
        picker.draw(Canvas(image))
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(220))
        picker.draw(Canvas(image))
        assertTrue(cells.last().selectScale < 1f)
        assertTrue(frames.any { it.draws > 0 })
        assertTrue(cells[2].performClick())
        assertEquals(5, selected.size)
        assertFalse(cells[2].isSelected)
        assertTrue(cells.last().contentDescription.toString().contains("selected 5 of 5"))
        host.get().setContentView(View(host.get()))
        assertTrue(frames.all { it.closed && !it.running && it.callback == null })
        image.recycle()
        host.pause().stop().destroy()
    }

    @Test fun cellStopsHiddenFramesAndReducedMotionSettlesTheSourceSelection() {
        val host = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val frame = Frame()
        // Robolectric SDK 35 leaves this plain host's AttachInfo window GONE.
        // Supply the window lifecycle input explicitly; real platform visibility
        // and native animation playback are additionally asserted in AndroidTest.
        var windowState = View.VISIBLE
        val cell = object : QrEmojiCell(host.get()) {
            override fun getWindowVisibility() = windowState
        }
        host.get().setContentView(cell)
        host.visible().windowFocusChanged(true)
        layout(cell)
        cell.setFrameDrawable(frame)
        assertTrue("Attached=${cell.isAttachedToWindow}, shown=${cell.isShown}, visibility=${cell.visibility}, window=${cell.windowVisibility}", frame.running)
        cell.visibility = View.INVISIBLE
        assertFalse(frame.running)
        cell.visibility = View.VISIBLE
        assertTrue(frame.running)
        windowState = View.GONE
        cell.dispatchWindowVisibilityChanged(View.GONE)
        assertFalse(frame.running)
        windowState = View.VISIBLE
        cell.dispatchWindowVisibilityChanged(View.VISIBLE)
        assertTrue(frame.running)
        cell.setMotionEnabled(false)
        assertFalse(frame.running)
        cell.isPressed = true
        val image = Bitmap.createBitmap(420, 420, Bitmap.Config.ARGB_8888)
        cell.draw(Canvas(image))
        assertTrue(frame.draws > 0)
        assertEquals("Original pressed scale settles immediately", (420 * .78f * .8f).toDouble(),
            frame.bounds.width().toDouble(), 2.0)
        cell.setFrameDrawable(null)
        assertNull(frame.callback)
        image.recycle()
        host.pause().stop().destroy()
    }

    @Test fun scrollSnapshotSurvivesAnEmptyCatalogueLayoutBeforeAsyncItemsArrive() {
        val host = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val items = (1..60).map { QrEmojiPattern.custom("item-$it", "Emoji $it", "res:raw/tg_fire_on") }
        fun picker() = QrEmojiPickerGrid(host.get(), { emptyList() }, {}, { null }, { _, _ -> Frame() })
        // Keep the actual child viewport fixed while the Activity's window traverses;
        // otherwise its default width changes the span count between snapshots.
        val container = android.widget.FrameLayout(host.get())
        host.get().setContentView(container)
        val original = picker()
        container.addView(original, android.widget.FrameLayout.LayoutParams(420, 240))
        original.setItems(items)
        layout(original, 240)
        val grid = original.getChildAt(0) as androidx.recyclerview.widget.RecyclerView
        val manager = grid.layoutManager as androidx.recyclerview.widget.GridLayoutManager
        manager.scrollToPositionWithOffset(40, 0)
        layout(original, 240)
        val first = manager.findFirstVisibleItemPosition()
        assertTrue("Fixture must actually scroll", first > 0)
        val saved = original.saveScrollState()
        val restored = picker()
        restored.restoreScrollState(saved)
        container.removeAllViews()
        container.addView(restored, android.widget.FrameLayout.LayoutParams(420, 240))
        layout(restored, 240) // actual sheet opens before its catalogue finishes
        restored.setItems(items)
        layout(restored, 240)
        val restoredManager = (restored.getChildAt(0) as androidx.recyclerview.widget.RecyclerView)
            .layoutManager as androidx.recyclerview.widget.GridLayoutManager
        assertEquals(first, restoredManager.findFirstVisibleItemPosition())
        host.pause().stop().destroy()
    }

    @Test fun lateNonCooperativeAssetCannotAttachAfterPickerDismissal() {
        val host = Robolectric.buildActivity(Activity::class.java).setup().visible()
        var pending: kotlin.coroutines.Continuation<Drawable>? = null
        val frame = Frame()
        val picker = QrEmojiPickerGrid(host.get(), { emptyList() }, {}, { null }, { _, _ ->
            kotlin.coroutines.suspendCoroutine { pending = it }
        })
        host.get().setContentView(picker)
        picker.setItems(choices().take(1))
        layout(picker)
        host.get().setContentView(View(host.get()))
        requireNotNull(pending).resumeWith(Result.success(frame))
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Cancelled load must release late native ownership", frame.closed)
        assertNull(frame.callback)
        assertFalse(frame.running)
        host.pause().stop().destroy()
    }
}
