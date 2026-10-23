package com.eve.app.ui.navigation

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Prevent vertical padding from clipping the 56dp floating pill's labels/touch targets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainTabsLayoutSizingTest {
    @Test
    fun fourPrimaryTabsRetainAtLeast48dpHeightInside56dpPill() {
        val context = RuntimeEnvironment.getApplication()
        val density = context.resources.displayMetrics.density
        val layout = MainTabsLayout(context)
        repeat(4) { layout.addView(View(context)) }

        val width = View.MeasureSpec.makeMeasureSpec((344 * density).toInt(), View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec((56 * density).toInt(), View.MeasureSpec.EXACTLY)
        layout.measure(width, height)

        assertEquals(0, layout.paddingTop)
        assertEquals(0, layout.paddingBottom)
        val minTouchHeight = (48 * density).toInt()
        for (i in 0 until layout.childCount) {
            assertTrue("Tab $i has a clipped touch target", layout.getChildAt(i).measuredHeight >= minTouchHeight)
        }
    }
}
