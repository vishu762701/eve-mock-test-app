package com.eve.app.ui.pyq

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PyqActivityOwnerLifecycleTest {
    @Test
    fun activePyqRequiresOriginalSignedInAccount() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("uid-pyq-1", "uid-pyq-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-pyq-1", "uid-pyq-2"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-pyq-1", null))
        assertFalse(SubmissionRetryPolicy.ownsSubmission(null, "uid-pyq-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("", "uid-pyq-1"))
    }

    @Test
    fun pyqActivityEnforcesLifecycleAccountGuardAndRestoration() {
        val src = File("src/main/java/com/eve/app/ui/pyq/PyqActivity.kt").readText()

        // 1. OwnsSubmission guard in declaration
        assertTrue("Must use SubmissionRetryPolicy for PYQ account ownership",
            src.contains("SubmissionRetryPolicy.ownsSubmission(pyqOwnerUid, pyqAuth.currentUser?.uid)"))

        // 2. Auth state listener registration in onStart and removal in onStop
        assertTrue("Must add AuthStateListener onStart",
            src.contains("pyqAuth.addAuthStateListener(pyqAuthListener)"))
        assertTrue("Must remove AuthStateListener onStop",
            src.contains("pyqAuth.removeAuthStateListener(pyqAuthListener)"))

        // 3. Saved instance state preservation
        assertTrue("Must restore owner uid from savedInstanceState in onCreate",
            src.contains("savedInstanceState.getString(\"pyq_owner_uid\")"))
        assertTrue("Must persist owner uid to outState in onSaveInstanceState",
            src.contains("outState.putString(\"pyq_owner_uid\", pyqOwnerUid)"))

        // 4. Verification in onResume and click handlers
        assertTrue("Must check ownership in onResume",
            src.contains("if (!hasPyqOwner()) finish()"))
        assertTrue("Must guard startPyq with ownership check",
            src.contains("if (!isCurrentOwner()"))
        assertTrue("Must guard paper click with ownership check",
            src.contains("if (!hasPyqOwner()) { finish(); return@setOnClickListener }"))
    }
}
