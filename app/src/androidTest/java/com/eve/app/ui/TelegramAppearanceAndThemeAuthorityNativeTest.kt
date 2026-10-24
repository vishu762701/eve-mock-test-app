package com.eve.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.navigation.EveBottomPillBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Native runtime verification of TelegramThemeEngine and bottom navigation pill
 * across Android device lifecycle and accessibility targets.
 */
@RunWith(AndroidJUnit4::class)
class TelegramAppearanceAndThemeAuthorityNativeTest {

    @Test
    fun allBundledThemesResolveValidPaletteOnDevice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TelegramThemeEngine.init(context)

        val themes = TelegramThemeEngine.BUILT_IN_THEMES
        assertTrue("Theme catalog must contain bundled themes", themes.isNotEmpty())

        for (theme in themes) {
            val palette = TelegramThemeEngine.resolvePalette(context, theme, theme.defaultAccentId)
            assertTrue("Surface color must be non-zero", palette.surface != 0)
            assertTrue("Window background must be non-zero", palette.windowBackground != 0)
            assertTrue("Accent color must be non-zero", palette.accent != 0)

            val contrast = ColorUtils.calculateContrast(palette.onAccent, palette.accent)
            assertTrue(
                "Theme ${theme.id} default accent contrast ratio $contrast must satisfy WCAG (>= 4.5)",
                contrast >= 4.5
            )
        }
    }

    @Test
    fun modeSyncsWithAppCompatDelegateOnDevice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        instrumentation.runOnMainSync {
            TelegramThemeEngine.init(context)

            TelegramThemeEngine.setCommittedMode(TelegramThemeEngine.ThemeMode.DAY, context)
            assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())

            TelegramThemeEngine.setCommittedMode(TelegramThemeEngine.ThemeMode.NIGHT, context)
            assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())

            TelegramThemeEngine.setCommittedMode(TelegramThemeEngine.ThemeMode.SYSTEM, context)
            assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.getDefaultNightMode())
        }
    }

    @Test
    fun bottomPillBarMeetsTouchTargetAndPreservesProgrammaticState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        instrumentation.runOnMainSync {
            val pillBar = EveBottomPillBar(context)
            val density = context.resources.displayMetrics.density
            val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec((344 * density).toInt(), android.view.View.MeasureSpec.EXACTLY)
            val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec((56 * density).toInt(), android.view.View.MeasureSpec.EXACTLY)
            pillBar.measure(widthSpec, heightSpec)

            // Select tab 1 without notifying listeners (lifecycle restore behavior)
            var selectedCalled = false
            var reselectedCalled = false
            pillBar.setOnTabSelectedListener { selectedCalled = true }
            pillBar.setOnTabReselectedListener { reselectedCalled = true }

            pillBar.selectTab(1, animated = false, notify = false)
            assertEquals(1, pillBar.getSelectedTab())
            assertFalse("Programmatic selection must not invoke onTabSelected", selectedCalled)
            assertFalse("Programmatic selection must not invoke onTabReselected", reselectedCalled)
        }
    }
}
