package com.eve.app.ui.admin

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Admin feedback reply keeps its secure server callback and single-dialog lifecycle. */
class FeedbackReplySourceDialogContractTest {
    @Test fun customAdminReplyUsesOriginalTelegramDialogChromeWithoutAutoDismiss() {
        val source = File("src/main/java/com/eve/app/ui/admin/FeedbackMessagesActivity.kt").readText()
        val reply = source.substringAfter("private fun showReplyDialog(message: FeedbackMessage)")
            .substringBefore("override fun onDestroy()")
        assertTrue(reply.contains("TelegramNativeAlertDialog.Builder(this)"))
        assertTrue(reply.contains(".setView(dialogBinding.root)"))
        assertTrue(reply.contains(".create()"))
        assertTrue(reply.contains("dialog.show()"))
        assertTrue(!reply.contains("TelegramDialogSourceBackground.apply(dialog)"))
        assertTrue(reply.contains("dialogBinding.btnSendReply.setOnClickListener"))
        assertTrue(reply.contains("dialogBinding.btnCancelReply.setOnClickListener"))
    }

    @Test fun dialogCanNeverStackOrOutliveItsActivityAndPayloadRemainsUnchanged() {
        val source = File("src/main/java/com/eve/app/ui/admin/FeedbackMessagesActivity.kt").readText()
        assertTrue(source.contains("private var replyDialog: TelegramNativeAlertDialog? = null"))
        assertTrue(source.contains("if (isFinishing || isDestroyed || replyDialog?.isShowing == true) return"))
        assertTrue(source.contains("dialog.setOnDismissListener"))
        assertTrue(source.contains("if (replyDialog === dialog) replyDialog = null"))
        assertTrue(source.contains("replyDialog?.dismiss()"))
        val reply = source.substringAfter("private fun showReplyDialog(message: FeedbackMessage)")
        assertTrue(reply.contains("repo.replyFeedbackMessage(message.id, replyText)"))
        assertTrue(reply.contains("dialogBinding.btnSendReply.isEnabled = false"))
        assertTrue(reply.contains("dialogBinding.btnSendReply.isEnabled = true"))
        assertTrue(reply.contains("if (replyText.isBlank())"))
        assertTrue("Server authorization gate remains unchanged",
            source.contains("AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true)"))
        assertTrue("Delete undo flow remains unchanged",
            source.contains("AppUndoBar.show(") &&
                source.contains(".makeRed(DialogInterface.BUTTON_POSITIVE).show()"))
    }
}
