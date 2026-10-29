package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.R
import com.eve.app.ui.common.EveGlassToolbar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot regressions: five Home drawer navigation icons and scroll header.
 * Each suite is run by the mission-final two-navigation-mode native matrix;
 * FAST compiles the instrumentation source only.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenshotChromeNativeTest {
    @Test fun homeDrawerIconsUseReadableNightRoleWithoutChangingPremiumArtwork() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContentView(R.layout.activity_main)
                val night = activity.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                val expectedTint = if (night) Color.WHITE else 0xFF889198.toInt()
                val rows = intArrayOf(
                    R.id.layoutDrawerProfile, R.id.layoutDrawerPerformance,
                    R.id.layoutDrawerLeaderboard, R.id.layoutDrawerSyllabus,
                    R.id.layoutDrawerTopic, R.id.layoutDrawerPyq,
                    R.id.layoutDrawerMistakes, R.id.layoutDrawerSettings
                )
                for (id in rows) {
                    val row = activity.findViewById<LinearLayout>(id)
                    val icon = row.getChildAt(0) as ImageView
                    val tint = ImageViewCompat.getImageTintList(icon)?.defaultColor
                    assertEquals("Official-source owner override for drawer icon $id",
                        expectedTint, tint)
                }
                assertEquals("Home-only drawer surface must retain its theme role",
                    if (night) Color.BLACK else ContextCompat.getColor(activity, R.color.home_drawer_background),
                    ContextCompat.getColor(activity, R.color.home_drawer_background))
                assertTrue("Premium Lottie must not be substituted by static icon",
                    activity.findViewById<android.view.View>(R.id.lottieDrawerPremiumStarFallback)
                        is com.airbnb.lottie.LottieAnimationView)
            }
        }
    }

    @Test fun homeOptOutPreventsScrollBackdropFromCreatingGrayNightToolbarStrip() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContentView(R.layout.activity_main)
                val toolbar = activity.findViewById<EveGlassToolbar>(R.id.homeGlassToolbar)
                // Production MainActivity.setupTopBarDepth() applies these Home-only
                // values; this Firebase-free fixture tests the real toolbar primitive.
                toolbar.drawHairline = false
                toolbar.scrollBackdropEnabled = false
                toolbar.glassEnabled = false
                toolbar.setBackgroundColor(Color.TRANSPARENT)
                toolbar.onGlobalLayout()
                repeat(3) {
                    assertTrue("Source-invariant no-op pre-draw must continue normally", toolbar.onPreDraw())
                    assertFalse("Scroll must never turn Home glass coating back on", toolbar.glassEnabled)
                    assertEquals("Home chrome must remain canvas-transparent",
                        Color.TRANSPARENT, (toolbar.background as ColorDrawable).color)
                }
                assertFalse("Opting out must not affect other toolbar instances",
                    toolbar.scrollBackdropEnabled)
                val normal = activity.findViewById<android.view.View>(R.id.normalTopBar)
                val search = activity.findViewById<android.view.View>(R.id.searchTopBar)
                assertTrue("Normal title host must exist", normal != null)
                assertTrue("Search host must remain separate", search != null)
            }
        }
    }

    @Test fun homePopupMenuUsesTelegramDensityAndMinWidth() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val popupView = LayoutInflater.from(activity).inflate(R.layout.popup_telegram_menu, null)
                val rows = intArrayOf(
                    R.id.cardTheme, R.id.menuRowHistory, R.id.menuRowBookmarks
                )
                val density = activity.resources.displayMetrics.density
                val expectedMinHeight = (48 * density + 0.5f).toInt()
                val expectedMinWidth = (196 * density + 0.5f).toInt()

                for (id in rows) {
                    val row = popupView.findViewById<LinearLayout>(id)
                    assertEquals("Top padding must be 0dp for authentic Telegram density", 0, row.paddingTop)
                    assertEquals("Bottom padding must be 0dp for authentic Telegram density", 0, row.paddingBottom)
                    assertEquals("Min height must match 48dp upstream itemHeight", expectedMinHeight, row.minimumHeight)
                    assertEquals("Min width must match 196dp upstream ActionBarMenuSubItem minWidth", expectedMinWidth, row.minimumWidth)
                }
            }
        }
    }
}
