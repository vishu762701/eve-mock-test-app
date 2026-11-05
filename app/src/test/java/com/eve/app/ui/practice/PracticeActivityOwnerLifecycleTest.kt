package com.eve.app.ui.practice

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeActivityOwnerLifecycleTest {
    @Test
    fun activePracticeRequiresOriginalSignedInAccount() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("uid-student-1", "uid-student-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-student-1", "uid-student-2"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-student-1", null))
        assertFalse(SubmissionRetryPolicy.ownsSubmission(null, "uid-student-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("", "uid-student-1"))
    }

    @Test
    fun practiceActivityEnforcesLifecycleAccountGuardAndRestoration() {
        val src = File("src/main/java/com/eve/app/ui/practice/PracticeActivity.kt").readText()

        // 1. OwnsSubmission guard in declaration
        assertTrue("Must use SubmissionRetryPolicy for practice account ownership",
            src.contains("SubmissionRetryPolicy.ownsSubmission(practiceOwnerUid, practiceAuth.currentUser?.uid)"))

        // 2. Auth state listener registration in onStart and removal in onStop
        assertTrue("Must add AuthStateListener onStart",
            src.contains("practiceAuth.addAuthStateListener(practiceAuthListener)"))
        assertTrue("Must remove AuthStateListener onStop",
            src.contains("practiceAuth.removeAuthStateListener(practiceAuthListener)"))

        // 3. Saved instance state preservation
        assertTrue("Must restore owner uid from savedInstanceState in onCreate",
            src.contains("savedInstanceState.getString(\"practice_owner_uid\")"))
        assertTrue("Must persist owner uid to outState in onSaveInstanceState",
            src.contains("outState.putString(\"practice_owner_uid\", practiceOwnerUid)"))

        // 4. Verification in onResume and click handlers
        assertTrue("Must check ownership in onResume",
            src.contains("if (!hasPracticeOwner()) finish()"))
        assertTrue("Must guard startPractice with ownership check",
            src.contains("if (!hasPracticeOwner()) { finish(); return }"))
    }
}
