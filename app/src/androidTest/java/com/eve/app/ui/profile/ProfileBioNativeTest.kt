package com.eve.app.ui.profile

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileBioNativeTest {
    private fun containsBioLabel(view: View): Boolean {
        if (view is TextView && (view.text.toString() == "Bio" || view.text.toString() == "Add bio")) return true
        if (view is ViewGroup) for (i in 0 until view.childCount) {
            if (containsBioLabel(view.getChildAt(i))) return true
        }
        return false
    }
    @Test fun profileNeverShowsBioInDayOrNight() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val profile = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                assertNotNull(profile.cellMobile)
                assertNotNull(profile.cellUsername)
                assertNotNull(profile.cellBirthday)
                assertFalse(containsBioLabel(profile.root))
            }
        }
    }
}
