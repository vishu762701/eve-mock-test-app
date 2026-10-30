package com.eve.app.ui

import android.content.DialogInterface
import android.graphics.drawable.NinePatchDrawable
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.designsystem.TelegramNativeAlertDialog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TelegramNativeLogoutDialogTest {
    @Test fun nativeWindowUsesSourceAndCancelCannotLogOut() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                var logouts = 0
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Log out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log out") { _, _ -> logouts++ }
                    .setNegativeButton("Cancel", null).show()
                try {
                    assertTrue(dialog.isShowing)
                    assertTrue(dialog.window?.decorView?.background is NinePatchDrawable)
                    assertNotNull(dialog.window?.decorView?.findViewWithTag<TextView>("telegram_native_alert_title"))
                    assertNotNull(dialog.window?.decorView?.findViewWithTag<TextView>("telegram_native_alert_message"))
                    val cancel = dialog.getActionButton(DialogInterface.BUTTON_NEGATIVE)!!
                    assertTrue(cancel.minHeight >= (48 * activity.resources.displayMetrics.density).toInt())
                    cancel.performClick()
                    assertEquals(0, logouts)
                    assertFalse(dialog.isShowing)
                } finally {
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
        }
    }
    @Test fun positiveCallbackIsAtMostOnceEvenOnRapidTap() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                var logouts = 0
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Log out").setMessage("Confirm")
                    .setPositiveButton("Log out") { _, _ -> logouts++ }
                    .setNegativeButton("Cancel", null).show()
                val button = dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!
                button.performClick()
                button.performClick()
                assertEquals(1, logouts)
                assertFalse(dialog.isShowing)
            }
        }
    }
}
