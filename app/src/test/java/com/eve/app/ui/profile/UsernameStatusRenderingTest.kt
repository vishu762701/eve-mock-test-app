package com.eve.app.ui.profile

import android.app.Application
import android.content.Intent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,shadows=[ProfileEditorAuthFixture.ShadowAuth::class])
class UsernameStatusRenderingTest {
    @Before fun before() { ProfileEditorAuthFixture.reset(); ProfileEditorAuthFixture.setOwner("owner-a") }
    @After fun after() { ProfileEditorAuthFixture.reset() }
    private fun exercise(block:(ChangeUsernameActivity,EditText,TextView)->Unit) {
        val intent=Intent(RuntimeEnvironment.getApplication(),ChangeUsernameActivity::class.java)
            .putExtra("current_username","ownername")
            .putExtra(ProfileEditorAccountGuard.EXTRA_OWNER_UID,"owner-a")
        val controller=Robolectric.buildActivity(ChangeUsernameActivity::class.java,intent)
        controller.get().setTheme(R.style.Theme_Eve_Profile)
        controller.setup().visible()
        try { block(controller.get(),controller.get().findViewById(R.id.etUsername),controller.get().findViewById(R.id.tvStatus)) }
        finally { controller.pause().stop().destroy() }
    }
    private fun palette(activity:ChangeUsernameActivity,night:Boolean):TelegramThemeEngine.ResolvedPalette {
        val p=TelegramThemeEngine.resolvePalette(activity,TelegramThemeEngine.BUILT_IN_THEMES[if(night)1 else 0],0)
        ReflectionHelpers.callInstanceMethod<Any?>(activity,"onApplyThemePalette",ClassParameter.from(TelegramThemeEngine.ResolvedPalette::class.java,p))
        return p
    }
    private fun readable(status:TextView,p:TelegramThemeEngine.ResolvedPalette) {
        for (background in listOf(p.surface,p.windowBackground)) {
            assertTrue("Actual status ink must retain 4.5:1 contrast: ${status.text}",ColorUtils.calculateContrast(status.currentTextColor,background)>=4.5)
        }
    }
    @Test fun availableStatusUsesReadableDayInk()=exercise { activity,_,status ->
        readable(status,palette(activity,false))
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,status.accessibilityLiveRegion)
    }
    @Test fun invalidStatusTracksDayNightWithoutRetyping()=exercise { activity,input,status ->
        input.setText("_bad")
        readable(status,palette(activity,false)); val day=status.currentTextColor
        readable(status,palette(activity,true))
        assertNotEquals("Source Day/Night error roles differ",day,status.currentTextColor)
        input.setText("abcd")
        assertTrue("EVE keeps its authoritative five-character minimum",status.text.toString().contains("5"))
    }
    @Test fun checkingStatusTracksNightBeforeDebouncedNetworkRequest()=exercise { activity,input,status ->
        input.setText("newhandle") // CHECKING is synchronous; destroy cancels the 300ms request.
        val text=status.text.toString()
        readable(status,palette(activity,true))
        assertEquals(text,status.text.toString())
        assertEquals(View.VISIBLE,status.visibility)
    }
    @Test @Config(qualifiers="hi-rIN-ldrtl-night")
    fun hindiRtlLargeFontStatusUsesLocalizedValidationAndLiveRegion() {
        RuntimeEnvironment.setFontScale(2f)
        exercise { activity,input,status ->
            input.setText("abcd")
            assertTrue(status.text.toString().contains("अक्षर"))
            assertEquals("यूज़रनेम",activity.findViewById<TextView>(R.id.tvToolbarTitle).text.toString())
            readable(status,palette(activity,true))
            assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,status.accessibilityLiveRegion)
        }
    }
}
