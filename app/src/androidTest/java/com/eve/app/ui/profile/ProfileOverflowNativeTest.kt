package com.eve.app.ui.profile

import android.graphics.Rect
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.databinding.ActivityProfileBinding
import com.eve.app.ui.RepairVerificationActivity
import com.eve.app.ui.profile.popup.ActionBarPopupWindow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises real windows and callbacks; no Firebase account or network fixture. */
@RunWith(AndroidJUnit4::class)
class ProfileOverflowNativeTest {
    private fun sourceWindow(popup: ProfileOverflowPopup): ActionBarPopupWindow =
        ProfileOverflowPopup::class.java.getDeclaredField("window").apply { isAccessible = true }
            .get(popup) as ActionBarPopupWindow

    @Test fun publicPlatformAnchorTrackingFollowsScrollAndDetachesOnDismiss() {
        val inst = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            lateinit var scroll: android.widget.ScrollView
            lateinit var popup: ProfileOverflowPopup
            lateinit var anchor: android.widget.ImageButton
            var movement = 0
            var initialY = 0
            var initialAnchorY = 0
            scenario.onActivity { activity ->
                val density = activity.resources.displayMetrics.density
                movement = (40 * density).toInt()
                scroll = android.widget.ScrollView(activity)
                // ScrollView measures its child UNSPECIFIED vertically: a fixed
                // LayoutParams height alone does not establish scroll extent.
                val content = android.widget.FrameLayout(activity).apply {
                    minimumHeight = (2000 * density).toInt()
                }
                anchor = android.widget.ImageButton(activity)
                content.addView(anchor, android.widget.FrameLayout.LayoutParams(
                    (48 * density).toInt(), (48 * density).toInt()).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                    topMargin = (200 * density).toInt()
                })
                scroll.addView(content, android.widget.FrameLayout.LayoutParams(-1, -2))
                activity.setContentView(scroll)
            }
            inst.waitForIdleSync()
            scenario.onActivity {
                popup = ProfileOverflowPopup(anchor)
                popup.show(true, {}, {})
            }
            android.os.SystemClock.sleep(400)
            scenario.onActivity {
                assertTrue(popup.isShowing)
                initialY = IntArray(2).also(sourceWindow(popup).contentView::getLocationOnScreen)[1]
                initialAnchorY = IntArray(2).also(anchor::getLocationOnScreen)[1]
                scroll.scrollTo(0, movement)
            }
            android.os.SystemClock.sleep(160)
            inst.waitForIdleSync()
            scenario.onActivity {
                assertEquals("The fixture must actually perform the requested scroll", movement, scroll.scrollY)
                val anchorY = IntArray(2).also(anchor::getLocationOnScreen)[1]
                assertEquals("The anchor itself must move before testing popup tracking",
                    (initialAnchorY - movement).toFloat(), anchorY.toFloat(), 2f)
                val afterY = IntArray(2).also(sourceWindow(popup).contentView::getLocationOnScreen)[1]
                assertEquals("Public PopupWindow anchor tracking must follow a real scroll",
                    (initialY - movement).toFloat(), afterY.toFloat(), 2f)
                popup.dismiss(immediate = true)
                scroll.scrollTo(0, movement * 2)
            }
            inst.waitForIdleSync()
            scenario.onActivity {
                assertFalse(popup.isShowing)
                popup.show(true, {}, {})
            }
            android.os.SystemClock.sleep(400)
            scenario.onActivity {
                assertTrue("The same owner can reopen after public anchor detachment", popup.isShowing)
                popup.dismiss(immediate = true)
            }
        }
    }

    @Test fun actualMenuAnchorsDispatchesDismissesAndReopens() {
        val inst = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            lateinit var b: ActivityProfileBinding
            lateinit var popup: ProfileOverflowPopup
            var usernameCalls = 0
            var copyCalls = 0
            scenario.onActivity { activity ->
                b = ActivityProfileBinding.inflate(activity.layoutInflater)
                activity.setContentView(b.root)
            }
            inst.waitForIdleSync()
            for (collapsed in listOf(false, true)) {
                scenario.onActivity {
                    // Real category options provide content taller than this
                    // fixture viewport; do not label a clamped scroll as motion.
                    b.layoutCategoryOptions.visibility = if (collapsed) View.VISIBLE else View.GONE
                }
                inst.waitForIdleSync()
                android.os.SystemClock.sleep(120)
                scenario.onActivity {
                    b.profileScrollView.scrollTo(0, if (collapsed) 1000 else 0)
                    if (collapsed) assertTrue("Exercise a real scrolled body", b.profileScrollView.scrollY > 0)
                    else assertEquals(0, b.profileScrollView.scrollY)
                    popup = ProfileOverflowPopup(b.btnProfileMenu)
                    popup.show(true, { usernameCalls++ }, { copyCalls++ })
                }
                android.os.SystemClock.sleep(400)
                scenario.onActivity {
                    assertTrue(popup.isShowing)
                    val window = sourceWindow(popup)
                    val root = window.contentView as ActionBarPopupWindow.ActionBarPopupWindowLayout
                    assertNotNull("Original source swipe-back parent is mounted", root.swipeBack)
                    assertTrue(root.backgroundDrawable is android.graphics.drawable.NinePatchDrawable)
                    assertEquals(2, root.itemsCount)
                    assertEquals(1f, root.backScaleY, .001f)
                    val frame = Rect().also(b.btnProfileMenu::getWindowVisibleDisplayFrame)
                    val xy = IntArray(2).also(root::getLocationOnScreen)
                    assertTrue(xy[0] >= frame.left && xy[0] + root.width <= frame.right)
                    assertTrue(xy[1] >= frame.top && xy[1] + root.height <= frame.bottom)
                    for (i in 0 until root.itemsCount) {
                        val row = root.getItemAt(i)
                        assertTrue(row.isEnabled)
                        assertTrue(row.height >= 48 * row.resources.displayMetrics.density)
                        assertEquals(1f, row.alpha, .001f)
                    }
                    // Actual source row callback, not a duplicate test handler.
                    root.getItemAt(if (collapsed) 1 else 0).performClick()
                }
                android.os.SystemClock.sleep(200)
                scenario.onActivity { assertFalse(popup.isShowing) }
            }
            assertEquals(1, usernameCalls)
            assertEquals(1, copyCalls)
            scenario.onActivity { popup.show(false, { usernameCalls++ }, { copyCalls++ }) }
            android.os.SystemClock.sleep(400)
            scenario.onActivity {
                val root = sourceWindow(popup).contentView as ActionBarPopupWindow.ActionBarPopupWindowLayout
                assertFalse("Absent username cannot copy a fabricated link", root.getItemAt(1).isEnabled)
            }
            inst.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            android.os.SystemClock.sleep(200)
            scenario.onActivity {
                assertFalse(popup.isShowing)
                popup.show(true, { usernameCalls++ }, { copyCalls++ })
            }
            android.os.SystemClock.sleep(400)
            val outside = Rect()
            scenario.onActivity { b.root.getWindowVisibleDisplayFrame(outside) }
            val downTime = android.os.SystemClock.uptimeMillis()
            for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
                val event = android.view.MotionEvent.obtain(downTime, android.os.SystemClock.uptimeMillis(),
                    action, outside.centerX().toFloat(), (outside.bottom - 16).toFloat(), 0)
                event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                inst.uiAutomation.injectInputEvent(event, true)
                event.recycle()
            }
            android.os.SystemClock.sleep(200)
            scenario.onActivity { assertFalse("Outside tap dismisses the source window", popup.isShowing) }
        }
    }
}
