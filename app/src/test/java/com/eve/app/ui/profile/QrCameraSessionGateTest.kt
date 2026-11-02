package com.eve.app.ui.profile

import org.junit.Assert.*
import org.junit.Test

class QrCameraSessionGateTest {
    @Test fun delayedProviderFromPausedSessionCannotReplaceNewBinding() {
        val gate = QrCameraSessionGate()
        gate.resume()
        val old = gate.beginBind()!!
        gate.pause()
        gate.resume()
        val current = gate.beginBind()!!
        assertFalse(gate.completeBind(old, true))
        assertTrue(gate.isBinding(current))
        assertTrue(gate.completeBind(current, true))
        assertFalse(gate.acceptsFrame(old))
        assertTrue(gate.acceptsFrame(current))
    }

    @Test fun decodedFrameQueuedBeforePauseCannotNavigateAfterResume() {
        val gate = QrCameraSessionGate()
        gate.resume()
        val ticket = gate.beginBind()!!
        assertTrue(gate.completeBind(ticket, true))
        assertTrue(gate.acceptsFrame(ticket))
        val queuedCompletion = { gate.acceptsFrame(ticket) }
        gate.pause()
        assertFalse(gate.isResumed())
        assertFalse(queuedCompletion())
        gate.resume()
        val next = gate.beginBind()!!
        assertTrue(gate.completeBind(next, true))
        assertFalse(queuedCompletion())
        assertTrue(gate.acceptsFrame(next))
    }

    @Test fun onlyOnePendingOrActiveSessionAndFailedBindCanRetry() {
        val gate = QrCameraSessionGate()
        assertNull(gate.beginBind())
        gate.resume()
        val failed = gate.beginBind()!!
        assertNull(gate.beginBind())
        assertTrue(gate.completeBind(failed, false))
        assertFalse(gate.acceptsFrame(failed))
        val retry = gate.beginBind()!!
        assertTrue(gate.completeBind(retry, true))
        assertNull(gate.beginBind())
        assertFalse(gate.completeBind(retry, true))
        assertTrue(gate.acceptsFrame(retry))
    }

    @Test fun destroyOrCloseIsTerminalDespiteDelayedResumeAndFuture() {
        val gate = QrCameraSessionGate()
        gate.resume()
        val ticket = gate.beginBind()!!
        gate.close()
        gate.close()
        gate.resume()
        assertFalse(gate.isResumed())
        assertNull(gate.beginBind())
        assertFalse(gate.isBinding(ticket))
        assertFalse(gate.completeBind(ticket, true))
        assertFalse(gate.acceptsFrame(ticket))
    }

    @Test fun duplicateResumeDoesNotInvalidateInFlightProvider() {
        val gate = QrCameraSessionGate()
        gate.resume()
        val ticket = gate.beginBind()!!
        gate.resume()
        assertTrue(gate.completeBind(ticket, true))
        gate.resume()
        assertTrue(gate.acceptsFrame(ticket))
    }
}
