package com.eve.app.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.designsystem.UiArcUploadStepperView
import com.eve.app.ui.admin.ExamWithStats
import com.eve.app.ui.admin.ExistingExamsAdapter
import com.eve.app.ui.admin.UiArcPaginationStrip
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/** Attached Android views; no Firebase login, network, fake backend or live account. */
@RunWith(AndroidJUnit4::class)
class Batch08NativeBehaviorTest {
    @Test fun paginationKeepsFiveRealClickablePagesAndAccessibleCurrentMark() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            val tapped = AtomicInteger(-1)
            scenario.onActivity { activity ->
                val strip = UiArcPaginationStrip(activity)
                activity.setContentView(strip)
                strip.bind(10, 25) { tapped.set(it) }
                val row = strip.getChildAt(1) as LinearLayout
                assertEquals(5, row.childCount)
                assertEquals("Page 10, current page",
                    (row.getChildAt(2) as TextView).contentDescription.toString())
                assertTrue(row.getChildAt(4).performClick())
                assertEquals(12, tapped.get())
                strip.bind(25, 25) { tapped.set(it) }
                assertEquals("Page 25, current page",
                    (row.getChildAt(4) as TextView).contentDescription.toString())
                strip.bind(1, 0) { tapped.set(it) }
                assertEquals(0, row.childCount)
                strip.layoutDirection = View.LAYOUT_DIRECTION_RTL
                strip.bind(3, 5) { tapped.set(it) }
                assertEquals(5, row.childCount)
            }
        }
    }

    @Test fun stepperNeverMarksInvalidOrInterruptedUploadComplete() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val progress = UiArcUploadStepperView(activity)
                activity.setContentView(progress)
                progress.update(false, false, true, true, true)
                assertTrue(progress.contentDescription.contains("Step 1 of 3"))
                progress.update(true, false, false, true, false)
                assertTrue(progress.contentDescription.contains("Step 2 of 3"))
                progress.update(true, true, true, false, false)
                assertTrue(progress.contentDescription.contains("in progress"))
                progress.update(true, true, false, false, true)
                assertTrue(progress.contentDescription.contains("failed"))
                progress.update(true, true, false, true, false)
                assertTrue(progress.contentDescription.contains("All steps complete"))
                progress.update(true, false, false, false, false)
                assertTrue(progress.contentDescription.contains("Step 2 of 3"))
            }
        }
    }

    @Test fun existingExamActionsStillInvokeRealExamIdentityOnSingleTap() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val edited = AtomicInteger()
                val previewed = AtomicInteger()
                val expanded = AtomicInteger()
                val testExam = Exam(id = "real-id-one", examName = "Parent")
                val adapter = ExistingExamsAdapter(
                    onExamClick = { },
                    onEditClick = { if (it.id == testExam.id) edited.incrementAndGet() },
                    onPreviewClick = { if (it.id == testExam.id) previewed.incrementAndGet() },
                    onDeleteClick = { },
                    onExpandToggle = { if (it.id == testExam.id) expanded.incrementAndGet() }
                )
                val list = RecyclerView(activity).apply {
                    layoutManager = LinearLayoutManager(activity)
                    this.adapter = adapter
                }
                activity.setContentView(list)
                adapter.submitList(listOf(ExamWithStats(
                    exam = testExam, hasChildren = true
                ))) {
                    val holder = list.findViewHolderForAdapterPosition(0)
                    assertNotNull("Bound admin exam card must exist", holder)
                    val root = holder!!.itemView
                    assertTrue(root.findViewById<View>(R.id.btnQuickPreview).performClick())
                    assertTrue(root.findViewById<View>(R.id.btnQuickEdit).performClick())
                    assertTrue(root.findViewById<View>(R.id.btnExpandExam).performClick())
                    assertEquals(1, previewed.get())
                    assertEquals(1, edited.get())
                    assertEquals(1, expanded.get())
                }
            }
        }
    }
}
