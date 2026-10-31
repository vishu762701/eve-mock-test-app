package com.eve.app.ui.admin

import android.content.res.Configuration
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.data.model.Poll
import com.eve.app.databinding.ItemPollAdminBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PollDeleteLabelNativeTest {
    @Test fun destructiveActionHasRuntimeLabelAndCorrectRecycledClickInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (mode in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val deleted = mutableListOf<String>()
                val adapter = PollsAdminAdapter({}, { deleted.add(it.id) })
                adapter.submitList(listOf(
                    Poll(id = "first", question = "First question"),
                    Poll(id = "second", question = "Second question")
                ))
                val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
                val binding = ItemPollAdminBinding.bind(holder.itemView)
                for (position in 0..1) {
                    adapter.onBindViewHolder(holder, position)
                    assertEquals("Delete", binding.btnDeletePoll.text.toString())
                    assertEquals("Delete poll", binding.btnDeletePoll.contentDescription.toString())
                    assertTrue("Delete button must remain enabled for admin action", binding.btnDeletePoll.isEnabled)
                    val tint = binding.btnDeletePoll.backgroundTintList
                    if (tint != null) {
                        val background = tint.getColorForState(binding.btnDeletePoll.drawableState, tint.defaultColor)
                        if (Color.alpha(background) == 255) {
                            assertTrue("Destructive button label must be legible in each theme",
                                ColorUtils.calculateContrast(binding.btnDeletePoll.currentTextColor, background) >= 4.5)
                        }
                    }
                    binding.btnDeletePoll.performClick()
                }
                assertEquals(listOf("first", "second"), deleted)
            }
        }
    }
}
