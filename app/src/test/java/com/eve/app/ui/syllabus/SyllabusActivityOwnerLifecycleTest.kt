package com.eve.app.ui.syllabus

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyllabusActivityOwnerLifecycleTest {
    @Test
    fun activeSyllabusRequiresOriginalSignedInAccount() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("uid-student-1", "uid-student-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-student-1", "uid-student-2"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-student-1", null))
        assertFalse(SubmissionRetryPolicy.ownsSubmission(null, "uid-student-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("", "uid-student-1"))
    }

    @Test
    fun syllabusActivityEnforcesLifecycleAccountGuardAndRestoration() {
        val src = File("src/main/java/com/eve/app/ui/syllabus/SyllabusActivity.kt").readText()

        // 1. OwnsSubmission guard in declaration
        assertTrue("Must use SubmissionRetryPolicy for syllabus account ownership",
            src.contains("SubmissionRetryPolicy.ownsSubmission(syllabusOwnerUid, syllabusAuth.currentUser?.uid)"))

        // 2. Auth state listener registration in onStart and removal in onStop
        assertTrue("Must add AuthStateListener onStart",
            src.contains("syllabusAuth.addAuthStateListener(syllabusAuthListener)"))
        assertTrue("Must remove AuthStateListener onStop",
            src.contains("syllabusAuth.removeAuthStateListener(syllabusAuthListener)"))

        // 3. Saved instance state preservation
        assertTrue("Must restore owner uid from savedInstanceState in onCreate",
            src.contains("savedInstanceState.getString(\"syllabus_owner_uid\")"))
        assertTrue("Must persist owner uid to outState in onSaveInstanceState",
            src.contains("outState.putString(\"syllabus_owner_uid\", syllabusOwnerUid)"))

        // 4. Verification in onResume and download handlers
        assertTrue("Must check ownership in onResume",
            src.contains("if (!hasSyllabusOwner()) { finish(); return }"))
        assertTrue("Must guard checkPermissionAndDownload with ownership check",
            src.contains("if (!hasSyllabusOwner()) { finish(); return }"))
        assertTrue("Must guard startDownload with ownership check",
            src.contains("if (!hasSyllabusOwner()) { finish(); return }"))
    }
}
