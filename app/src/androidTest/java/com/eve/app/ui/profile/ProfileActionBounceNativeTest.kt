package com.eve.app.ui.profile

import android.content.res.Configuration
import android.os.SystemClock
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileActionBounceNativeTest {
    @Test fun dayNightTapGoesThroughRealClickAndDraggedGestureIsCanceled() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val configuration = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_Eve)
                val binding = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                val actions = listOf(binding.btnSetPhoto, binding.btnEditInfo, binding.btnSettings)

                for (view in actions) {
                    var clicks = 0
                    view.setOnClickListener { clicks++ }
                    ProfileActionBounce.install(view)
                    view.layout(0, 0, 160, 80)
                    val start = SystemClock.uptimeMillis()
                    fun touch(action: Int, x: Float, y: Float, elapsed: Long) {
                        val e = MotionEvent.obtain(start, start + elapsed, action, x, y, 0)
                        try { assertTrue(view.dispatchTouchEvent(e)) } finally { e.recycle() }
                    }
                    touch(MotionEvent.ACTION_DOWN, 50f, 40f, 0L)
                    touch(MotionEvent.ACTION_UP, 50f, 40f, 110L)
                    assertEquals("Valid tap invokes existing OnClick exactly once", 1, clicks)

                    touch(MotionEvent.ACTION_DOWN, 50f, 40f, 150L)
                    touch(MotionEvent.ACTION_MOVE, 75f, 40f, 175L)
                    touch(MotionEvent.ACTION_UP, 50f, 40f, 190L)
                    assertEquals("Telegram 20px drift cancels the tap", 1, clicks)

                    touch(MotionEvent.ACTION_DOWN, 50f, 40f, 210L)
                    touch(MotionEvent.ACTION_CANCEL, 50f, 40f, 230L)
                    assertEquals("Cancel never dispatches click", 1, clicks)

                    view.isEnabled = false
                    val e = MotionEvent.obtain(start, start + 250L, MotionEvent.ACTION_DOWN, 50f, 40f, 0)
                    try { view.dispatchTouchEvent(e) } finally { e.recycle() }
                    assertEquals(1, clicks)
                }
            }
        }
    }
}
