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
                    b.profileScrollView.scrollTo(0, if (collapsed) 1000 else 0)
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
