package com.eve.app.ui.premium

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentCheckoutActivityOwnerLifecycleTest {
    @Test
    fun activeCheckoutRequiresOriginalSignedInAccount() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("uid-student-1", "uid-student-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-student-1", "uid-student-2"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("uid-student-1", null))
        assertFalse(SubmissionRetryPolicy.ownsSubmission(null, "uid-student-1"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("", "uid-student-1"))
    }

    @Test
    fun checkoutActivityEnforcesLifecycleAccountGuardAndRestoration() {
        val src = File("src/main/java/com/eve/app/ui/premium/PaymentCheckoutActivity.kt").readText()

        // 1. Ownership check and listener declaration
        assertTrue("Must declare checkoutAuthListener",
            src.contains("private val checkoutAuthListener = FirebaseAuth.AuthStateListener"))
        assertTrue("Must check hasOriginalAccount in listener",
            src.contains("if (checkoutOwnerUid != null && !hasOriginalAccount()) finish()"))

        // 2. Auth state listener registration in onStart and removal in onStop
        assertTrue("Must add AuthStateListener onStart",
            src.contains("checkoutAuth.addAuthStateListener(checkoutAuthListener)"))
        assertTrue("Must remove AuthStateListener onStop",
            src.contains("checkoutAuth.removeAuthStateListener(checkoutAuthListener)"))

        // 3. Saved instance state preservation
        assertTrue("Must restore owner uid from savedInstanceState in onCreate",
            src.contains("savedInstanceState?.getString(\"checkout_owner_uid\")"))
        assertTrue("Must persist owner uid to outState in onSaveInstanceState",
            src.contains("outState.putString(\"checkout_owner_uid\", checkoutOwnerUid)"))

        // 4. Verification in onResume
        assertTrue("Must check ownership in onResume",
            src.contains("if (!hasOriginalAccount()) finish()"))
    }
}
