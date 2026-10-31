package com.eve.app.data.repository

import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.AuthUserResponse
import com.eve.app.data.remote.EveApiService
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminWorkerAuthorityTest {
    private fun repository(response: ApiResponse<AuthUserResponse>): AdminRepository {
        val api = Proxy.newProxyInstance(EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)) { _, method, _ ->
            check(method.name == "getMe")
            response
        } as EveApiService
        return AdminRepository(api)
    }

    @Test fun onlyTheMatchingWorkersSuccessfulAdminDecisionAuthorizes() = runBlocking {
        val owner = AuthUserResponse("uid", "admin@example.test", "Admin", true)
        assertTrue(repository(ApiResponse(true, owner)).isAdminFresh("uid", "ADMIN@example.test"))
        assertFalse(repository(ApiResponse(true, owner)).isAdminFresh("other", owner.email))
        assertFalse(repository(ApiResponse(true, owner)).isAdminFresh(owner.uid, "other@example.test"))
        assertFalse(repository(ApiResponse(false, owner)).isAdminFresh(owner.uid, owner.email))
        assertFalse(repository(ApiResponse(true, owner.copy(isAdmin = false))).isAdminFresh(owner.uid, owner.email))
        assertFalse(repository(ApiResponse<AuthUserResponse>(true, null)).isAdminFresh(owner.uid, owner.email))
    }
}
