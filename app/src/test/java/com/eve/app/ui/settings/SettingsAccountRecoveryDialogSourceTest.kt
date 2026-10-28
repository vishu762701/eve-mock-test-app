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
