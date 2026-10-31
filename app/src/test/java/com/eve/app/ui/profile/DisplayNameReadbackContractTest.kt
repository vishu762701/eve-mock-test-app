package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DisplayNameReadbackContractTest {
    @Test fun sourceSupportsWordsWithoutWeakeningCanonicalUsernamePolicy() {
        val xml = File("src/main/res/layout/activity_change_name.xml").readText()
        assertTrue(xml.contains("textPersonName|textCapWords|textMultiLine"))
        assertTrue(xml.contains("android:maxLength=\"100\""))
        assertTrue(xml.contains("android:minLines=\"2\""))
        assertFalse(xml.contains("android:singleLine=\"true\""))
        val current = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        assertTrue(current.contains("putExtra(\"current_name\", binding.tvName.text.toString())"))
    }

    @Test fun displayNameOnlyFinishesAfterAuthenticatedReadback() {
        val code = File("src/main/java/com/eve/app/ui/profile/ChangeNameActivity.kt").readText()
        val method = code.substringAfter("private fun saveName()")
        assertTrue(method.contains("replace(Regex(\"\\\\s+\"), \" \")"))
        assertTrue(method.contains("name.length > 100"))
        assertTrue(method.indexOf("ApiClient.apiService.getProfile()") < method.indexOf("setResult(Activity.RESULT_OK)"))
        assertTrue(method.contains("confirmed.data?.displayName == name"))
        assertTrue(method.contains("FirebaseAuth.getInstance().currentUser?.uid == user.uid"))
        assertTrue(method.contains("binding.etName.isEnabled = true"))
    }
}
