package com.eve.app.ui.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.telegram.animation.NativeQrDrawable
import com.eve.app.telegram.qr.QrEmojiCell
import com.eve.app.ui.RepairVerificationActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

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
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
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
                picker.draw(Canvas(image))
                val pixels = IntArray(image.width * image.height)
                image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
                assertTrue("Real picker must paint local native animation pixels", pixels.any { it ushr 24 > 0 })
                image.recycle()
                activity.setContentView(View(activity))
                assertTrue(frames.all { !it.isRunning && it.callback == null })
            }
        }
    }
}
