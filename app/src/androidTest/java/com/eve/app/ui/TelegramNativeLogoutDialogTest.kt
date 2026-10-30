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
    @Test fun listItemSelectionDispatchesIndexAndDismisses() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                var selected = -1
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Select App Language")
                    .setItems(arrayOf("System", "English", "Hindi")) { _, which -> selected = which }
                    .setNegativeButton("Cancel", null)
                    .show()
                try {
                    val row = dialog.window!!.decorView.findViewWithTag<android.view.View>(
                        "telegram_native_alert_item_2"
                    )
                    assertNotNull("Original 50dp Telegram choice row missing", row)
                    assertTrue(row.height >= (48 * activity.resources.displayMetrics.density).toInt())
                    row.performClick()
                    assertEquals(2, selected)
                    assertFalse(dialog.isShowing)
                } finally {
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
        }
    }

    @Test fun radioChoiceMarksOnlyConfiguredEntryAndCancelDoesNotChoose() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                var choices = 0
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Select App Language")
                    .setSingleChoiceItems(arrayOf("System", "English", "Hindi"), 1) { _, _ -> choices++ }
                    .setNegativeButton("Cancel", null).show()
                try {
                    val first = dialog.window!!.decorView.findViewWithTag<android.view.View>(
                        "telegram_native_alert_item_0")
                    val selected = dialog.window!!.decorView.findViewWithTag<android.view.View>(
                        "telegram_native_alert_item_1")
                    assertFalse(first.isSelected)
                    assertTrue(selected.isSelected)
                    dialog.getActionButton(DialogInterface.BUTTON_NEGATIVE)!!.performClick()
                    assertEquals(0, choices)
                } finally {
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
        }
    }

    @Test fun customViewPreservesOriginalViewIdentityAndCancellation() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val custom = android.widget.EditText(activity).apply {
                    tag = "telegram_custom_input"
                    hint = "Enter name"
                }
                val previousParent = android.widget.FrameLayout(activity).apply { addView(custom) }
                var submissions = 0
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Edit name")
                    .setView(custom)
                    .setPositiveButton("Save") { _, _ -> submissions++ }
                    .setNegativeButton("Cancel", null).show()
                try {
                    assertFalse("View must detach its old parent", previousParent.indexOfChild(custom) >= 0)
                    assertTrue(dialog.window!!.decorView.findViewWithTag<android.view.View>(
                        "telegram_custom_input") === custom)
                    dialog.getActionButton(DialogInterface.BUTTON_NEGATIVE)!!.performClick()
                    assertEquals(0, submissions)
                } finally {
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
        }
    }

    @Test fun sourceRedDestructiveAndNeutralActionsPreserveSemantics() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                var deleted = 0
                var neutral = 0
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Delete")
                    .setMessage("Confirm")
                    .setPositiveButton("Delete") { _, _ -> deleted++ }
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Details") { _, _ -> neutral++ }
                    .makeRed(DialogInterface.BUTTON_POSITIVE)
                    .show()
                try {
                    val positive = dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!
                    val info = dialog.getActionButton(DialogInterface.BUTTON_NEUTRAL)!!
                    assertEquals(
                        androidx.core.content.ContextCompat.getColor(
                            activity, com.eve.app.R.color.design_destructive_text
                        ),
                        positive.currentTextColor
                    )
                    assertNotNull(info.background)
                    info.performClick()
                    assertEquals(0, deleted)
                    assertEquals(1, neutral)
                    assertFalse(dialog.isShowing)
                } finally {
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
        }
    }

    @Test fun longButtonsOnNarrowPhoneWrapVerticallyWithoutClipping() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Long localized confirmation")
                    .setMessage("Confirm this action.")
                    .setNeutralButton("Review additional information", null)
                    .setNegativeButton("Cancel this operation", null)
                    .setPositiveButton("Confirm and continue", null)
                    .show()
                try {
                    val group = dialog.window!!.decorView.findViewWithTag<android.widget.LinearLayout>(
                        "telegram_native_alert_actions"
                    )
                    assertNotNull(group)
                    assertEquals(android.widget.LinearLayout.VERTICAL, group.orientation)
                    val buttons = listOf(
                        DialogInterface.BUTTON_NEUTRAL,
                        DialogInterface.BUTTON_NEGATIVE,
                        DialogInterface.BUTTON_POSITIVE
                    )
                    for (key in buttons) {
                        val b = dialog.getActionButton(key)!!
                        assertTrue(b.width > 0)
                        assertTrue(b.height >= (48 * activity.resources.displayMetrics.density).toInt())
                    }
                } finally {
                    dialog.dismiss()
                }
            }
        }
    }

    @Test fun longReadOnlyMessageUsesScrollContainerAndCloseOnly() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val details = (1..80).joinToString("\n") { "Question $it: explanation and review notes" }
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Test Preview")
                    .setMessage(details)
                    .setPositiveButton("Close", null)
                    .show()
                try {
                    val message = dialog.window!!.decorView.findViewWithTag<android.widget.TextView>(
                        "telegram_native_alert_message"
                    )
                    assertNotNull(message)
                    assertEquals(details, message.text.toString())
                    assertTrue(message.parent.parent is android.widget.ScrollView)
                    dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick()
                    assertFalse(dialog.isShowing)
                } finally {
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
        }
    }

}
