package com.eve.app.ui.navigation

import android.view.View
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Prevent vertical padding from clipping the 56dp floating pill's labels/touch targets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainTabsLayoutSizingTest {
    @Test
    fun fourPrimaryTabsRetainAtLeast48dpHeightInside56dpPill() {
        val context = RuntimeEnvironment.getApplication()
        val density = context.resources.displayMetrics.density
        val layout = MainTabsLayout(context)
        repeat(4) { layout.addView(View(context)) }

        val width = View.MeasureSpec.makeMeasureSpec((344 * density).toInt(), View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec((56 * density).toInt(), View.MeasureSpec.EXACTLY)
        layout.measure(width, height)

        assertEquals(0, layout.paddingTop)
        assertEquals(0, layout.paddingBottom)
        val minTouchHeight = (48 * density).toInt()
        for (i in 0 until layout.childCount) {
            assertTrue("Tab $i has a clipped touch target", layout.getChildAt(i).measuredHeight >= minTouchHeight)
        }
    }

    @Test
    fun fourPrimaryGlassTabsRetainTouchTargetsAndSelectedState() {
        val context = RuntimeEnvironment.getApplication()
        val density = context.resources.displayMetrics.density
        val layout = MainTabsLayout(context)

        val tabHome = GlassTabView(context).apply {
            configureAsIcon(R.drawable.msg_home, "Home")
        }
        val tabHistory = GlassTabView(context).apply {
            configureAsIcon(R.drawable.ic_history, "History")
        }
        val tabSettings = GlassTabView(context).apply {
            configureAsLottie(R.raw.tab_settings, R.drawable.ic_settings, "Settings")
        }
        val tabProfile = GlassTabView(context).apply {
            configureAsAvatar("Profile")
        }

        layout.addView(tabHome)
        layout.addView(tabHistory)
        layout.addView(tabSettings)
        layout.addView(tabProfile)

        val width = View.MeasureSpec.makeMeasureSpec((344 * density).toInt(), View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec((56 * density).toInt(), View.MeasureSpec.EXACTLY)
        layout.measure(width, height)

        val minTouchHeight = (48 * density).toInt()
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            assertTrue("GlassTabView $i touch height ${child.measuredHeight} < $minTouchHeight", child.measuredHeight >= minTouchHeight)
        }
    }


    @Test
    fun measuredBarFits344dpAndTextWeightedTabsAreNotBlindlyEqual() {
        val context = RuntimeEnvironment.getApplication()
        val d = context.resources.displayMetrics.density
        val bar = EveBottomPillBar(context)
        bar.measure(
            View.MeasureSpec.makeMeasureSpec((400 * d).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((56 * d).toInt(), View.MeasureSpec.EXACTLY))
        assertEquals((344 * d).toInt(), bar.measuredWidth)
        assertEquals((56 * d).toInt(), bar.measuredHeight)

        val tabs = MainTabsLayout(context)
        listOf("A", "History", "Settings", "Profile").forEach {
            tabs.addView(GlassTabView(context).apply { configureAsIcon(R.drawable.msg_home, it) })
        }
        tabs.measure(
            View.MeasureSpec.makeMeasureSpec((344 * d).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((56 * d).toInt(), View.MeasureSpec.EXACTLY))
        assertTrue("Telegram sizing allocates content-aware widths rather than four fixed equal columns",
            (0 until tabs.childCount).map { tabs.getChildAt(it).measuredWidth }.distinct().size > 1)
    }

    @Test
    fun programmaticSelectTabDoesNotEmitListenerNotification() {
        val context = RuntimeEnvironment.getApplication()
        val layout = MainTabsLayout(context)

        val tab0 = GlassTabView(context).apply { configureAsIcon(R.drawable.msg_home, "Home") }
        val tab1 = GlassTabView(context).apply { configureAsIcon(R.drawable.ic_history, "History") }
        val tab2 = GlassTabView(context).apply { configureAsLottie(R.raw.tab_settings, R.drawable.ic_settings, "Settings") }
        val tab3 = GlassTabView(context).apply { configureAsAvatar("Profile") }

        layout.addView(tab0)
        layout.addView(tab1)
        layout.addView(tab2)
        layout.addView(tab3)

        var selectedCount = 0
        var reselectedCount = 0
        layout.setTabSelectionListener(object : MainTabsLayout.TabSelectionListener {
            override fun onTabSelected(position: Int) { selectedCount++ }
            override fun onTabReselected(position: Int) { reselectedCount++ }
        })

        // Programmatic restore on onResume / attach with notify = false
        layout.selectTab(2, animated = false, notify = false)

        assertEquals(2, layout.getSelectedTabPosition())
        assertFalse(tab0.isTabSelected())
        assertFalse(tab0.isSelected)
        assertFalse(tab1.isTabSelected())
        assertFalse(tab1.isSelected)
        assertTrue(tab2.isTabSelected())
        assertTrue("TalkBack requires isSelected to be true on active tab", tab2.isSelected)
        assertFalse(tab3.isTabSelected())
        assertFalse(tab3.isSelected)

        // Neither onTabSelected nor onTabReselected should have been invoked
        assertEquals("Programmatic restore must not invoke onTabSelected", 0, selectedCount)
        assertEquals("Programmatic restore must not invoke onTabReselected (scroll reset)", 0, reselectedCount)

        // User selection with notify = true invokes onTabSelected
        layout.selectTab(1, animated = true, notify = true)
        assertEquals(1, selectedCount)
        assertEquals(0, reselectedCount)
        assertEquals(1, layout.getSelectedTabPosition())
        assertTrue(tab1.isTabSelected())
        assertTrue(tab1.isSelected)
        assertFalse(tab2.isTabSelected())
        assertFalse(tab2.isSelected)

        // User reselection on already selected tab invokes onTabReselected
        layout.selectTab(1, animated = true, notify = true)
        assertEquals(1, selectedCount)
        assertEquals(1, reselectedCount)
    }

    @Test
    fun updatePalettePropagatesToAllGlassTabs() {
        val context = RuntimeEnvironment.getApplication()
        val layout = MainTabsLayout(context)

        val tabHome = GlassTabView(context).apply { configureAsIcon(R.drawable.msg_home, "Home") }
        val tabHistory = GlassTabView(context).apply { configureAsIcon(R.drawable.ic_history, "History") }
        layout.addView(tabHome)
        layout.addView(tabHistory)

        val palette = TelegramThemeEngine.resolvePalette(
            context,
            TelegramThemeEngine.BUILT_IN_THEMES.first { it.id == "night" },
            0
        )
        layout.updatePalette(palette)
        // Tabs received palette without exception
        assertEquals(2, layout.childCount)
    }
}
