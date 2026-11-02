package com.eve.app.ui.profile

import org.junit.Assert.*
import org.junit.Test

class QrTorchGateTest {
    @Test fun futureSuccessUpdatesStateButRapidTapIsRejected() {
        val state = QrTorchGate()
        val on = requireNotNull(state.request())
        assertFalse(state.enabled)
        assertNull(state.request())
        assertTrue(state.complete(on, true))
        assertTrue(state.enabled)
        val off = requireNotNull(state.request())
        assertFalse(off.enable)
        assertTrue(state.complete(off, true))
        assertFalse(state.enabled)
    }

    @Test fun failedFutureLeavesStateUnchanged() {
        val state = QrTorchGate()
        val pending = requireNotNull(state.request())
        assertTrue(state.complete(pending, false))
        assertFalse(state.enabled)
        assertFalse(state.complete(pending, true))
    }

    @Test fun closingOldCameraRejectsLateCallbacks() {
        val state = QrTorchGate()
        val old = requireNotNull(state.request())
        state.reset()
        val fresh = requireNotNull(state.request())
        assertFalse(state.complete(old, true))
        assertFalse(state.enabled)
        assertTrue(state.complete(fresh, true))
        assertTrue(state.enabled)
        state.reset()
        assertFalse(state.enabled)
    }
}
