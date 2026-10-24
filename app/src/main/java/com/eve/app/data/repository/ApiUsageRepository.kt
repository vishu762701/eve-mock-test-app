package com.eve.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthlyApiUsage(
    val yearMonth: String,
    val monthDisplayName: String,
    val geminiCalls: Long = 0L,
    val documentsCreatedProxy: Long = 0L,
    val testSubmissionsProxy: Long = 0L,
    val lastUpdated: Long = 0L
)

class ApiUsageRepository {

    private val firestore get() = FirebaseFirestore.getInstance()

    fun getCurrentYearMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }

    fun getMonthDisplayName(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        return sdf.format(Date())
    }

    /** Increment Gemini call counter for the current month */
    fun incrementGeminiCalls() {
        try {
            val ym = getCurrentYearMonth()
            firestore.collection("usage_stats").document(ym).set(
                mapOf(
                    "geminiCalls" to FieldValue.increment(1),
                    "month" to ym,
                    "updatedAt" to System.currentTimeMillis()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Increment document creation proxy (exams, questions, broadcasts, feedback) */
    fun incrementDocumentCreation(count: Long = 1L) {
        try {
            val ym = getCurrentYearMonth()
            firestore.collection("usage_stats").document(ym).set(
                mapOf(
                    "documentsCreated" to FieldValue.increment(count),
                    "month" to ym,
                    "updatedAt" to System.currentTimeMillis()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Alias for incrementDocumentCreation */
    fun incrementDocumentWrites(count: Int = 1) {
        incrementDocumentCreation(count.toLong())
    }

    /** Increment student test submissions proxy */
    fun incrementTestSubmissions() {
        try {
            val ym = getCurrentYearMonth()
            firestore.collection("usage_stats").document(ym).set(
                mapOf(
                    "testSubmissions" to FieldValue.increment(1),
                    "month" to ym,
                    "updatedAt" to System.currentTimeMillis()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getUsageForCurrentMonth(): MonthlyApiUsage {
        val ym = getCurrentYearMonth()
        val displayName = getMonthDisplayName()

        return try {
            val doc = firestore.collection("usage_stats").document(ym).get().await()

            var gemini = doc.getLong("geminiCalls") ?: 0L
            var docsCreated = doc.getLong("documentsCreated") ?: 0L
            var testSubmissions = doc.getLong("testSubmissions") ?: 0L
            val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

            // Calculate start of current month timestamp
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfMonth = cal.timeInMillis

            // If proxy counters in doc are 0, estimate from collections for real baseline
            if (testSubmissions == 0L) {
                try {
                    val attemptsSnap = firestore.collection("attempts")
                        .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
                        .get()
                        .await()
                    testSubmissions = attemptsSnap.size().toLong()
                } catch (_: Exception) {}
            }

            if (docsCreated == 0L) {
                try {
                    val logsSnap = firestore.collection("admin_audit_log")
                        .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
                        .get()
                        .await()
                    docsCreated = logsSnap.documents.count {
                        val t = it.getString("actionType").orEmpty()
                        t.contains("CREATED") || t.contains("SENT")
                    }.toLong().coerceAtLeast(1L)
                } catch (_: Exception) {}
            }

            MonthlyApiUsage(
                yearMonth = ym,
                monthDisplayName = displayName,
                geminiCalls = gemini,
                documentsCreatedProxy = docsCreated,
                testSubmissionsProxy = testSubmissions,
                lastUpdated = updatedAt
            )
        } catch (e: Exception) {
            MonthlyApiUsage(
                yearMonth = ym,
                monthDisplayName = displayName,
                geminiCalls = 0L,
                documentsCreatedProxy = 0L,
                testSubmissionsProxy = 0L,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
}
