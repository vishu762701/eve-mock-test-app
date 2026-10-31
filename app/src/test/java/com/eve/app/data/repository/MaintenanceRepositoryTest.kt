package com.eve.app.data.repository

import com.eve.app.data.model.AppConfig
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class MaintenanceRepositoryTest {
    private fun api(handler: (String, Array<out Any?>) -> Any?): EveApiService =
        Proxy.newProxyInstance(EveApiService::class.java.classLoader, arrayOf(EveApiService::class.java)) { _, method, args ->
            handler(method.name, args ?: emptyArray())
        } as EveApiService

    @Test fun togglesWriteOnlyMaintenanceAndRequirePublicReadback() = runBlocking {
        for (enabled in listOf(true, false)) {
            val calls = mutableListOf<String>()
            val config = AppConfig(47, enabled, "retain this", "live")
            val repo = AdminRepository(api { name, args ->
                calls.add(name)
                if (name == "updateAppConfig") assertEquals(mapOf("maintenance_mode" to enabled), args[0])
                ApiResponse(true, config)
            })
            assertEquals(config, repo.updateMaintenanceMode(enabled))
            assertEquals(listOf("updateAppConfig", "getAppConfig"), calls)
        }
    }

    @Test fun rejectedWriteMissingAckFailedReadAndConflictingReadNeverReportSuccess() = runBlocking {
        for (failure in listOf("write", "ack", "read", "conflict")) {
            var writes = 0
            val repo = AdminRepository(api { name, _ ->
                if (name == "updateAppConfig") {
                    writes++
                    when (failure) {
                        "write" -> ApiResponse<AppConfig>(false, error = "denied")
                        "ack" -> ApiResponse<AppConfig>(true)
                        else -> ApiResponse(true, AppConfig(maintenance_mode = true))
                    }
                } else when (failure) {
                    "read" -> throw java.io.IOException("offline")
                    else -> ApiResponse(true, AppConfig(maintenance_mode = false))
                }
            })
            try {
                repo.updateMaintenanceMode(true)
                fail("$failure must not be reported as a verified save")
            } catch (_: Exception) { }
            assertEquals(1, writes)
        }
    }

    @Test fun unavailablePublicConfigCannotBecomeDefaultOff() = runBlocking {
        val repo = AdminRepository(api { _, _ -> ApiResponse<AppConfig>(false, error = "unavailable") })
        try { repo.getAppConfig(); fail("Read failure must throw") } catch (_: Exception) { }
    }
    @Test fun configEditorSendsOnlyChangedFieldsAndReadsBack() = runBlocking {
        val before = AppConfig(47, false, "Keep message", "live")
        val after = before.copy(maintenance_message = "New message")
        val calls = mutableListOf<String>()
        val repo = AdminRepository(api { name, args ->
            calls.add(name)
            if (name == "updateAppConfig") assertEquals(mapOf("maintenance_message" to "New message"), args[0])
            ApiResponse(true, after)
        })
        assertTrue(repo.updateAppConfig(after, before))
        assertEquals(listOf("updateAppConfig", "getAppConfig"), calls)
    }

}
