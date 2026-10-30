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
                    assertTrue("Cancel action layout must have 48dp accessible height",
                        cancel.layoutParams.height >= (48 * activity.resources.displayMetrics.density).toInt())
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
                    assertTrue("Telegram choice row reserves a real 50dp tap height",
                        row.layoutParams.height >= (48 * activity.resources.displayMetrics.density).toInt())
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
                        assertEquals("Narrow screen actions must use full-width rows",
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT, b.layoutParams.width)
                        assertTrue("Action row must reserve at least 48dp for touch",
                            b.layoutParams.height >= (48 * activity.resources.displayMetrics.density).toInt())
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

    @Test fun nativeBuilderPreservesNonCancelableExamNoticeAndDismissCleanup() {
        val dismissed = java.util.concurrent.atomic.AtomicInteger()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                var completed = 0
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Attempt Limit Reached")
                    .setMessage("You have reached the maximum number of attempts.")
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ -> completed++ }
                    .setOnDismissListener { dismissed.incrementAndGet() }
                    .show()
                try {
                    assertTrue(dialog.isShowing)
                    dialog.onBackPressed()
                    assertTrue("Non-cancelable exam notice must survive Back", dialog.isShowing)
                    assertEquals(0, completed)
                    dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick()
                    assertEquals(1, completed)
                    assertFalse(dialog.isShowing)
                } finally {
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
            // Android Dialog delivers OnDismissListener via the main-looper Handler.
            // Its callback is not guaranteed synchronously within performClick().
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals("Dismiss notification delivered exactly once", 1, dismissed.get())
        }
    }

    @Test fun secureCustomInputValidationCanKeepNativeDialogOpen() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val field = android.widget.EditText(activity).apply {
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    isSingleLine = true
                }
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Confirm Account Identity")
                    .setView(field)
                    .setPositiveButton("Verify", null)
                    .setNegativeButton("Cancel", null)
                    .show()
                try {
                    val verify = dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!
                    var accepted = 0
                    verify.setOnClickListener {
                        if (field.text.isNullOrBlank()) {
                            field.error = "Password required"
                        } else {
                            accepted++
                            field.text?.clear()
                            dialog.dismiss()
                        }
                    }
                    verify.performClick()
                    assertEquals(0, accepted)
                    assertTrue("Blank input may not close reauthentication", dialog.isShowing)
                    field.setText("sample-only-password")
                    verify.performClick()
                    assertEquals(1, accepted)
                    assertTrue(field.text.isNullOrEmpty())
                    assertFalse(dialog.isShowing)
                } finally {
                    field.text?.clear()
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
        }
    }


    @Test fun confirmationChoiceKeepsWindowOpenAndUpdatesRadioBeforeCommit() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                var chosen = 0
                var confirmed = -1
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Reassign Syllabus")
                    .setSingleChoiceItems(
                        arrayOf("Exam A", "Exam B"), 0, dismissOnSelection = false
                    ) { _, index -> chosen = index }
                    .setPositiveButton("Reassign") { _, _ -> confirmed = chosen }
                    .setNegativeButton("Cancel", null)
                    .show()
                try {
                    val second = dialog.window!!.decorView.findViewWithTag<android.widget.LinearLayout>(
                        "telegram_native_alert_item_1"
                    )
                    assertNotNull(second)
                    second.performClick()
                    assertTrue("Choice selection must not submit or dismiss", dialog.isShowing)
                    assertEquals(-1, confirmed)
                    assertEquals(1, chosen)
                    val first = dialog.window!!.decorView.findViewWithTag<android.widget.LinearLayout>(
                        "telegram_native_alert_item_0"
                    )
                    assertNotNull(first)
                    assertFalse("Previous radio row must be unselected", first.isSelected)
                    assertTrue("Previous radio dot must be cleared", (0 until first.childCount).none {
                        (first.getChildAt(it) as? android.widget.RadioButton)?.isChecked == true
                    })
                    assertTrue(second.isSelected)
                    assertTrue((0 until second.childCount).any {
                        (second.getChildAt(it) as? android.widget.RadioButton)?.isChecked == true
                    })
                    dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)!!.performClick()
                    assertEquals(1, confirmed)
                    assertFalse(dialog.isShowing)
                } finally {
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
        }
    }


    @Test fun sourceNativeCancelListenerFiresForOutsideOrBackWithoutCommitting() {
        val cancellations = java.util.concurrent.atomic.AtomicInteger()
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                var confirmations = 0
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Unsaved Changes")
                    .setMessage("Do you want to save before switching?")
                    .setOnCancelListener { cancellations.incrementAndGet() }
                    .setPositiveButton("Save") { _, _ -> confirmations++ }
                    .setNegativeButton("Discard", null)
                    .show()
                dialog.cancel()
                assertEquals(0, confirmations)
                assertFalse(dialog.isShowing)
                val discard = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Unsaved Changes")
                    .setOnCancelListener { cancellations.incrementAndGet() }
                    .setNegativeButton("Discard", null)
                    .show()
                discard.getActionButton(DialogInterface.BUTTON_NEGATIVE)!!.performClick()
                assertFalse(discard.isShowing)
            }
            // Dialog cancellation is delivered from Android's main-looper Handler.
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals("Only Back/outside cancellation invokes OnCancelListener", 1, cancellations.get())
        }
    }

    @Test fun sourceNativeDialogConstrainsWindowAndUsesOriginalScrimWithUsableActions() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val dialog = TelegramNativeAlertDialog.Builder(activity)
                    .setTitle("Review confirmation")
                    .setMessage("Read the explanation before confirming.")
                    .setPositiveButton("Continue", null)
                    .setNegativeButton("Cancel", null)
                    .show()
                try {
                    val window = requireNotNull(dialog.window)
                    val params = window.attributes
                    val dp = activity.resources.displayMetrics.density
                    val screen = activity.resources.displayMetrics.widthPixels
                    assertTrue("Telegram window must not extend beyond display", params.width in 1..screen)
                    assertTrue("Dialog width must leave room for readable content",
                        params.width >= (240f * dp).toInt())
                    assertTrue("Original dialog dims content behind the window",
                        params.flags and android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0)
                    assertEquals("Source-native message scrim", 0.5f, params.dimAmount, 0.001f)
                    val action = requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE))
                    assertTrue("Primary action must remain accessible",
                        action.layoutParams.height >= (48f * dp).toInt())
                    dialog.getActionButton(DialogInterface.BUTTON_NEGATIVE)!!.performClick()
                    assertFalse(dialog.isShowing)
                } finally {
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
        }
    }

}
