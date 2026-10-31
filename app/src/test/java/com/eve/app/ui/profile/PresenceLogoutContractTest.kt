package com.eve.app.ui.profile

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/**
 * Guards authenticated presence revocation before locally destroying the JWT.
 * Network failure remains bounded: user logout must never be held indefinitely.
 */
class PresenceLogoutContractTest {
    @Test fun bothExplicitLogoutPathsRevokeLeaseBeforeRemovingFirebaseCredentials() {
        for (path in listOf(
            "src/main/java/com/eve/app/ui/settings/SettingsActivity.kt",
            "src/main/java/com/eve/app/ui/home/MainActivity.kt"
        )) {
            val src = File(path).readText()
            val request = src.substringAfter("private fun logout()")
                .takeIf { path.contains("MainActivity") }
                ?: src.substringAfter("private fun executeLogout()")
            assertTrue("Explicit logout must invoke revokeBeforeLogout: $path",
                request.contains("ProfilePresenceClient.revokeBeforeLogout()"))
            assertTrue("Local logout follows server lease revoke: $path",
                request.indexOf("revokeBeforeLogout()") < request.indexOf("performLocalLogout()"))
            val local = src.substringAfter("private fun performLocalLogout()").substringBefore("private fun ")
            assertTrue("Firebase logout must still occur: $path",
                local.contains("FirebaseAuth.getInstance().signOut()"))
        }
    }

    @Test fun logoutRevokeIsBoundedAndGuardsConcurrentHeartbeatRestarts() {
        val src = File("src/main/java/com/eve/app/util/ProfilePresenceClient.kt").readText()
        val revoke = src.substringAfter("suspend fun revokeBeforeLogout()")
            .substringBefore("private fun stopSession()")
        assertTrue(revoke.contains("logoutInProgress = true"))
        assertTrue(revoke.contains("previousHeartbeat?.cancel()"))
        assertTrue(revoke.contains("previousHeartbeat?.join()"))
        assertTrue(revoke.contains("withTimeoutOrNull(1_500L)"))
        assertTrue(revoke.contains("ApiClient.apiService.endPresence("))
        assertTrue(revoke.indexOf("previousHeartbeat?.join()") <
            revoke.indexOf("ApiClient.apiService.endPresence("))
        assertTrue(src.contains("if (logoutInProgress || unsupportedByServer ||"))
        assertTrue(src.contains("logoutInProgress = false"))
        assertFalse(src.contains("Thread.sleep("))
    }
}
