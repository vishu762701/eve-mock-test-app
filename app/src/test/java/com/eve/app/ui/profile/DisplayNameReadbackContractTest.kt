package com.eve.app.ui.profile

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DisplayNameReadbackContractTest {
    @Test fun sourceSupportsWordsWithoutWeakeningCanonicalUsernamePolicy() {
        val code = File("src/main/java/org/telegram/ui/ChangeNameActivity.java").readText()
        assertTrue(code.contains("InputType.TYPE_TEXT_FLAG_CAP_SENTENCES"))
        assertTrue(code.contains("firstNameField.setMaxLines(1)"))
        val current = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        assertTrue("The name editor must receive the confirmed API value, never a display fallback",
            current.contains("putExtra(\"current_name\", currentDisplayName)"))
        assertTrue(current.contains("currentDisplayName = profile.displayName.orEmpty()"))
        assertTrue(current.contains("if (!profileConfirmed) { loadProfile(); return@setOnClickListener }"))
    }

    @Test fun displayNameOnlyFinishesAfterAuthenticatedReadback() {
        val code = File("src/main/java/com/eve/app/ui/profile/ChangeNameActivity.kt").readText()
        val method = code.substringAfter("override fun saveName(")
        assertTrue(method.contains("replace(Regex(\"\\\\s+\"), \" \")"))
        assertTrue(method.contains("name.length > 100"))
        assertTrue(method.indexOf("ApiClient.apiService.getProfile(ProfileRequestOwner(ownerUid))") < method.indexOf("setResult(Activity.RESULT_OK)"))
        assertTrue(method.contains("confirmed.data?.displayName == name"))
        assertTrue(method.contains("FirebaseAuth.getInstance().currentUser?.uid == user.uid"))
    }
}
