package com.eve.app.ui.profile

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilePresenceStatusNativeTest {
    @Test fun dayNightStatusNeverFakesOnlineOnLegacyServer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val configuration = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_Eve)
                val binding = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                binding.tvStatus.text = ProfilePresenceStatus.label(null, "EV-ABC123")
                assertEquals("Eve ID: EV-ABC123", binding.tvStatus.text.toString())
                binding.tvStatus.text = ProfilePresenceStatus.label(true, "EV-ABC123")
                assertEquals("online", binding.tvStatus.text.toString())
                binding.tvStatus.text = ProfilePresenceStatus.label(false, "EV-ABC123")
                assertEquals("offline", binding.tvStatus.text.toString())
            }
        }
    }
}
