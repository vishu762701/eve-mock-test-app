package com.eve.app.ui.profile

import android.content.Context
import android.os.Parcelable
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import com.eve.app.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfilePullStateTest {
    private fun scroll(): ProfilePullScrollView = ProfilePullScrollView(ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Eve)).apply {
        addView(LinearLayout(context), android.widget.FrameLayout.LayoutParams(320, 2000))
        canExpand = { true }; motionEnabled = { false }; maximumPull = 200f
        measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY))
        layout(0, 0, 320, 600)
    }
    private fun gesture(scroll: ProfilePullScrollView, y: Float, cancel: Boolean = false) {
        val now = android.os.SystemClock.uptimeMillis()
        for ((index, action) in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE,
            if (cancel) MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP).withIndex()) {
            val event = MotionEvent.obtain(now, now + index * 20, action, 100f, if (index == 0) 50f else y, 0)
            scroll.dispatchTouchEvent(event); event.recycle()
        }
    }
    @Test fun realDragThresholdCancellationAndRepeatedGesturesDoNotInventExpansion() {
        val view = scroll()
        gesture(view, 150f) // 100px drag / 2 = 50px; below 0.33*200.
        assertFalse(view.isExpanded); assertEquals(0f, view.expansionFraction, 0f)
        gesture(view, 210f, cancel = true)
        assertFalse(view.isExpanded)
        gesture(view, 210f)
        assertTrue(view.isExpanded); assertEquals(1f, view.expansionFraction, 0f)
        view.setExpanded(false, animated = false)
        gesture(view, 210f)
        assertTrue(view.isExpanded)
    }
    @Test fun thresholdReleasesResistanceWithoutJumpingTheHeader() {
        val view = scroll()
        val now = android.os.SystemClock.uptimeMillis()
        for ((index, y) in listOf(50f, 210f, 230f).withIndex()) {
            val event = MotionEvent.obtain(now, now + index * 20,
                if (index == 0) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_MOVE, 100f, y, 0)
            view.dispatchTouchEvent(event); event.recycle()
        }
        assertEquals(.5f, view.expansionFraction, .001f)
        val cancel = MotionEvent.obtain(now, now + 80, MotionEvent.ACTION_CANCEL, 100f, 230f, 0)
        view.dispatchTouchEvent(cancel); cancel.recycle()
        assertFalse(view.isExpanded)
    }
    @Test fun restorationKeepsStableStateWithoutAUserCallbackOrAnimation() {
        val before = scroll(); before.setExpanded(true, animated = false)
        val save = ProfilePullScrollView::class.java.getDeclaredMethod("onSaveInstanceState").apply { isAccessible = true }
        val parcel = android.os.Parcel.obtain()
        parcel.writeParcelable(save.invoke(before) as Parcelable, 0)
        parcel.setDataPosition(0)
        val state = parcel.readParcelable<Parcelable>(ProfilePullScrollView::class.java.classLoader)!!
        parcel.recycle()
        assertTrue((state as ProfilePullScrollView.PullState).expanded)
        val after = scroll()
        val restore = ProfilePullScrollView::class.java.getDeclaredMethod("onRestoreInstanceState", Parcelable::class.java).apply { isAccessible = true }
        restore.invoke(after, state)
        after.layout(0, 0, 320, 600)
        assertTrue(after.isExpanded); assertEquals(1f, after.expansionFraction, 0f)
        after.setExpanded(false, animated = false)
        assertFalse(after.isExpanded)
        val deferred = scroll().apply { maximumPull = 0f }
        restore.invoke(deferred, state)
        assertFalse(deferred.isExpanded)
        deferred.maximumPull = 200f
        assertTrue(deferred.isExpanded)
        assertEquals(1f, deferred.expansionFraction, 0f)
    }
}
