package com.eve.app.ui.profile

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/**
 * End-to-end inflation and behavior regression across the original camera host,
 * source action geometry, Bio feature gate, theme icons and collapsed click gate.
 * A native emulator executes this in FULL; FAST compilation alone is insufficient.
 */
@RunWith(AndroidJUnit4::class)
class ProfileMissionIntegrationNativeTest {
    @Test fun sourceMappedProfileRowWorksWithDayNightRtlAndLargeText() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
                    val base = instrumentation.targetContext
                    val config = Configuration(base.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                        fontScale = 1.35f
                    }
                    val context = ContextThemeWrapper(
                        base.createConfigurationContext(config), R.style.Theme_Eve
                    )
                    val b = ActivityProfileBinding.inflate(LayoutInflater.from(context))
                    b.root.layoutDirection = direction
                    val width = (360f * context.resources.displayMetrics.density).roundToInt()
                    val height = (720f * context.resources.displayMetrics.density).roundToInt()
                    b.root.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                    )
                    b.root.layout(0, 0, width, height)
                    assertEquals("Camera host must match window width", width, b.avatarGooey.width)
                    assertEquals("Camera host must match window height", height, b.avatarGooey.height)
                    assertEquals(width, b.profileScrollView.width)
                    assertFalse("Old Worker must not expose a misleading Bio editor", b.cellBio.isEnabled)
                    assertEquals("Bio", b.tvBioLabel.text.toString())

                    val buttons = listOf(b.btnSetPhoto, b.btnEditInfo, b.btnSettings)
                    ProfileActionAccessibility.install(buttons)
                    val surface = ContextCompat.getColor(context, R.color.category_pill_unselected_bg)
                    ProfileActionIcons.applyTo(buttons, surface)
                    assertEquals(night == Configuration.UI_MODE_NIGHT_NO,
                        ProfileActionIcons.isLightButtonColor(surface))
                    for (button in buttons) {
                        assertTrue(button.width > 0)
                        assertNotNull((button.getChildAt(0) as ImageView).drawable)
                        assertTrue(button.isClickable)
                    }
                    val density = context.resources.displayMetrics.density
                    ProfileActionsCollapse.applyTo(buttons, ProfileActionsCollapse.atScroll(0f, density))
                    assertTrue(buttons.all { it.isEnabled })
                    ProfileActionsCollapse.applyTo(buttons, ProfileActionsCollapse.atScroll(100f * density, density))
                    assertTrue(buttons.none { it.isEnabled })
                }
            }
        }
    }
}
