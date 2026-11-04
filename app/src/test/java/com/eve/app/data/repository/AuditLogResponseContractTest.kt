package com.eve.app.data.repository

import com.eve.app.data.remote.AdminAuditLogDto
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import java.io.IOException
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AuditLogResponseContractTest {
    private fun repository(methodName: String, result: Any? = null, failure: Throwable? = null):
        AuditLogRepository {
        val api = Proxy.newProxyInstance(EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)) { _, method, _ ->
            check(method.name == methodName) { "Unexpected ${method.name}" }
            failure?.let { throw it }
            result
        } as EveApiService
        return AuditLogRepository(api)
    }

    private suspend fun rejectsLogs(repo: AuditLogRepository) {
        try {
            repo.getLogs()
            fail("A denied or incomplete audit read must not look like empty history")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().isNotBlank())
        }
    }

    @Test fun confirmedEmptyAndPopulatedAuditHistoryRemainDistinctFromErrors() = runBlocking {
        val empty = repository("getAdminAuditLogs",
            ApiResponse(true, emptyList<AdminAuditLogDto>()))
        assertEquals(emptyList<Any>(), empty.getLogs())
        val audit = AdminAuditLogDto(id = "log-1", actionType = "CREATE",
            adminEmail = "admin@example.test", timestamp = 1234L)
        val rows = repository("getAdminAuditLogs", ApiResponse(true, listOf(audit))).getLogs()
        assertEquals(1, rows.size)
        assertEquals("log-1", rows.single().id)
    }

    @Test fun failedAndMalformedAuditReadsFailClosed() = runBlocking {
        rejectsLogs(repository("getAdminAuditLogs",
            ApiResponse<List<AdminAuditLogDto>>(false, data = emptyList(), error = "Denied")))
        rejectsLogs(repository("getAdminAuditLogs",
            ApiResponse<List<AdminAuditLogDto>>(true, data = null)))
    }

    @Test fun auditTransportFailuresPropagate() = runBlocking {
        try {
            repository("getAdminAuditLogs", failure = IOException("Offline")).getLogs()
            fail("Network failure must not report empty audit history")
        } catch (expected: IOException) {
            assertEquals("Offline", expected.message)
        }
    }

    @Test fun actionTypeReadNeedsConfirmedPayload() = runBlocking {
        val available = repository("getAuditLogTypes",
            ApiResponse(true, listOf("CREATE", "DELETE")))
        assertEquals(listOf("CREATE", "DELETE"), available.getActionTypes())
        for (bad in listOf(
            ApiResponse<List<String>>(false, data = emptyList(), error = "Denied"),
            ApiResponse<List<String>>(true, data = null)
        )) {
            try {
                repository("getAuditLogTypes", bad).getActionTypes()
                fail("Action types cannot masquerade as empty on failure")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().isNotBlank())
            }
        }
    }
}
