package com.eve.app.ui.profile

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileAvatarWindowHostNativeTest {
    @Test fun profileGooeyInflatesFullWindowBehindAccessibleScrollPhotoTarget() {
        val inst = InstrumentationRegistry.getInstrumentation()
        inst.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = inst.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val ctx = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val binding = ActivityProfileBinding.inflate(LayoutInflater.from(ctx))
                assertSame(binding.root, binding.avatarGooey.parent)
                assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, binding.avatarGooey.layoutParams.height)
                assertSame(binding.avatarGooey, binding.avatarContainer.parent)
                assertSame(binding.avatarContainer, binding.ivProfilePhoto.parent)
                assertSame(binding.avatarOuterContainer, binding.avatarTouchTarget.parent)
                assertTrue(binding.avatarTouchTarget.isClickable)
                assertTrue(binding.avatarTouchTarget.isFocusable)
                assertEquals(
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                    binding.avatarGooey.importantForAccessibility
                )
                assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, binding.profileScrollView.layoutParams.height)
            }
        }
    }
}
