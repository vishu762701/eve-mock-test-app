package com.eve.app.data.repository

import com.eve.app.data.model.TestAttempt
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HistoryMalformedResponseTest {
    private fun repository(reply: Any): HistoryRepository {
        val api = Proxy.newProxyInstance(
            EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)
        ) { _, method, _ ->
            check(method.name == "getAttempts") { "Unexpected ${method.name}" }
            reply
        } as EveApiService
        return HistoryRepository(api)
    }

    @Test fun genuineEmptyHistoryRemainsAnEmptyList() = runBlocking {
        assertTrue(repository(ApiResponse(true, emptyList<TestAttempt>())).getAttempts("student-a").isEmpty())
    }

    @Test fun serverSuccessWithoutDataFailsClosedInsteadOfShowingNoAttempts() = runBlocking {
        val repo = repository(ApiResponse<List<TestAttempt>>(true, data = null))
        try {
            repo.getAttempts("student-a")
            fail("Missing server payload cannot be displayed as zero attempts")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("missing"))
        }
    }

    @Test fun sortedHistoryStillPreservesServerRows() = runBlocking {
        val first = TestAttempt(id = "old", userId = "student-a", timestamp = 100)
        val next = TestAttempt(id = "new", userId = "student-a", timestamp = 300)
        val all = repository(ApiResponse(true, listOf(first, next))).getAttempts("student-a")
        assertEquals(listOf("new", "old"), all.map { it.id })
    }
}
