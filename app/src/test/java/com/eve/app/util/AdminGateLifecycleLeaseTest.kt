package com.eve.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class AdminGateLifecycleLeaseTest {
    @Test fun supersededRunCannotAuthorizeAndReleasesCallbackExactlyOnce() {
        var releases = 0
        val previous = AdminGateLifecycleLease { releases++ }
        assertTrue(previous.isActive)
        previous.invalidate()
        assertFalse(previous.isActive)
        previous.invalidate()
        assertEquals(1, releases)

        val fresh = AdminGateLifecycleLease { releases++ }
        assertTrue(fresh.isActive)
        assertFalse(previous.isActive)
        fresh.invalidate()
        assertEquals(2, releases)
    }

    @Test fun productionGateInvalidatesPreviousListenerAndPendingRequest() {
        val gate = File("src/main/java/com/eve/app/util/AdminAccessGate.kt").readText()
        assertTrue(gate.contains("protectedRoot.getTag(R.id.eve_admin_gate_lifetime)"))
        assertTrue(gate.contains("?.invalidate()"))
        assertTrue(gate.contains("authorizationJob?.cancel()"))
        assertTrue(gate.contains("auth.removeAuthStateListener(identityListener)"))
        assertTrue(gate.contains("activity.lifecycle.removeObserver(it)"))
        assertTrue(gate.contains("if (!lease.isActive || protectedRoot.getTag("))
        assertTrue(gate.contains("lease.invalidate()"))
        val activity = File("src/main/java/com/eve/app/ui/admin/ManageUsersActivity.kt").readText()
        assertTrue(activity.contains("override fun onResume()"))
        assertTrue(activity.contains("requireFreshServer = true"))
    }
}
