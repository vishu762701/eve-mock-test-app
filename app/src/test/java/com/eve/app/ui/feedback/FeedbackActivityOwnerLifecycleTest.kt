package com.eve.app.ui.feedback

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackActivityOwnerLifecycleTest {
    @Test
    fun activeFeedbackRequiresOriginalSignedInAccount() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("uid-student-1", "uid-student-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-student-1", "uid-student-2"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-student-1", null))
        assertFalse(SubmissionRetryPolicy.ownsSubmission(null, "uid-student-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("", "uid-student-1"))
    }

    @Test
    fun feedbackActivityEnforcesLifecycleAccountGuardAndRestoration() {
        val src = File("src/main/java/com/eve/app/ui/feedback/FeedbackActivity.kt").readText()

        // 1. OwnsSubmission guard in declaration
        assertTrue("Must use SubmissionRetryPolicy for feedback account ownership",
            src.contains("SubmissionRetryPolicy.ownsSubmission(feedbackOwnerUid, feedbackAuth.currentUser?.uid)"))

        // 2. Auth state listener registration in onStart and removal in onStop
        assertTrue("Must add AuthStateListener onStart",
            src.contains("feedbackAuth.addAuthStateListener(feedbackAuthListener)"))
        assertTrue("Must remove AuthStateListener onStop",
            src.contains("feedbackAuth.removeAuthStateListener(feedbackAuthListener)"))

        // 3. Saved instance state preservation
        assertTrue("Must restore owner uid from savedInstanceState in onCreate",
            src.contains("savedInstanceState.getString(\"feedback_owner_uid\")"))
        assertTrue("Must persist owner uid to outState in onSaveInstanceState",
            src.contains("outState.putString(\"feedback_owner_uid\", feedbackOwnerUid)"))

        // 4. Verification in onResume and click handlers
        assertTrue("Must check ownership in onResume",
            src.contains("if (!hasFeedbackOwner()) finish()"))
        assertTrue("Must guard sendFeedback with ownership check",
            src.contains("if (!hasFeedbackOwner()) { finish(); return@setOnClickListener }"))
    }
}
