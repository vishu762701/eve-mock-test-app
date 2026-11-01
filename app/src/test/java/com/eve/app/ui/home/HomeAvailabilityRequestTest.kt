package com.eve.app.ui.home

import com.eve.app.data.model.AppConfig
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class HomeAvailabilityRequestTest {
    @Test fun themeRecreationReusesOnlyItsOwnConfirmedOff() {
        assertTrue(HomeAvailabilityRequest.canReuseConfirmedOffOnThemeChange(true, true))
        assertFalse(HomeAvailabilityRequest.canReuseConfirmedOffOnThemeChange(true, false))
        assertFalse(HomeAvailabilityRequest.canReuseConfirmedOffOnThemeChange(false, true))
        assertFalse(HomeAvailabilityRequest.canReuseConfirmedOffOnThemeChange(false, false))
    }
    @Test fun confirmedOffNeverWaitsForAuthorization() = runBlocking {
        var authCalls = 0
        val request = HomeAvailabilityRequest({ AppConfig(maintenance_mode = false) }, {
            authCalls++; delay(Long.MAX_VALUE); true
        })
        val config = request.config()
        assertFalse(request.adminBypass(config))
        assertEquals(0, authCalls)
    }

    @Test fun unknownConfigTimesOutWithoutManufacturingOff() = runBlocking {
        val request = HomeAvailabilityRequest({ delay(Long.MAX_VALUE); AppConfig() }, { true }, 20)
        try { request.config(); fail("Unknown must remain unknown") } catch (_: IOException) { }
    }

    @Test fun confirmedOnSurvivesSlowOrDeniedAuthorizationAndCanRetryOff() = runBlocking {
        var config = AppConfig(maintenance_mode = true, maintenance_message = "Scheduled work")
        val request = HomeAvailabilityRequest({ config }, { delay(Long.MAX_VALUE); true }, adminTimeoutMs = 20)
        assertEquals("Scheduled work", request.config().maintenance_message)
        assertFalse(request.adminBypass(config))
        config = config.copy(maintenance_mode = false)
        assertFalse(request.config().maintenance_mode)
        val denied = HomeAvailabilityRequest({ config }, { false })
        assertFalse(denied.adminBypass(config.copy(maintenance_mode = true)))
        val allowed = HomeAvailabilityRequest({ config }, { true })
        assertTrue(allowed.adminBypass(config.copy(maintenance_mode = true)))
    }

    @Test fun lifecycleCancellationPropagatesAndNextResumeCanFetch() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var calls = 0
        val request = HomeAvailabilityRequest({
            if (++calls == 1) { started.complete(Unit); delay(Long.MAX_VALUE) }
            AppConfig(maintenance_mode = false)
        }, { false })
        val job = launch { request.config(); fail("Cancelled request cannot publish") }
        started.await(); job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertFalse(request.config().maintenance_mode)
    }
}
