package com.eve.app.ui

import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.ui.home.HomeAppearance
import com.eve.app.ui.home.HomeCategoryChipGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Source-compiled by FAST; runs on both navigation emulators in mission-final FULL. */
@RunWith(AndroidJUnit4::class)
class HomeCategoryTouchNativeTest {
    @Test fun delegatedTapsChooseOnlyTheirHitChipAndCancelDoesNotClick() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val group = HomeCategoryChipGroup(activity)
                activity.setContentView(group)
                var firstClicks = 0
                var secondClicks = 0
                val first = HomeAppearance.categoryChip(activity, "All", "All") { firstClicks++ }
                val second = HomeAppearance.categoryChip(activity, "Other", "All") { secondClicks++ }
                group.addView(first)
                group.addView(second)
                group.measure(
                    View.MeasureSpec.makeMeasureSpec(activity.resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.AT_MOST)
                )
                group.layout(0, 0, group.measuredWidth, group.measuredHeight)
                val delegate = group.touchDelegate
                assertNotNull(delegate)
                fun event(action: Int, x: Float, y: Float, downTime: Long = 100L): Boolean {
                    val e = MotionEvent.obtain(downTime, downTime + 16, action, x, y, 0)
                    return try { delegate!!.onTouchEvent(e) } finally { e.recycle() }
                }
                val fy = (first.top + first.bottom) / 2f
                val fx = (first.left + first.right) / 2f
                val sx = (second.left + second.right) / 2f
                val sy = (second.top + second.bottom) / 2f
                assertTrue(event(MotionEvent.ACTION_DOWN, fx, fy))
                assertTrue(event(MotionEvent.ACTION_UP, fx, fy))
                assertEquals(1, firstClicks)
                assertEquals(0, secondClicks)
                assertTrue(event(MotionEvent.ACTION_DOWN, sx, sy))
                assertTrue(event(MotionEvent.ACTION_UP, sx, sy))
                assertEquals(1, firstClicks)
                assertEquals(1, secondClicks)
                assertTrue(event(MotionEvent.ACTION_DOWN, fx, fy))
                assertTrue(event(MotionEvent.ACTION_CANCEL, fx, fy))
                assertEquals(1, firstClicks)
                assertEquals(1, secondClicks)
            }
        }
    }
}
