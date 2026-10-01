package com.eve.app.data.repository

import android.util.Log
import com.eve.app.data.model.AdminAuditLog
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.CreateAuditLogRequest
import com.eve.app.data.remote.EveApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AuditLogTimeRange {
    TODAY,
    LAST_7_DAYS,
    ALL_TIME
}

class AuditLogRepository(
    private val api: EveApiService = ApiClient.api
) {

    companion object {
        // Lifecycle-safe application scope for non-blocking fire-and-forget audit logging
        private val auditScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    fun recordLog(
        actionType: String,
        description: String,
        adminEmail: String? = null
    ) {
        auditScope.launch {
            try {
                api.createAuditLog(
                    CreateAuditLogRequest(
                        actionType = actionType,
                        description = description,
                        adminEmail = adminEmail
                    )
                )
            } catch (e: Exception) {
                // Non-blocking fallback: never fail admin core workflows if audit logging fails
                Log.w("AuditLogRepo", "Failed to record audit log: ${e.message}")
            }
        }
    }

    suspend fun getLogs(timeRange: AuditLogTimeRange = AuditLogTimeRange.ALL_TIME): List<AdminAuditLog> = withContext(Dispatchers.IO) {
        try {
            val rangeParam = when (timeRange) {
                AuditLogTimeRange.TODAY -> "today"
                AuditLogTimeRange.LAST_7_DAYS -> "7d"
                AuditLogTimeRange.ALL_TIME -> "all"
            }
            val response = api.getAdminAuditLogs(range = rangeParam)
            if (response.success && response.data != null) {
                response.data.map { dto ->
                    AdminAuditLog(
                        id = dto.id,
                        actionType = dto.actionType,
                        description = dto.description,
                        adminEmail = dto.adminEmail,
                        timestamp = dto.timestamp
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("AuditLogRepo", "Failed to fetch audit logs", e)
            emptyList()
        }
    }
}
