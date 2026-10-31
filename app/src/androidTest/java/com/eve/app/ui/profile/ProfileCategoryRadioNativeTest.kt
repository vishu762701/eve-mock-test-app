package com.eve.app.ui.profile

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
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
class ProfileCategoryRadioNativeTest {
    @Test fun categoryChoicesAreCompactSingleSelectedAndAccessibleAcrossThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (mode in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) for (font in listOf(1f, 1.6f)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
                    fontScale = font
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val binding = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                val cells = listOf(binding.radioGeneral, binding.radioObc, binding.radioSc, binding.radioSt)
                assertTrue(cells.all { it is TelegramProfileRadioCell })
                cells.forEach { cell ->
                    assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, cell.layoutParams.height)
                    assertTrue(cell.minimumHeight >= 50 * context.resources.displayMetrics.density)
                    assertTrue(cell.isClickable)
                    assertFalse(cell.isChecked)
                }
                cells[2].isChecked = true
                assertEquals(1, cells.count { it.isChecked })
                cells[0].isChecked = true
                cells[2].isChecked = false
                assertEquals(1, cells.count { it.isChecked })
            }
        }
    }
}
