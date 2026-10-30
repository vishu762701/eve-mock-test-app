package com.eve.app.ui.common

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.R
import com.eve.app.ui.RepairVerificationActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** R07. Green describes the student's selected answer, never correctness. */
@RunWith(AndroidJUnit4::class)
class TelegramAnswerGreenNativeTest {
    @Test fun selectedAndRestoredOptionUsesGreenWithoutTouchingAnswerState() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val option = TelegramRadioButton(activity)
                activity.setContentView(option)
                val greenSurface = ContextCompat.getColor(activity, R.color.system_green_container)
                val greenIndicator = ContextCompat.getColor(activity, R.color.system_green_text)
                option.isChecked = true
                val checked = option.background.current as GradientDrawable
                assertEquals(greenSurface, checked.color?.defaultColor)
                assertEquals(ContextCompat.getColor(activity, R.color.text_primary),
                    option.textColors.getColorForState(intArrayOf(android.R.attr.state_checked), -1))
                assertEquals(greenIndicator, option.indicator?.checkedColor)
                option.isChecked = false
                assertNotEquals(greenSurface, (option.background.current as GradientDrawable).color?.defaultColor)
                option.isChecked = true
                assertEquals(greenSurface, (option.background.current as GradientDrawable).color?.defaultColor)
                assertTrue(option.isChecked)

                // Theme-preview callbacks can arrive before Configuration/qualifier
                // recreation. Both directions must rebind the checked indicator.
                val current = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(activity)
                for (night in listOf(true, false, true)) {
                    option.updatePalette(current.copy(isDark = night))
                    assertEquals(
                        if (night) 0xFF30D158.toInt() else 0xFF1F7735.toInt(),
                        option.indicator?.checkedColor)
                    assertTrue(option.isChecked)
                }
            }
        }
    }
}
