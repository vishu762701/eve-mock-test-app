package com.eve.app.designsystem

import android.app.Activity
import android.app.Application
import android.content.DialogInterface
import android.widget.EditText
import com.eve.app.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class NativeDialogDismissPolicyTest {
    private fun form() {
        val controller=Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.Theme_Eve)
        controller.setup().visible()
        val input=EditText(controller.get()).apply { setText("Draft question") }
        var confirmed=false
        var dismissals=0
        val dialog=TelegramNativeAlertDialog.Builder(controller.get())
            .setTitle("Question") .setView(input)
            .setPositiveButton("Add") { owner,_ -> if (confirmed) owner.dismiss() else input.error="Not confirmed" }
            .setNegativeButton("Cancel") { owner,_ -> owner.dismiss() }.create()
        dialog.setDismissDialogByButtons(false)
        dialog.setOnDismissListener { dismissals++ }
        try {
            dialog.show()
            val add=requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE))
            add.performClick()
            assertTrue("Validation/server failure retains the actual form",dialog.isShowing)
            assertEquals("Draft question",input.text.toString())
            assertNotNull(input.error)
            confirmed=true
            add.performClick()
            assertFalse(dialog.isShowing)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(1,dismissals)
            add.performClick()
            assertEquals(1,dismissals)
        } finally { dialog.dismiss(); controller.pause().stop().destroy() }
    }
    @Test fun invalidOrUnconfirmedFormKeepsInputUntilExplicitSuccess()=form()
    @Test @Config(qualifiers="night") fun nightFormUsesSameDismissalPolicy()=form()
    @Test fun defaultConfirmationStillDismissesAndPolicyCanBeRestored() {
        val controller=Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.Theme_Eve)
        controller.setup().visible()
        val dialog=TelegramNativeAlertDialog.Builder(controller.get()).setMessage("Confirm?")
            .setPositiveButton("OK",null).create()
        try {
            dialog.show()
            requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)).performClick()
            assertFalse(dialog.isShowing)
            dialog.show()
            dialog.setDismissDialogByButtons(false)
            requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)).performClick()
            assertTrue(dialog.isShowing)
            dialog.setDismissDialogByButtons(true)
            requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)).performClick()
            assertFalse(dialog.isShowing)
        } finally { dialog.dismiss(); controller.pause().stop().destroy() }
    }
}
