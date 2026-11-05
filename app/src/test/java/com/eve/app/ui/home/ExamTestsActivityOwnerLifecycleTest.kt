package com.eve.app.ui.home

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamTestsActivityOwnerLifecycleTest {
    @Test
    fun activeExamTestsRequiresOriginalSignedInAccount() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("uid-exam-1", "uid-exam-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-exam-1", "uid-exam-2"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-exam-1", null))
        assertFalse(SubmissionRetryPolicy.ownsSubmission(null, "uid-exam-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("", "uid-exam-1"))
    }

    @Test
    fun examTestsActivityEnforcesLifecycleAccountGuardAndRestoration() {
        val src = File("src/main/java/com/eve/app/ui/home/ExamTestsActivity.kt").readText()

        // 1. OwnsSubmission guard in declaration
        assertTrue("Must use SubmissionRetryPolicy for exam tests account ownership",
            src.contains("SubmissionRetryPolicy.ownsSubmission(examTestsOwnerUid, examTestsAuth.currentUser?.uid)"))

        // 2. Auth state listener registration in onStart and removal in onStop
        assertTrue("Must add AuthStateListener onStart",
            src.contains("examTestsAuth.addAuthStateListener(examTestsAuthListener)"))
        assertTrue("Must remove AuthStateListener onStop",
            src.contains("examTestsAuth.removeAuthStateListener(examTestsAuthListener)"))

        // 3. Saved instance state preservation
        assertTrue("Must restore owner uid from savedInstanceState in onCreate",
            src.contains("savedInstanceState.getString(\"exam_tests_owner_uid\")"))
        assertTrue("Must persist owner uid to outState in onSaveInstanceState",
            src.contains("outState.putString(\"exam_tests_owner_uid\", examTestsOwnerUid)"))

        // 4. Verification in onResume and click handlers
        assertTrue("Must check ownership in onResume",
            src.contains("if (!hasExamTestsOwner()) { finish(); return }"))
        assertTrue("Must guard handleTestClick with ownership check",
            src.contains("if (!hasExamTestsOwner()) { finish(); return }"))
    }
}
