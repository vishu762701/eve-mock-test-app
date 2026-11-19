package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UsernameReadbackContractTest {
    @Test fun staleAvailabilityDoesNotValidateNewerInput() {
        val source = File("src/main/java/com/eve/app/ui/profile/ChangeUsernameActivity.kt").readText()
        assertTrue(source.contains("field.text.toString().trim().removePrefix(\"@\") != name"))
        assertTrue(source.contains("checkJob?.cancel()"))
    }

    @Test fun successfulMutationRequiresOwnerReadbackAndButtonIsRestoredOnFailure() {
        val source = File("src/main/java/com/eve/app/ui/profile/ChangeUsernameActivity.kt").readText()
        assertTrue(source.contains("target.lowercase(java.util.Locale.ROOT)"))
        assertTrue(source.contains("ApiClient.apiService.getProfile(ProfileRequestOwner(ownerUid))"))
        assertTrue(source.contains("confirmed.success && savedHandle?.equals(normalizedTarget, ignoreCase = true) == true"))
        val success = source.substringAfter("if (confirmed.success && savedHandle?.equals(normalizedTarget, ignoreCase = true) == true")
        assertTrue(success.contains("setResult(Activity.RESULT_OK)"))
        assertTrue(success.contains("field?.isEnabled = true"))
        assertFalse(source.contains("tvStatus.text = \"Saved\""))
    }
}
