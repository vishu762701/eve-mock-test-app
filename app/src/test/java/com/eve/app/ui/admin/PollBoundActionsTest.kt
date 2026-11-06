package com.eve.app.ui.admin

import android.app.Application
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.eve.app.R
import com.eve.app.data.model.Poll
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Bind the actual production adapter/layout; tools:text is never runtime text. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PollBoundActionsTest {
    @Test fun activeClosedAndExpiredRowsExposeRealLabelsAndCorrectCallbacks() {
        val parent = FrameLayout(ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Eve))
        var toggled: String? = null
        var deleted: String? = null
        val adapter = PollsAdminAdapter({toggled=it.id},{deleted=it.id})
        val holder = adapter.onCreateViewHolder(parent,0)
        for ((poll,label) in listOf(
            Poll(id="active",active=true,endsAt=0) to "Close Poll",
            Poll(id="closed",active=false,endsAt=0) to "Reopen Poll",
            Poll(id="expired",endsAt=1) to "Expired")) {
            holder.bind(poll)
            val close = holder.itemView.findViewById<TextView>(R.id.btnToggleActive)
            val remove = holder.itemView.findViewById<TextView>(R.id.btnDeletePoll)
            assertEquals(label, close.text.toString())
            assertEquals("Delete", remove.text.toString())
            assertEquals(View.VISIBLE, remove.visibility)
            assertTrue(Color.alpha(remove.currentTextColor)>0)
            assertTrue(Color.alpha(close.currentTextColor)>0)
            assertEquals(poll.id!="expired",close.isEnabled)
            if (close.isEnabled) { close.performClick(); assertEquals(poll.id,toggled) }
            remove.performClick()
            assertEquals(poll.id,deleted)
        }
    }

    @Test @Config(qualifiers = "night")
    fun nightRowsKeepActualActionLabels() = activeClosedAndExpiredRowsExposeRealLabelsAndCorrectCallbacks()

    @Test
    fun expandingPollResultsBindsTelegramLineProgressView() {
        val parent = FrameLayout(ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Eve))
        val adapter = PollsAdminAdapter({}, {})
        val holder = adapter.onCreateViewHolder(parent, 0)
        val poll = Poll(
            id = "test_poll",
            question = "Which exam are you targeting?",
            options = listOf("UPSC CSE", "SSC CGL"),
            voteCounts = mapOf("0" to 10, "1" to 20),
            active = true
        )
        holder.bind(poll)
        val tapToView = holder.itemView.findViewById<TextView>(R.id.tvTapToView)
        tapToView.performClick()
        val optionsContainer = holder.itemView.findViewById<android.widget.LinearLayout>(R.id.optionsContainer)
        assertEquals(2, optionsContainer.childCount)
        val firstRow = optionsContainer.getChildAt(0)
        val pb = firstRow.findViewById<com.eve.app.ui.common.TelegramLineProgressView>(R.id.pbOption)
        assertNotNull(pb)
        assertEquals(33, pb.progress)
    }
}
