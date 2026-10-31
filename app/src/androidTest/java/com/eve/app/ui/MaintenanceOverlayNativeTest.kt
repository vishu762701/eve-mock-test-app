package com.eve.app.ui

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.ScrollView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityMainBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaintenanceOverlayNativeTest {
    @Test fun longHindiAndRtlMessagesRemainScrollableAndRetryReachableInBothThemes() {
        val inst = InstrumentationRegistry.getInstrumentation()
        inst.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = inst.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                    fontScale = 2f
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val binding = ActivityMainBinding.inflate(LayoutInflater.from(context))
                val overlay = binding.maintenanceOverlay
                assertTrue(overlay is ScrollView)
                overlay.visibility = View.VISIBLE
                overlay.layoutDirection = View.LAYOUT_DIRECTION_RTL
                binding.tvMaintenanceMessage.text = "रखरखाव जारी है। कृपया फिर प्रयास करें। ".repeat(25)
                val w = (320 * context.resources.displayMetrics.density).toInt()
                val h = (480 * context.resources.displayMetrics.density).toInt()
                overlay.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                overlay.layout(0, 0, w, h)
                assertNotNull(overlay.background)
                assertEquals(255, overlay.background.alpha)
                assertTrue(overlay.isClickable)
                assertTrue(overlay.getChildAt(0).height > overlay.height)
                overlay.fullScroll(View.FOCUS_DOWN)
                var retries = 0
                binding.btnCheckMaintenanceAgain.setOnClickListener { retries++ }
                binding.btnCheckMaintenanceAgain.performClick()
                assertEquals(1, retries)
                assertTrue(binding.btnCheckMaintenanceAgain.height >= (48 * context.resources.displayMetrics.density).toInt())
            }
        }
    }
}
