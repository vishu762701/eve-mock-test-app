package com.eve.app.ui

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.home.HomeAppearance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runtime AA verification for whichever Telegram Day/Night theme is active.
 * Runs on the terminal FULL native matrix; FAST compiles the test source only.
 * Source role constants: Telegram ThemeColors.java:15,211-216;
 * source indicator: FilterTabsView.java:1448-1514, alpha=31.
 */
@RunWith(AndroidJUnit4::class)
class HomeTelegramFilterContrastNativeTest {
    @Test fun homeSelectedAndIdleTabTextAreAaReadableOnRealThemeBackdrops() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val palette = TelegramThemeEngine.resolveCurrentPalette(activity)
                val selected = HomeAppearance.categoryChip(activity, "History", "History") {}
                val idle = HomeAppearance.categoryChip(activity, "Geography", "History") {}
                assertTrue(selected.isChecked)
                assertTrue(!idle.isChecked)
                assertTrue(!selected.isCheckedIconVisible && !idle.isCheckedIconVisible)
                assertEquals("Checked source selector is behind transparent tab",
                    Color.TRANSPARENT, selected.chipBackgroundColor?.getColorForState(
                        selected.drawableState, Color.BLACK))
                assertEquals("Idle tab must not draw a second opaque selected pill",
                    Color.TRANSPARENT, idle.chipBackgroundColor?.getColorForState(
                        idle.drawableState, Color.BLACK))
                for (backdrop in listOf(palette.surface, palette.windowBackground)) {
                    val selectedBackdrop = ColorUtils.blendARGB(
                        backdrop, palette.filterTabLine, 31f / 255f
                    )
                    val selectedContrast = ColorUtils.calculateContrast(
                        selected.currentTextColor, selectedBackdrop
                    )
                    val idleContrast = ColorUtils.calculateContrast(
                        idle.currentTextColor, backdrop
                    )
                    assertTrue("Selected Home tab must meet WCAG AA: $selectedContrast",
                        selectedContrast >= 4.49)
                    assertTrue("Idle Home tab must meet WCAG AA: $idleContrast",
                        idleContrast >= 4.49)
                }
                assertEquals("Selected contentDescription must name its category",
                    "History", selected.contentDescription)
                assertEquals("Idle contentDescription must name its category",
                    "Geography", idle.contentDescription)
            }
        }
    }
}
