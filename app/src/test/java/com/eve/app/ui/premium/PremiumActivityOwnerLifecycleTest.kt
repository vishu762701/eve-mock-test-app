package com.eve.app.ui.premium

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumActivityOwnerLifecycleTest {
    @Test
    fun activePremiumRequiresOriginalSignedInAccount() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("uid-student-1", "uid-student-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-student-1", "uid-student-2"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-student-1", null))
        assertFalse(SubmissionRetryPolicy.ownsSubmission(null, "uid-student-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("", "uid-student-1"))
    }

    @Test
    fun premiumActivityEnforcesLifecycleAccountGuardAndRestoration() {
        val src = File("src/main/java/com/eve/app/ui/premium/PremiumActivity.kt").readText()

        // 1. Ownership check and listener declaration
        assertTrue("Must declare premiumAuthListener",
            src.contains("private val premiumAuthListener = FirebaseAuth.AuthStateListener"))
        assertTrue("Must check hasOriginalAccount in listener",
            src.contains("if (premiumOwnerUid != null && !hasOriginalAccount()) finish()"))

        // 2. Auth state listener registration in onStart and removal in onStop
        assertTrue("Must add AuthStateListener onStart",
            src.contains("premiumAuth.addAuthStateListener(premiumAuthListener)"))
        assertTrue("Must remove AuthStateListener onStop",
            src.contains("premiumAuth.removeAuthStateListener(premiumAuthListener)"))

        // 3. Saved instance state preservation
        assertTrue("Must restore owner uid from savedInstanceState in onCreate",
            src.contains("savedInstanceState?.getString(\"premium_owner_uid\")"))
        assertTrue("Must persist owner uid to outState in onSaveInstanceState",
            src.contains("outState.putString(\"premium_owner_uid\", premiumOwnerUid)"))

        // 4. Verification in onResume and loadData
        assertTrue("Must check ownership in onResume",
            src.contains("if (!hasOriginalAccount()) finish()"))
        assertTrue("Must guard loadData with ownership check",
            src.contains("if (!hasOriginalAccount()) { finish(); return }"))
    }
}
