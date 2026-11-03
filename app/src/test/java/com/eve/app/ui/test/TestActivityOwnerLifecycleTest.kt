package com.eve.app.ui.test

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestActivityOwnerLifecycleTest {
    @Test fun activeAttemptRequiresOriginalSignedInAccount() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("uid-A", "uid-A"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-A", "uid-B"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-A", null))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("", "uid-B"))
    }

    @Test fun testHostGuardsRestorationResumptionAndAsyncStart() {
        val src = File("src/main/java/com/eve/app/ui/test/TestActivity.kt").readText()
        val onCreate = src.substringAfter("override fun onCreate(savedInstanceState: Bundle?)")
            .substringBefore("private fun submit()")
        assertTrue(onCreate.contains("savedInstanceState.getString(\"key_attempt_owner\")"))
        assertTrue(onCreate.contains("SubmissionRetryPolicy.ownsSubmission(originUid, currentUid)"))
        assertTrue(onCreate.contains("if (!isTestOwner()) {"))
        assertTrue(src.contains("FirebaseAuth.getInstance().addAuthStateListener(testAccountListener)"))
        assertTrue(src.contains("FirebaseAuth.getInstance().removeAuthStateListener(testAccountListener)"))
        val pause = src.substringAfter("override fun onPause()").substringBefore("override fun onSaveInstanceState")
        assertTrue(pause.contains("if (isTestOwner() && !submitted"))
        assertTrue(src.contains("outState.putString(\"key_attempt_owner\", testOwnerUid)"))
    }

    @Test fun lateSubmitResponseCannotWriteOldAccountCachesOrNavigate() {
        val src = File("src/main/java/com/eve/app/ui/test/TestActivity.kt").readText()
        val body = src.substringAfter("private fun submit()")
            .substringBefore("private fun showExamNotice(")
        val network = body.indexOf("submitAttempt(body)")
        val ownerCheck = body.indexOf("if (!isTestOwner() || isFinishing || isDestroyed) return@launch", network)
        val firstMutation = body.indexOf("PendingSubmissionStore(")
        assertTrue("Every graded response must confirm original owner first",
            network >= 0 && ownerCheck > network && firstMutation > ownerCheck)
        assertTrue("Failure must not enqueue another account's offline attempt",
            body.substringAfter("catch (e: Exception) {").contains(
                "if (!isTestOwner() || isFinishing || isDestroyed) return@launch"))
    }
}
