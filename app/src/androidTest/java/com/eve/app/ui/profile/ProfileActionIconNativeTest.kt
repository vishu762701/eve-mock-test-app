package com.eve.app.ui.profile

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileActionIconNativeTest {
    @Test fun originalTelegramLightAndDarkVariantsInflateWithoutLosingActions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val binding = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                val buttons = listOf(binding.btnSetPhoto, binding.btnEditInfo, binding.btnSettings)
                val before = buttons.map { (it.getChildAt(1) as TextView).text.toString() }
                ProfileActionIcons.applyTo(buttons, night == Configuration.UI_MODE_NIGHT_YES)
                for (button in buttons) {
                    val icon = button.getChildAt(0) as ImageView
                    assertNotNull(icon.drawable)
                    assertEquals(24f * context.resources.displayMetrics.density,
                        icon.drawable.intrinsicWidth.toFloat(), 1f)
                    assertTrue(button.isFocusable)
                    assertTrue(button.isClickable)
                }
                assertEquals(listOf("Set Photo", "Edit Info", "Settings"), before)
                assertEquals(before, buttons.map { (it.getChildAt(1) as TextView).text.toString() })
            }
        }
    }
}
