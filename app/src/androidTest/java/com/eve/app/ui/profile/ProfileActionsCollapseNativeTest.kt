package com.eve.app.ui.profile

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileActionsCollapseNativeTest {
    @Test fun dayAndNightActionCellsUseSeparateSourceAlphaStagesAndTouchGate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val b = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                val buttons = listOf(b.btnSetPhoto, b.btnEditInfo, b.btnSettings)
                val density = context.resources.displayMetrics.density

                val mid = ProfileActionsCollapse.atScroll(20f * density, density)
                ProfileActionsCollapse.applyTo(buttons, mid)
                buttons.forEach { button ->
                    assertTrue(button.isEnabled)
                    assertEquals((mid.backgroundAlpha * 255f).toInt().toFloat(),
                        button.background.alpha.toFloat(), 1f)
                    for (index in 0 until button.childCount) {
                        assertEquals(mid.contentAlpha, button.getChildAt(index).alpha, 0.001f)
                    }
                }

                ProfileActionsCollapse.applyTo(
                    buttons, ProfileActionsCollapse.atScroll(74f * density, density)
                )
                buttons.forEach { button ->
                    assertFalse(button.isEnabled)
                    assertEquals(0, button.background.alpha)
                }

                ProfileActionsCollapse.applyTo(
                    buttons, ProfileActionsCollapse.atScroll(0f, density)
                )
                buttons.forEach { button ->
                    assertTrue(button.isEnabled)
                    assertEquals(255, button.background.alpha)
                    for (index in 0 until button.childCount) {
                        assertEquals(1f, button.getChildAt(index).alpha, 0.001f)
                    }
                }
            }
        }
    }
}
