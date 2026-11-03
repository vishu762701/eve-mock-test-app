package com.eve.app.data.repository

import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FeedbackFailureTest {
    private fun repo(response: Any): FeedbackRepository {
        val api = Proxy.newProxyInstance(EveApiService::class.java.classLoader, arrayOf(EveApiService::class.java)) { _, _, _ ->
            if (response is Throwable) throw response
            response
        } as EveApiService
        return FeedbackRepository(api)
    }

    @Test fun failedLoadsAreNeverReportedAsEmptySuccess() = runBlocking {
        for (repository in listOf(repo(ApiResponse<Any>(false,null,error="Access revoked")),repo(java.io.IOException("Offline")))) {
            for (read in listOf<suspend () -> Any>({ repository.getFeedbackPosts() },{ repository.getRepliesForPost("post") },{ repository.getFeedbackMessages() })) {
                try { read(); fail("Failed load became empty success") } catch (_: Exception) { }
            }
        }
    }

    @Test fun cancelledMutationsRemainCancelled() = runBlocking {
        try { repo(CancellationException("Closed screen")).markAsRead("message"); fail("Cancellation was swallowed") }
        catch (_: CancellationException) { }
    }

    @Test fun malformedFeedbackCreateAcknowledgementsNeverBecomeSuccess() = runBlocking {
        for (response in listOf(
            ApiResponse<Map<String, String>>(success = true, data = null),
            ApiResponse(success = true, data = emptyMap<String, String>()),
            ApiResponse(success = true, data = mapOf("id" to "")),
            ApiResponse(success = true, data = mapOf("id" to "  ")),
            ApiResponse(success = false, data = mapOf("id" to "incorrect"), error = "Denied")
        )) {
            val result = repo(response).createFeedbackPost("Announcement", "Message", "uid", "admin@example.org")
            assertTrue("Missing/malformed Worker ID must not confirm publication", result.isFailure)
        }
    }

    @Test fun validFeedbackCreateIdIsPreservedAndCancellationPropagates() = runBlocking {
        val confirmed = repo(ApiResponse(success = true, data = mapOf("id" to "worker-created-id")))
            .createFeedbackPost("Announcement", "Message", "uid", "admin@example.org")
        assertEquals("worker-created-id", confirmed.getOrThrow())
        try {
            repo(CancellationException("Screen stopped"))
                .createFeedbackPost("Title", "Message", "uid", "admin@example.org")
            fail("Cancellation must not become an ordinary Result failure")
        } catch (_: CancellationException) { }
    }

    @Test fun failedMutationsAreReturnedForUiRollback() = runBlocking {
        val repository = repo(ApiResponse<Any>(false,null,error="No longer available"))
        assertTrue(repository.markReplyAsRead("post","reply").isFailure)
        assertTrue(repository.deleteSingleReply("post","reply").isFailure)
        assertTrue(repository.deleteFeedbackPost("post").isFailure)
        assertTrue(repository.replyFeedbackMessage("message","Reply").isFailure)
    }
}
