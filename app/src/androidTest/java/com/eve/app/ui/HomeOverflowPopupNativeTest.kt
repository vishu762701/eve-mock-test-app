package com.eve.app.ui

import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.ui.home.TelegramMenuPopup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the actual PopupWindow, not only inflated XML or unbounded layout metrics.
 * Uses a credentials-free debug Activity; does not trigger logout or exam operations.
 * Mission 05 final FULL gate intentionally reruns both native navigation modes.
 * Final repaired check covers animated and settled visual bounds in both modes.
 */
@RunWith(AndroidJUnit4::class)
class HomeOverflowPopupNativeTest {
    @Test
    fun allSevenRowsRemainInsideVisibleWindowAndScroll() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            lateinit var popup: TelegramMenuPopup
            lateinit var anchor: View
            scenario.onActivity { activity ->
                val host = FrameLayout(activity)
                val dp = activity.resources.displayMetrics.density
                anchor = ImageButton(activity).apply {
                    contentDescription = "Home overflow anchor"
                    setImageResource(R.drawable.ic_more_vert)
                }
                host.addView(anchor, FrameLayout.LayoutParams((48 * dp).toInt(), (48 * dp).toInt()).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = (80 * dp).toInt()
                })
                activity.setContentView(host)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                popup = TelegramMenuPopup(activity, { _, _, _, _ -> }, {}, {}, {}, {}, {}, {})
                popup.show(anchor)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertTrue("Home popup must be attached", popup.isShowing)
                val root = popup.contentView
                assertTrue("Original Telegram nine-patch should back Home popup",
                    root.background is android.graphics.drawable.NinePatchDrawable)
                val frame = Rect().also(anchor::getWindowVisibleDisplayFrame)
                // PopupWindow has its own window coordinate origin. getGlobalVisibleRect()
                // returns that window's local coordinates (often 0,0), not display coordinates.
                // Compare against the anchor's screen-space display frame using the popup's
                // actual screen position and full measured size so clipping cannot hide overflow.
                val popupLocation = IntArray(2)
                root.getLocationOnScreen(popupLocation)
                assertTrue("Popup root must have nonzero measured dimensions",
                    root.width > 0 && root.height > 0 && root.isAttachedToWindow)
                // getLocationOnScreen incorporates the active scale transform, whereas
                // width/height are unscaled layout dimensions. Using both unchanged
                // mixes two coordinate systems during the 220ms anchored entrance.
                val visualWidth = kotlin.math.round(root.width * root.scaleX).toInt()
                val visualHeight = kotlin.math.round(root.height * root.scaleY).toInt()
                val bounds = Rect(popupLocation[0], popupLocation[1],
                    popupLocation[0] + visualWidth, popupLocation[1] + visualHeight)
                val tolerance = (2 * root.resources.displayMetrics.density).toInt().coerceAtLeast(2)
                assertTrue("Animated popup extends outside available horizontal viewport: $bounds vs $frame",
                    bounds.left >= frame.left - tolerance && bounds.right <= frame.right + tolerance)
                assertTrue("Animated popup extends outside available vertical viewport: $bounds vs $frame",
                    bounds.top >= frame.top - tolerance && bounds.bottom <= frame.bottom + tolerance)

                val scroll = root.findViewById<ScrollView>(R.id.menuScroll)
                val rows = intArrayOf(R.id.cardTheme, R.id.menuRowHistory, R.id.menuRowBookmarks,
                    R.id.menuRowMistakes, R.id.menuRowTopic, R.id.menuRowPyq, R.id.menuRowLogout)
                for (id in rows) {
                    val row = root.findViewById<View>(id)
                    assertTrue("Source-isolated Telegram radial ripple must be assigned to each action: $id",
                        row.background is android.graphics.drawable.RippleDrawable && row.foreground == null)
                    assertTrue("Missing or zero-height popup row: $id", row.width > 0 && row.height > 0)
                    assertTrue("Row must be at least 48dp: $id",
                        row.height >= (48 * root.resources.displayMetrics.density).toInt() - 1)
                    val label = (row as android.view.ViewGroup).getChildAt(1) as TextView
                    assertTrue("Popup label may not be cropped to zero width: $id", label.width > 0)
                }
                scroll.scrollTo(0, scroll.getChildAt(0).height)
                assertTrue("Last popup destination remains mounted", root.findViewById<View>(R.id.menuRowLogout).isAttachedToWindow)
            }
            // Verify the full final size separately, after the real entrance animation
            // has settled. This does not disable animation or bypass viewport checks.
            android.os.SystemClock.sleep(350)
            scenario.onActivity {
                val root = popup.contentView
                val frame = Rect().also(anchor::getWindowVisibleDisplayFrame)
                val screen = IntArray(2).also(root::getLocationOnScreen)
                val bounds = Rect(screen[0], screen[1], screen[0] + root.width, screen[1] + root.height)
                val tolerance = (2 * root.resources.displayMetrics.density).toInt().coerceAtLeast(2)
                assertTrue("Original source row entrance must settle without whole-popup scaling",
                    kotlin.math.abs(root.scaleX - 1f) < 0.001f &&
                    kotlin.math.abs(root.scaleY - 1f) < 0.001f)
                for (id in intArrayOf(R.id.cardTheme, R.id.menuRowHistory,
                        R.id.menuRowBookmarks, R.id.menuRowMistakes, R.id.menuRowTopic,
                        R.id.menuRowPyq, R.id.menuRowLogout)) {
                    val row = root.findViewById<View>(id)
                    assertTrue("Original row cascade must end at zero Y and full alpha: $id",
                        kotlin.math.abs(row.translationY) < 0.01f &&
                        kotlin.math.abs(row.alpha - 1f) < 0.01f)
                }
                assertTrue("Settled popup horizontal bounds must remain safe: $bounds vs $frame",
                    bounds.left >= frame.left - tolerance && bounds.right <= frame.right + tolerance)
                assertTrue("Settled popup vertical bounds must remain safe: $bounds vs $frame",
                    bounds.top >= frame.top - tolerance && bounds.bottom <= frame.bottom + tolerance)
                popup.dismiss()
            }
        }
    }
}
