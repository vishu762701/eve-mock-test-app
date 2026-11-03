package com.eve.app.data.repository

import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.remote.FloatingLinkResponse
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Drives the production repository with only its external Retrofit transport replaced. */
class FloatingLinkResponseContractTest {
    private fun repository(answer: Any): FloatingLinkRepository {
        val api = Proxy.newProxyInstance(
            EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)
        ) { _, _, _ ->
            if (answer is Throwable) throw answer
            answer
        } as EveApiService
        return FloatingLinkRepository(api)
    }

    @Test fun deniedResponsesCannotBePresentedAsConfirmedWrites() = runBlocking {
        val denied = repository(ApiResponse<FloatingLinkResponse>(
            success = false, error = "No longer authorized"
        ))
        assertTrue(denied.updateFloatingLink("https://example.org").isFailure)
        assertTrue(denied.clearFloatingLink().isFailure)
        assertTrue(denied.updateFloatingLinkPosition(0.4f, 0.5f).isFailure)
        assertTrue(denied.getAdminFloatingLink().isFailure)
    }

    @Test fun missingAndContradictoryAcknowledgementsFailClosed() = runBlocking {
        val missing = repository(ApiResponse<FloatingLinkResponse>(success = true, data = null))
        assertTrue(missing.updateFloatingLink("https://example.org").isFailure)
        assertTrue(missing.clearFloatingLink().isFailure)
        assertTrue(missing.updateFloatingLinkPosition(0.2f, 0.9f).isFailure)

        val empty = repository(ApiResponse(success = true, data = FloatingLinkResponse(url = "")))
        assertTrue(empty.updateFloatingLink("https://example.org").isFailure)

        val nonEmpty = repository(ApiResponse(success = true, data = FloatingLinkResponse(url = "https://example.org")))
        assertTrue(nonEmpty.clearFloatingLink().isFailure)
    }

    @Test fun confirmedResponsesKeepServerValuesAndEmptyClear() = runBlocking {
        val saved = repository(ApiResponse(
            success = true,
            data = FloatingLinkResponse(url = "https://example.org/path", x = 0.3f, y = 0.6f)
        ))
        assertEquals("https://example.org/path",
            saved.updateFloatingLink("https://example.org/path").getOrThrow())
        assertEquals(0.3f, saved.updateFloatingLinkPosition(0.3f, 0.6f).getOrThrow().x ?: -1f, 0.0001f)
        val cleared = repository(ApiResponse(success = true, data = FloatingLinkResponse(url = "")))
        assertTrue(cleared.clearFloatingLink().isSuccess)
        assertEquals("", cleared.getAdminFloatingLink().getOrThrow())
    }

    @Test fun cancellationIsNotConvertedToResultFailure() = runBlocking {
        val interrupted = repository(CancellationException("Navigation stopped"))
        for (call in listOf<suspend () -> Any>(
            { interrupted.updateFloatingLink("https://example.org") },
            { interrupted.clearFloatingLink() },
            { interrupted.updateFloatingLinkPosition(0.2f, 0.8f) },
            { interrupted.getAdminFloatingLink() }
        )) {
            try {
                call()
                fail("Cancellation must propagate to the Activity")
            } catch (_: CancellationException) { }
        }
    }
}
