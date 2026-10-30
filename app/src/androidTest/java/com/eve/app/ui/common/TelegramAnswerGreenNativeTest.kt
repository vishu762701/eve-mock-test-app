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
                val green = ContextCompat.getColor(activity, R.color.system_green)
                option.isChecked = true
                val checked = option.background.current as GradientDrawable
                assertEquals(green, checked.color?.defaultColor)
                assertEquals(ContextCompat.getColor(activity, R.color.solid_black),
                    option.textColors.getColorForState(intArrayOf(android.R.attr.state_checked), -1))
                assertEquals(ContextCompat.getColor(activity, R.color.solid_black),
                    option.indicator?.checkedColor)
                option.isChecked = false
                assertNotEquals(green, (option.background.current as GradientDrawable).color?.defaultColor)
                option.isChecked = true
                assertEquals(green, (option.background.current as GradientDrawable).color?.defaultColor)
                assertTrue(option.isChecked)
            }
        }
    }
}
