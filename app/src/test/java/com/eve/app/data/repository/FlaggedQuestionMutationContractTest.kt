package com.eve.app.data.repository

import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import java.lang.reflect.Proxy
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Exercises the canonical Android report repository with a controlled Worker transport. */
class FlaggedQuestionMutationContractTest {
    private fun repository(handler: (String, Array<out Any?>) -> Any?): FlaggedQuestionRepository {
        val api = Proxy.newProxyInstance(EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)) { _, method, args ->
            handler(method.name, args ?: emptyArray())
        } as EveApiService
        return FlaggedQuestionRepository(api)
    }

    @Test fun emptyDuplicateOrMalformedReportSelectionsCannotClaimSuccess() = runBlocking {
        var calls = 0
        val repo = repository { _, _ -> calls++; ApiResponse<Unit>(true) }
        for (ids in listOf(
            emptyList(), listOf("r1", "r1"), listOf(""),
            listOf("r1", " "), listOf(" r1"), listOf("r1 "),
            List(1001) { "report-$it" }, listOf("x".repeat(129))
        )) {
            assertFalse("Unsent or invalid selection cannot be confirmed dismissed", repo.dismissFlags(ids))
        }
        assertEquals("Invalid selections must never reach the Worker", 0, calls)
    }

    @Test fun validSelectionKeepsCanonicalWorkerPayloadAndRequiresSuccessfulResponse() = runBlocking {
        val calls = mutableListOf<String>()
        val valid = repository { name, args ->
            assertEquals("dismissReportBatch", name)
            calls.add(name)
            assertEquals(mapOf("ids" to listOf("r1", "r2")), args[0])
            ApiResponse<Unit>(true)
        }
        assertTrue(valid.dismissFlags(listOf("r1", "r2"), isTechnical = true))
        assertEquals(listOf("dismissReportBatch"), calls)

        val denied = repository { _, _ -> ApiResponse<Unit>(false, error = "permission revoked") }
        assertFalse(denied.dismissFlags(listOf("r1")))
        val offline = repository { _, _ -> throw IOException("offline") }
        assertFalse(offline.dismissFlags(listOf("r1")))
    }
}
