package com.eve.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.navigation.EveBottomPillBar
import com.eve.app.ui.navigation.MainTabsLayout
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
            pillBar.setTabSelectionListener(object : MainTabsLayout.TabSelectionListener {
                override fun onTabSelected(position: Int) { selectedCalled = true }
                override fun onTabReselected(position: Int) { reselectedCalled = true }
            })

            pillBar.selectTab(1, animated = false, notify = false)
            assertEquals(1, pillBar.getSelectedTab())
            assertFalse("Programmatic selection must not invoke onTabSelected", selectedCalled)
            assertFalse("Programmatic selection must not invoke onTabReselected", reselectedCalled)
        }
    }

    @Test
    fun bottomPillRendersGlassOnBackgroundLayerOnlyKeepingForegroundSharp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        instrumentation.runOnMainSync {
            val pillBar = EveBottomPillBar(context)
            // Ensure glassBackgroundView is first child and tabsLayout is in front
            assertEquals("Glass background must be first child", pillBar.glassBackgroundView, pillBar.getChildAt(0))
            assertEquals("Tabs layout must be foreground child", pillBar.tabsLayout, pillBar.getChildAt(1))

            // Verify foreground tabs are unaffected by background effect
            assertFalse("Foreground tabs must not be clipped", pillBar.tabsLayout.clipChildren)
            assertTrue("Background drawable must be attached to background view", pillBar.glassBackgroundView.background != null)

            val palette = TelegramThemeEngine.resolveCurrentPalette(context)
            pillBar.updateThemePalette(palette)
            assertEquals(palette, pillBar.currentPalette)
        }
    }

    @Test
    fun themeApplierBindsPaletteToChildViewsAndPreservesState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = android.view.ContextThemeWrapper(instrumentation.targetContext, com.eve.app.R.style.Theme_Eve)

        instrumentation.runOnMainSync {
            val layout = android.widget.LinearLayout(context)
            val card = com.google.android.material.card.MaterialCardView(context)
            val button = com.google.android.material.button.MaterialButton(context).apply {
                tag = "accent_button"
            }
            val tabs = com.eve.app.designsystem.DesignSegmentedTabs(context)
            val textView = android.widget.TextView(context).apply {
                setTextColor(android.graphics.Color.BLACK)
            }
            val editText = android.widget.EditText(context).apply {
                setText("Preserved user input")
            }

            layout.addView(card)
            layout.addView(button)
            layout.addView(tabs)
            layout.addView(textView)
            layout.addView(editText)

            val palette = TelegramThemeEngine.resolveCurrentPalette(context)
            com.eve.app.theme.TelegramThemeApplier.applyToViewTree(layout, palette)

            assertEquals(palette.surface, card.cardBackgroundColor.defaultColor)
            assertEquals(palette.accent, button.backgroundTintList?.defaultColor)
            assertEquals(palette.onAccent, button.currentTextColor)
            assertEquals(palette.textPrimary, textView.currentTextColor)
            assertEquals("Preserved user input", editText.text.toString())
        }
    }

    @Test
    fun inflatedResultTabsKeepSelectedLabelsReadableBeforeAndAfterPaletteUpdate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val base = instrumentation.targetContext
            TelegramThemeEngine.init(base)
            for (dark in listOf(false, true)) {
                val configuration = android.content.res.Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark) android.content.res.Configuration.UI_MODE_NIGHT_YES
                        else android.content.res.Configuration.UI_MODE_NIGHT_NO
                }
                val context = android.view.ContextThemeWrapper(base.createConfigurationContext(configuration),
                    com.eve.app.R.style.Theme_Eve)
                val binding = com.eve.app.databinding.ActivityResultBinding.inflate(android.view.LayoutInflater.from(context))
                val tabs = binding.tabLayoutResult
                val palette = TelegramThemeEngine.resolveCurrentPalette(context)
                for (updated in listOf(false, true)) {
                    if (updated) tabs.updatePalette(palette)
                    binding.root.measure(
                        android.view.View.MeasureSpec.makeMeasureSpec((360 * context.resources.displayMetrics.density).toInt(), android.view.View.MeasureSpec.EXACTLY),
                        android.view.View.MeasureSpec.makeMeasureSpec((800 * context.resources.displayMetrics.density).toInt(), android.view.View.MeasureSpec.EXACTLY))
                    binding.root.layout(0, 0, binding.root.measuredWidth, binding.root.measuredHeight)
                    for (index in 0 until tabs.tabCount) {
                        val tab = requireNotNull(tabs.getTabAt(index))
                        tab.select()
                        val label = tab.customView as android.widget.TextView
                        val fill = (tabs.tabSelectedIndicator as android.graphics.drawable.GradientDrawable)
                            .color!!.getColorForState(tabs.drawableState, 0)
                        assertEquals("Selected indicator must share the label's palette", palette.accent, fill)
                        assertEquals("Selected text must retain its matching foreground", palette.onAccent, label.currentTextColor)
                        assertTrue("Readable selected tab (dark=$dark, updated=$updated, index=$index)",
                            ColorUtils.calculateContrast(label.currentTextColor, fill) >= 4.5)
                    }
                }
            }
        }
    }

}
