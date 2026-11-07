package com.eve.app.ui.common

import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class TelegramHorizontalScrollLayoutManagerTest {

    @Test
    fun layoutManagerHasHorizontalOrientationAndPredictiveAnimations() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lm = TelegramHorizontalScrollLayoutManager(context)
        assertEquals(LinearLayoutManager.HORIZONTAL, lm.orientation)
        assertTrue("Predictive animations must be enabled like Telegram FilterTabsView", lm.supportsPredictiveItemAnimations())
    }

    @Test
    fun installSetsLayoutManagerAndAdaptiveOverScroll() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rv = RecyclerView(context)
        val lm = TelegramHorizontalScrollLayoutManager.install(rv)

        assertNotNull(lm)
        assertEquals(lm, rv.layoutManager)
        assertEquals("Initial overScrollMode must be OVER_SCROLL_NEVER", View.OVER_SCROLL_NEVER, rv.overScrollMode)

        // Simulate touch down triggers OVER_SCROLL_ALWAYS
        val downEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        rv.dispatchTouchEvent(downEvent)
        assertEquals("Touch down triggers OVER_SCROLL_ALWAYS", View.OVER_SCROLL_ALWAYS, rv.overScrollMode)

        // Simulate touch up restores OVER_SCROLL_NEVER
        val upEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, 10f, 10f, 0)
        rv.dispatchTouchEvent(upEvent)
        assertEquals("Touch up restores OVER_SCROLL_NEVER", View.OVER_SCROLL_NEVER, rv.overScrollMode)
    }

    @Test
    fun sourceContractHasVerifiedUpstreamTelegramLookaheadAndEasing() {
        val src = File("src/main/java/com/eve/app/ui/common/TelegramHorizontalScrollLayoutManager.kt").readText()
        assertTrue(src.contains("FilterTabsView.java:1060-1090"))
        assertTrue(src.contains("RecyclerListView.java:2159-2185"))
        assertTrue(src.contains("lookahead = dp(60f)"))
        assertTrue(src.contains("edgeBuffer = dp(21f)"))
        assertTrue(src.contains("DecelerateInterpolator"))
        assertTrue(src.contains("maxOf(180,"))
    }
}
