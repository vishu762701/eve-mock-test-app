package com.eve.app.ui.profile

import android.content.res.Configuration
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityChangeNameBinding
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DisplayNameEditorNativeTest {
    @Test fun displayNameFieldAcceptsManyWordsWithoutOneLineClippingInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                    fontScale = 1.5f
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val binding = ActivityChangeNameBinding.inflate(LayoutInflater.from(context))
                val words = "Administrator for Exams and Community Content Management"
                binding.etName.setText(words)
                assertTrue(binding.etName.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0)
                assertTrue(binding.etName.maxLines >= 5)
                assertTrue(binding.etName.text.toString() == words)
            }
        }
    }
}
