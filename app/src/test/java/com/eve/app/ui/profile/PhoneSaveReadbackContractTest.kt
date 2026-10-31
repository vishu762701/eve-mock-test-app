package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PhoneSaveReadbackContractTest {
    @Test fun phoneEditOnlyCompletesAfterAuthenticatedReadbackOfExpectedValue() {
        val code = File("src/main/java/com/eve/app/ui/profile/ChangePhoneActivity.kt").readText()
        val branch = code.substringAfter("if (res.success) {").substringBefore("} else {\n                    binding.btnDone.isEnabled = true")
        assertTrue(branch.contains("ApiClient.apiService.getProfile(ProfileRequestOwner(ownerUid))"))
        assertTrue(branch.contains("confirmed.success && confirmed.data?.phone?.trim() == phoneToSave"))
        assertTrue(branch.indexOf("ApiClient.apiService.getProfile(ProfileRequestOwner(ownerUid))") < branch.indexOf("setResult(Activity.RESULT_OK)"))
        assertTrue(branch.contains("binding.btnDone.isEnabled = true"))
        assertFalse(code.contains("Log.d("))
    }
}
