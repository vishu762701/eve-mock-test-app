package com.eve.app.data.repository

import com.eve.app.data.model.AdminAuditLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar

enum class AuditLogTimeRange {
    TODAY,
    LAST_7_DAYS,
    ALL_TIME
}

class AuditLogRepository {

    private val firestore get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    fun recordLog(
        actionType: String,
        description: String,
        adminEmail: String? = null
    ) {
        try {
            val email = adminEmail?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.email
                ?: "admin@eve.app"

            val log = hashMapOf(
                "actionType" to actionType,
                "description" to description,
                "adminEmail" to email,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("admin_audit_log").add(log)
        } catch (e: Exception) {
            // Non-blocking fallback: never fail admin core workflows if audit logging fails
            e.printStackTrace()
        }
    }

    suspend fun getLogs(timeRange: AuditLogTimeRange = AuditLogTimeRange.ALL_TIME): List<AdminAuditLog> {
        return try {
            val cutoff = when (timeRange) {
                AuditLogTimeRange.TODAY -> {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    cal.timeInMillis
                }
                AuditLogTimeRange.LAST_7_DAYS -> {
                    System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
                }
                AuditLogTimeRange.ALL_TIME -> 0L
            }

            val query = if (cutoff > 0L) {
                firestore.collection("admin_audit_log")
                    .whereGreaterThanOrEqualTo("timestamp", cutoff)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(200)
            } else {
                firestore.collection("admin_audit_log")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(200)
            }

            val snap = query.get().await()
            snap.documents.mapNotNull { doc ->
                val id = doc.id
                val type = doc.getString("actionType").orEmpty()
                val desc = doc.getString("description").orEmpty()
                val email = doc.getString("adminEmail").orEmpty()
                val time = doc.getLong("timestamp") ?: 0L
                AdminAuditLog(
                    id = id,
                    actionType = type,
                    description = desc,
                    adminEmail = email,
                    timestamp = time
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
