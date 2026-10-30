package com.eve.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.databinding.ActivityTestBinding
import com.eve.app.ui.test.TestPrimaryActionAppearance
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestPrimaryActionMonochromeNativeTest {
    @Test fun ownerMonochromeIsOpaqueAndScopedToNextActionInBothModes() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val binding = ActivityTestBinding.inflate(LayoutInflater.from(activity))
                activity.setContentView(binding.root)
                val untouchedSecondary = binding.btnPrev.backgroundTintList!!.defaultColor
                for (night in listOf(false, true, false)) {
                    TestPrimaryActionAppearance.apply(binding.btnNext, night)
                    val bg = if (night) Color.WHITE else Color.BLACK
                    val fg = if (night) Color.BLACK else Color.WHITE
                    val selectedState = intArrayOf(android.R.attr.state_enabled)
                    assertEquals(bg, binding.btnNext.backgroundTintList!!.getColorForState(selectedState, 0))
                    assertEquals(fg, binding.btnNext.textColors.getColorForState(selectedState, 0))
                    assertEquals(255, Color.alpha(bg))
                    binding.btnNext.isEnabled = false
                    assertEquals(if (night) Color.WHITE else Color.BLACK, binding.btnNext.currentTextColor)
                    binding.btnNext.isEnabled = true
                    assertEquals("R08 must not recolor Previous", untouchedSecondary,
                        binding.btnPrev.backgroundTintList!!.defaultColor)
                }
            }
        }
    }
}
