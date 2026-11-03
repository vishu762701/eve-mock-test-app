package com.eve.app.data.repository

import com.eve.app.data.model.AdminUser
import com.eve.app.data.model.AdminUsersPagination
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.model.TestAttempt
import com.google.gson.Gson
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AdminUsersResponseContractTest {
    private val first = AdminUser(id = "uid-1", email = "student@example.test", displayName = "Student")
    private val meta = AdminUsersPagination(page = 1, pageSize = 50, total = 1,
        totalPages = 1, hasNext = false, hasPrevious = false)

    private fun repository(methodName: String, reply: Any): AdminRepository {
        val api = Proxy.newProxyInstance(EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)) { _, method, _ ->
            check(method.name == methodName) { "Unexpected ${method.name}" }
            reply
        } as EveApiService
        return AdminRepository(api)
    }

    private fun rejects(response: ApiResponse<List<AdminUser>>) = runBlocking {
        val repo = repository("getUsers", response)
        try {
            repo.getUsersPage("", 1)
            fail("Malformed Admin users response must not be shown as confirmed empty data")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().isNotBlank())
        }
    }

    @Test fun matchingPageMetadataAndValidRowsPass() = runBlocking {
        val result = repository("getUsers", ApiResponse(true, listOf(first), pagination = meta))
            .getUsersPage("", 1)
        assertEquals(listOf(first), result.users)
        assertEquals(meta, result.pagination)
        val empty = repository("getUsers", ApiResponse<List<AdminUser>>(true,
            emptyList(), pagination = AdminUsersPagination(
                page = 1, pageSize = 50, total = 0, totalPages = 0)))
            .getUsersPage("", 1)
        assertTrue(empty.users.isEmpty())
    }

    @Test fun successWithoutCompletePageOrListFailsClosed() {
        rejects(ApiResponse<List<AdminUser>>(true, data = null, pagination = meta))
        rejects(ApiResponse(true, listOf(first), pagination = null))
        rejects(ApiResponse(true, listOf(first), pagination = meta.copy(page = 2)))
        rejects(ApiResponse(true, listOf(first), pagination = meta.copy(pageSize = 25)))
        rejects(ApiResponse(true, listOf(first), pagination = meta.copy(total = -1)))
        rejects(ApiResponse(true, listOf(first), pagination = meta.copy(totalPages = 0)))
        rejects(ApiResponse(true, listOf(first), pagination = meta.copy(hasNext = true)))
        rejects(ApiResponse(true, listOf(first), pagination = meta.copy(hasPrevious = true)))
        rejects(ApiResponse(true, listOf(first), pagination = meta.copy(total = 0, totalPages = 0)))
        rejects(ApiResponse(true, List(51) { first.copy(id = "u-$it") }, pagination = meta))
    }

    @Test fun duplicateAndMissingIdentityOrDisplayFieldsAreRejected() {
        rejects(ApiResponse(true, listOf(first, first), pagination = meta.copy(total = 2)))
        rejects(ApiResponse(true, listOf(first.copy(id = "")), pagination = meta))
        val unsafe = Gson().fromJson(
            """{"id":null,"email":null,"displayName":null,"category":null}""",
            AdminUser::class.java)
        rejects(ApiResponse(true, listOf(unsafe), pagination = meta))
    }

    @Test fun banAndUnbanNeedActualMatchingServerAcknowledgement() = runBlocking {
        for (disable in listOf(true, false)) {
            val correct = repository("toggleUserStatus", ApiResponse(true,
                mapOf<String, Any>("id" to "uid-1", "disabled" to disable)))
            assertTrue(correct.toggleUserStatus("uid-1", disable))
            for (bad in listOf(
                ApiResponse<Map<String, Any>>(true, data = null),
                ApiResponse(true, mapOf<String, Any>("id" to "different", "disabled" to disable)),
                ApiResponse(true, mapOf<String, Any>("id" to "uid-1", "disabled" to !disable)),
                ApiResponse(true, mapOf<String, Any>("id" to "uid-1"))
            )) {
                try {
                    repository("toggleUserStatus", bad).toggleUserStatus("uid-1", disable)
                    fail("Unconfirmed user status must not report success")
                } catch (expected: IllegalStateException) {
                    assertTrue(expected.message.orEmpty().contains("acknowledged"))
                }
            }
        }
    }

    @Test fun missingAttemptsListMustNotPretendStudentHasNoTests() = runBlocking {
        val missing = repository("getUserAttempts",
            ApiResponse<List<TestAttempt>>(true, data = null))
        try {
            missing.getUserAttempts("uid-1")
            fail("Missing attempts data must remain an error")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("incomplete"))
        }
    }
}
