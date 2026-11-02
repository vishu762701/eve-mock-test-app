package com.eve.app.data.repository

import com.eve.app.data.model.Poll
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PollResponseContractTest {
    private fun repo(response: Any, observe: (String, Array<out Any?>?) -> Unit = { _, _ -> }): PollRepository {
        val api = Proxy.newProxyInstance(EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)) { _, method, args ->
            observe(method.name, args)
            if (response is Throwable) throw response
            response
        } as EveApiService
        return PollRepository(api)
    }

    @Test fun deniedAndMalformedListsCannotBecomeAnEmptySuccess() = runBlocking {
        for (response in listOf(
            ApiResponse<List<Poll>>(false, null, error = "Access revoked"),
            ApiResponse<List<Poll>>(false, listOf(Poll(id="stale")), error="Access revoked"),
            ApiResponse<List<Poll>>(true, null))) {
            try { repo(response).getPolls(); fail("Failure/malformed response became an empty or stale success") }
            catch (_: IllegalStateException) { }
        }
    }

    @Test fun confirmedEmptyAndValidListsRemainUsable() = runBlocking {
        assertEquals(emptyList<Poll>(), repo(ApiResponse(true, emptyList<Poll>())).getPolls())
        val active = Poll(id="active", active=true, endsAt=0)
        val closed = Poll(id="closed", active=false, endsAt=0)
        assertEquals(listOf(active, closed), repo(ApiResponse(true,listOf(closed,active))).getPolls())
    }

    @Test fun createCannotSucceedWithoutANonblankServerIdentity() = runBlocking {
        for (response in listOf(
            ApiResponse<Map<String,String>>(false, null, error="Permission denied"),
            ApiResponse<Map<String,String>>(true, null),
            ApiResponse(true, emptyMap<String,String>()),
            ApiResponse(true, mapOf("id" to "  ")))) {
            try { repo(response).createPoll("Question",listOf("A","B"),0,true,"admin@example.test")
                fail("Unconfirmed poll creation reported success") }
            catch (_: IllegalStateException) { }
        }
    }

    @Test fun confirmedCreationPreservesExistingPayloadAndReturnsServerId() = runBlocking {
        var sent: Map<*,*>? = null
        val repository = repo(ApiResponse(true,mapOf("id" to "server-poll-id"))) { method, args ->
            assertEquals("createPoll", method)
            sent = args!!.first() as Map<*,*>
        }
        assertEquals("server-poll-id",repository.createPoll(" Question ",listOf(" A ","B"),123,true,"admin@example.test"))
        assertEquals(mapOf("question" to "Question", "options" to listOf("A","B"),
            "endsAt" to 123L,"active" to true,"createdBy" to "admin@example.test"),sent)
    }

    @Test fun permissionFailuresAndCancellationPropagateToActualUiErrorHandling() = runBlocking {
        val denied = repo(ApiResponse<Any>(false,null,error="Permission denied"))
        for (action in listOf<suspend () -> Unit>({denied.updatePollStatus("poll",false)}, {denied.deletePoll("poll")})) {
            try { action(); fail("Unconfirmed mutation succeeded") } catch (_: IllegalStateException) { }
        }
        val cancelled = repo(CancellationException("Screen closed"))
        try { cancelled.getPolls(); fail("Cancellation swallowed") } catch (_: CancellationException) { }
        try { cancelled.createPoll("Q",listOf("A","B"),0,true,"admin@example.test"); fail("Cancellation swallowed") }
        catch (_: CancellationException) { }
    }
}
