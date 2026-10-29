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
                assertEquals("Telegram ItemOptions uses PopupContextAnimation style",
                    R.style.PopupContextAnimation, popup.animationStyle)
                assertEquals("Original Telegram submenu does not consume IME insets",
                    android.widget.PopupWindow.INPUT_METHOD_NOT_NEEDED, popup.inputMethodMode)
                assertEquals("Original Telegram submenu keeps neutral soft-input state",
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED, popup.softInputMode)
                assertTrue("Original Telegram popup root owns keyboard focus",
                    popup.contentView.isFocusableInTouchMode)
                val root = popup.contentView
                assertTrue("Source-backed Telegram background must wrap the original 9-patch",
                    root.background is com.eve.app.designsystem.TelegramPopupSourceBackground)
                val source = root.background as com.eve.app.designsystem.TelegramPopupSourceBackground
                assertEquals("Original Telegram root does not fade with its backdrop",
                    1f, root.alpha, 0.001f)
                assertTrue("Unmodified nine-patch must remain the underlying source image",
                    source.sourceDrawable is android.graphics.drawable.NinePatchDrawable)
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

                val themeIcon = root.findViewById<android.widget.ImageView>(R.id.ivThemeIcon)
                assertTrue("Theme toggle must retain its official source icon",
                    themeIcon.drawable != null)
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
                    // Source geometry: ActionBarMenuSubItem.java:83-84,206-218
                    // 18dp row padding + 43dp inner title offset. TextView is
                    // attached to EVE's 24dp icon + 19dp dedicated start gap.
                    val inset = if (row.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                        row.width - label.right
                    } else {
                        label.left
                    }
                    val expected = (61f * row.resources.displayMetrics.density).toInt()
                    assertTrue("Telegram original 61dp title inset in LTR/RTL: $id ($inset vs $expected)",
                        kotlin.math.abs(inset - expected) <= (2f * row.resources.displayMetrics.density).toInt().coerceAtLeast(2))

                    assertTrue("Original Telegram menu title stays on one line: $id", label.isSingleLine)
                    assertEquals("Original Telegram menu title must end-ellipsize: $id",
                        android.text.TextUtils.TruncateAt.END, label.ellipsize)
                    assertEquals("Full title must remain readable by TalkBack: $id",
                        label.text.toString(), row.contentDescription.toString())
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
                assertTrue("Popup growth must end with full background scale",
                    kotlin.math.abs((root.background as
                        com.eve.app.designsystem.TelegramPopupSourceBackground).backScaleY - 1f) < 0.001f)
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
