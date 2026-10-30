package com.eve.app.data.repository

import com.eve.app.data.remote.ApiClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MonthlyApiUsage(
    val yearMonth: String,
    val monthDisplayName: String,
    val geminiCalls: Long = 0L,
    val documentsCreatedProxy: Long = 0L,
    val testSubmissionsProxy: Long = 0L,
    val lastUpdated: Long = 0L,
    val countedSince: String = ""
)

class ApiUsageRepository(
    private val api: com.eve.app.data.remote.EveApiService = ApiClient.apiService
) {

    fun getCurrentYearMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }

    fun getMonthDisplayName(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        return sdf.format(Date())
    }

    /** No-op: Gemini usage is recorded server-authoritatively on Cloudflare Worker */
    fun incrementGeminiCalls() {}

    /** No-op: Document creation is tracked server-authoritatively on Cloudflare D1 */
    fun incrementDocumentCreation(count: Long = 1L) {}

    /** No-op alias for incrementDocumentCreation */
    fun incrementDocumentWrites(count: Int = 1) {}

    /** No-op: Student test submissions are counted directly from D1 attempts */
    fun incrementTestSubmissions() {}

    /**
     * Fetches server-authoritative monthly usage counters from Cloudflare Worker / D1.
     * All counting is performed on the server where the operations occur.
     */
    suspend fun getUsageForCurrentMonth(): MonthlyApiUsage {
        val ym = getCurrentYearMonth()
        val displayName = getMonthDisplayName()

        return try {
            val response = api.getApiUsage(ym)
            if (response.success && response.data != null) {
                val data = response.data
                MonthlyApiUsage(
                    yearMonth = data.yearMonth.ifBlank { ym },
                    monthDisplayName = data.monthDisplayName.ifBlank { displayName },
                    geminiCalls = data.geminiCalls,
                    documentsCreatedProxy = data.documentsCreated,
                    testSubmissionsProxy = data.testSubmissions,
                    lastUpdated = data.lastUpdated,
                    countedSince = data.countedSince.orEmpty()
                )
            } else {
                MonthlyApiUsage(
                    yearMonth = ym,
                    monthDisplayName = displayName,
                    geminiCalls = 0L,
                    documentsCreatedProxy = 0L,
                    testSubmissionsProxy = 0L,
                    lastUpdated = System.currentTimeMillis()
                )
            }
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
