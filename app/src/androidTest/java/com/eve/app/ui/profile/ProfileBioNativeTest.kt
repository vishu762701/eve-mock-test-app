package com.eve.app.ui.profile

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityChangeBioBinding
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileBioNativeTest {
    @Test fun personalBioRendersInDayNightAndEditorEnforces160CharacterLimit() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val profile = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                ProfileBioCapability.apply(profile.cellBio, profile.tvBioValue, null)
                assertFalse("Disabled until server confirms Bio capability", profile.cellBio.isEnabled)
                ProfileBioCapability.apply(profile.cellBio, profile.tvBioValue, "")
                assertTrue("Authenticated empty Bio enables edit", profile.cellBio.isEnabled)
                assertEquals("Add bio", profile.tvBioValue.text.toString())
                ProfileBioCapability.apply(profile.cellBio, profile.tvBioValue, null)
                assertFalse("Capability loss must fail closed", profile.cellBio.isEnabled)
                assertTrue(profile.tvBioLabel.text.toString().contains("Bio"))
                val editor = ActivityChangeBioBinding.inflate(LayoutInflater.from(context))
                editor.etBio.setText("x".repeat(200))
                assertEquals(160, editor.etBio.text.length)
                assertTrue(editor.btnDone.contentDescription.toString().contains("Save bio"))
            }
        }
    }
}
