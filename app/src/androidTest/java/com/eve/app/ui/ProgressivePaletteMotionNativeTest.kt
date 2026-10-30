package com.eve.app.ui

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.ui.common.PaletteItem
import com.eve.app.ui.common.PaletteState
import com.eve.app.ui.common.QuestionPaletteAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** R10: appended question only, never all future items or a recycled ghost. */
@RunWith(AndroidJUnit4::class)
class ProgressivePaletteMotionNativeTest {
    @Test fun onlyAppendedCirclesExistAndPreviouslyRevealedItemsRemain() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val recycler = RecyclerView(activity)
                recycler.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
                recycler.itemAnimator = null
                activity.setContentView(recycler)
                val selected = mutableListOf<Int>()
                val adapter = QuestionPaletteAdapter(approvedTestStyle = true, progressiveMotion = true) {
                    selected.add(it)
                }
                recycler.adapter = adapter
                adapter.submit(listOf(PaletteItem(1, PaletteState.VISITED, true)))
                assertEquals(1, adapter.itemCount)
                adapter.submit(listOf(PaletteItem(1, PaletteState.ANSWERED, false),
                    PaletteItem(2, PaletteState.VISITED, true)))
                assertEquals(2, adapter.itemCount)
                adapter.submit(listOf(PaletteItem(1, PaletteState.ANSWERED, false),
                    PaletteItem(2, PaletteState.VISITED, false),
                    PaletteItem(3, PaletteState.MARKED, true)))
                assertEquals(3, adapter.itemCount)
                assertTrue("No future Q4 should be present", adapter.itemCount < 4)
            }
        }
    }
}
