package com.eve.app.ui.profile

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.ui.ChangeNameActivity as TelegramChangeNameActivity

@RunWith(AndroidJUnit4::class)
class DisplayNameEditorNativeTest {
    @Test fun displayNameFieldRendersInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                    fontScale = 1.5f
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val fragment = TelegramChangeNameActivity(null)
                val view = fragment.createView(context)
                assertNotNull(view)
                assertNotNull(fragment.actionBar)
            }
        }
    }
}
