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
}
