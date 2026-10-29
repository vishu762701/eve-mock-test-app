package com.eve.app.ui

import android.view.View
import android.widget.FrameLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.R
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Native host/z-order regression for the Home DrawerLayout without bottom bar. */
@RunWith(AndroidJUnit4::class)
class HomeDrawerNavigationNativeTest {
    @Test
    fun drawerOpensNormallyAndContentOccupiesWindow() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContentView(R.layout.activity_main)
                val drawer = activity.findViewById<DrawerLayout>(R.id.drawerLayout)
                val content = activity.findViewById<FrameLayout>(R.id.homeContentHost)
                val panel = activity.findViewById<View>(R.id.navDrawerPanel)
                assertSame(drawer, content.parent)
                assertSame(drawer, panel.parent)
                assertTrue("Native DrawerLayout must draw panel after content", drawer.indexOfChild(panel) > drawer.indexOfChild(content))
                val windowRoot = activity.findViewById<View>(android.R.id.content)
                val supplied = androidx.core.view.WindowInsetsCompat.Builder()
                    .setInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars(),
                        androidx.core.graphics.Insets.of(0, 24, 0, 32))
                    .build()
                androidx.core.view.ViewCompat.dispatchApplyWindowInsets(windowRoot, supplied)
                assertTrue("Drawer host must occupy the full-window inset region", windowRoot.paddingBottom == 0)
                assertTrue("Drawer must retain MATCH_PARENT", drawer.layoutParams.height == FrameLayout.LayoutParams.MATCH_PARENT)
                drawer.openDrawer(GravityCompat.START, false)
                assertTrue("Drawer must open normally", drawer.isDrawerOpen(GravityCompat.START))
            }
        }
    }
}
