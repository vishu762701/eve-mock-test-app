package com.eve.app.ui.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.R
import com.eve.app.telegram.animation.NativeQrDrawable
import com.eve.app.telegram.qr.QrEmojiCell
import com.eve.app.ui.RepairVerificationActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Offline local assets through real Java cells/overlay and bundled JNI; both FULL nav modes. */
@RunWith(AndroidJUnit4::class)
class QrEmojiPickerNativeTest {
    private fun cells(view: View): List<QrEmojiCell> = when (view) {
        is QrEmojiCell -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { cells(view.getChildAt(it)) }
        else -> emptyList()
    }

    @Test fun originalJavaPickerPaintsNativeAssetsAndRetainsSixSelectionContract() {
        val frames = mutableListOf<NativeQrDrawable>()
        val layoutReady = CountDownLatch(1)
        Log.i("EveNativeQrStage", "picker-start")
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            lateinit var picker: QrEmojiPickerGrid
            var selected = emptyList<QrEmojiChoice>()
            scenario.onActivity { activity ->
                val json = activity.resources.openRawResource(R.raw.tg_fire_on).bufferedReader().use { it.readText() }
                picker = QrEmojiPickerGrid(activity, { selected }, {
                    selected = QrEmojiPattern.toggle(selected, it)
                }, { null }, { _, size -> NativeQrDrawable.fromJson(json, size).also(frames::add) })
                activity.setContentView(picker)
                picker.setItems((1..7).map { QrEmojiPattern.custom("fixture-$it", "Fixture $it", "res:raw/tg_fire_on") })
                // Native frames continuously invalidate the UI; global idle is not a layout barrier.
                val layoutListener = object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int,
                                                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                        if (right > left && bottom > top) {
                            v.removeOnLayoutChangeListener(this)
                            layoutReady.countDown()
                        }
                    }
                }
                picker.addOnLayoutChangeListener(layoutListener)
                if (picker.width > 0 && picker.height > 0) {
                    picker.removeOnLayoutChangeListener(layoutListener)
                    layoutReady.countDown()
                }
                Log.i("EveNativeQrStage", "picker-items-bound")
            }
            assertTrue("Animated picker never laid out", layoutReady.await(30, TimeUnit.SECONDS))
            // Poll the real seven bound native frames without waiting for an animated UI to go idle.
            var framesReady = false
            val deadline = SystemClock.uptimeMillis() + 30_000L
            while (!framesReady && SystemClock.uptimeMillis() < deadline) {
                scenario.onActivity {
                    framesReady = cells(picker).size == 7 &&
                        frames.size == 7 && frames.all { it.isRunning }
                }
                if (!framesReady) Thread.sleep(100L)
            }
            assertTrue("Seven real native QR frames never became ready", framesReady)
            Log.i("EveNativeQrStage", "picker-frames-ready")
            scenario.onActivity { activity ->
                val visible = cells(picker)
                assertEquals(7, visible.size)
                visible.forEach { assertTrue(it.performClick()) }
                assertEquals(6, selected.size)
                assertEquals("c:fixture-2", selected.first().key)
                assertFalse(visible.first().isSelected)
                assertTrue(visible.last().contentDescription.toString().contains("selected 6 of 6"))
                assertEquals(7, frames.size)
                assertTrue("Actual visible window must run native picker frames", frames.all { it.isRunning })
                val image = Bitmap.createBitmap(picker.width, picker.height, Bitmap.Config.ARGB_8888)
                Log.i("EveNativeQrStage", "picker-draw-start")
                picker.draw(Canvas(image))
                Log.i("EveNativeQrStage", "picker-draw-finished")
                val pixels = IntArray(image.width * image.height)
                image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
                assertTrue("Real picker must paint local native animation pixels", pixels.any { it ushr 24 > 0 })
                image.recycle()
                activity.setContentView(View(activity))
                assertTrue(frames.all { !it.isRunning && it.callback == null })
                Log.i("EveNativeQrStage", "picker-detached")
            }
        }
    }
}
