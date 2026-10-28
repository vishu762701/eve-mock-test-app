package com.eve.app.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Source-backed account recovery dialogs, with original auth/network outcomes. */
class SettingsAccountRecoveryDialogSourceTest {
    private val source = File("src/main/java/com/eve/app/ui/settings/SettingsActivity.kt").readText()

    @Test fun accountDeletionFailureUsesOriginalSourceDialogOnlyWhileActivityAlive() {
        val deletion = source.substringAfter("private fun executeAccountDeletion()")
            .substringBefore("private fun promptReauthentication()")
        assertTrue(deletion.contains("ApiClient.apiService.deleteAccount()"))
        assertTrue(deletion.contains("if (!response.success)"))
        assertTrue(deletion.contains("user.delete().await()"))
        assertTrue(deletion.contains("if (isFinishing || isDestroyed) return@launch"))
        assertTrue(deletion.contains(".setTitle(\"Account Deletion Failed\")"))
        assertTrue(deletion.contains(".showTelegramSourceDialog()"))
        assertTrue(!deletion.contains(".showDestructive()"))
    }

    @Test fun passwordReauthenticationRetainsSecureInputAndOriginalSourceRenderer() {
        val recovery = source.substringAfter("private fun promptReauthentication()")
            .substringBefore("override fun onDestroy()")
        val emailBranch = recovery.substringAfter("EmailAuthProvider.PROVIDER_ID")
            .substringBefore(".setTitle(\"Re-authentication Required\")")
        assertTrue(source.contains("private var passwordReauthenticationDialog: AlertDialog? = null"))
        assertTrue(recovery.contains("passwordReauthenticationDialog?.isShowing == true"))
        assertTrue(emailBranch.contains("TYPE_TEXT_VARIATION_PASSWORD"))
        assertTrue(emailBranch.contains("dialog.setOnShowListener"))
        assertTrue(emailBranch.contains("user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()"))
        assertTrue(emailBranch.contains("field.error = \"Password required\""))
        assertTrue(emailBranch.contains("field.error = \"Verification failed. Check your password.\""))
        assertTrue(emailBranch.contains("dialog.setOnDismissListener"))
        assertTrue(emailBranch.contains("field.text?.clear()"))
        assertTrue(emailBranch.contains("passwordReauthenticationDialog = dialog"))
        assertTrue(emailBranch.contains("TelegramDialogSourceBackground.apply(dialog)"))
        assertTrue("Original source helper must be applied AFTER show to a real window",
            emailBranch.indexOf("TelegramDialogSourceBackground.apply(dialog)") >
                emailBranch.indexOf("dialog.show()"))
        val destroy = source.substringAfter("override fun onDestroy()")
        assertTrue(destroy.contains("passwordReauthenticationDialog?.dismiss()"))
        assertTrue(destroy.contains("passwordReauthenticationDialog = null"))
    }

    @Test fun googleReauthenticationKeepsOriginalLoginFlowAndSourceNightDialog() {
        val recovery = source.substringAfter("private fun promptReauthentication()")
            .substringBefore("override fun onDestroy()")
        assertTrue(recovery.contains("if (isFinishing || isDestroyed) return"))
        assertTrue(recovery.contains(".setTitle(\"Re-authentication Required\")"))
        assertTrue(recovery.contains(".setPositiveButton(\"Sign In\")"))
        assertTrue(recovery.contains(".setNegativeButton(\"Cancel\", null)"))
        assertTrue(recovery.contains(".showTelegramSourceDialog()"))
        assertTrue(recovery.contains("GoogleSignIn.getClient(this, gso)"))
        assertTrue(recovery.contains("client.signOut().addOnCompleteListener"))
        assertTrue(recovery.contains("if (!isFinishing && !isDestroyed)"))
        assertTrue(recovery.contains("reauthLauncher.launch(client.signInIntent)"))
        assertTrue("Email/password reauthentication branch must remain unchanged",
            recovery.contains("EmailAuthProvider.getCredential(email, password)") &&
                recovery.contains("user.reauthenticate("))
    }
}
