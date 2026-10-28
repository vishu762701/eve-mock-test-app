package com.eve.app.ui

import android.graphics.Color
import android.graphics.drawable.NinePatchDrawable
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.designsystem.showDestructive
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.home.TelegramDialogSourceBackground
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Actual window-level assertions for the source-backed Home confirmation.
 * Runs only in the owner-authorized terminal FULL native matrix; FAST compiles
 * this test source without launching emulators.
 *
 * Original source: AlertDialog.java:302-318,680-693, DarkAlertDialog.java:11-19
 * at DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 */
@RunWith(AndroidJUnit4::class)
class HomeTelegramDialogNativeTest {
    @Test fun originalDialogNinePatchAndDestructiveButtonsSurviveBothUiModes() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                var destructiveInvocations = 0
                val dialog = MaterialAlertDialogBuilder(activity)
                    .setTitle("Delete Feedback Post?")
                    .setMessage("This confirmation must not change server authorization.")
                    .setPositiveButton("Delete") { _, _ -> destructiveInvocations++ }
                    .setNegativeButton("Cancel", null)
                    .showDestructive()
                TelegramDialogSourceBackground.apply(dialog)
                try {
                    assertTrue("Confirmation window must be visible", dialog.isShowing)
                    val decor = dialog.window?.decorView
                    assertNotNull("Dialog must have a window", decor)
                    assertTrue("Original source nine-patch must be the window background",
                        decor?.background is NinePatchDrawable)
                    val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    assertNotNull("Delete button must remain present", positive)
                    assertNotNull("Cancel button must remain present", negative)
                    assertEquals("Delete", positive.text.toString())
                    assertEquals("Cancel", negative.text.toString())
                    val palette = TelegramThemeEngine.resolveCurrentPalette(activity)
                    if (palette.isDark) {
                        assertEquals("Original DarkAlertDialog message must be white",
                            Color.WHITE, dialog.findViewById<TextView>(android.R.id.message)?.currentTextColor)
                        assertEquals("Original DarkAlertDialog cancel must be white",
                            Color.WHITE, negative.currentTextColor)
                    }
                    // Showing and dismissing cannot execute a destructive server mutation.
                    dialog.dismiss()
                    assertEquals("No delete on dismiss", 0, destructiveInvocations)
                } finally {
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
        }
    }
}
