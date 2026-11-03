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

        val response = api.getApiUsage(ym)
        check(response.success && response.error.isNullOrBlank()) { response.error ?: "Failed to retrieve API usage" }
        val data = checkNotNull(response.data) { "Missing confirmed API usage" }
        check(data.yearMonth == ym && data.monthDisplayName.isNotBlank()) { "Invalid API usage month" }
        check(listOf(data.geminiCalls, data.geminiSuccessfulCalls, data.geminiFailedCalls,
            data.documentsCreated, data.testSubmissions).all { it >= 0 } && data.lastUpdated > 0) {
            "Invalid API usage counters"
        }
        check(Math.addExact(data.geminiSuccessfulCalls, data.geminiFailedCalls) == data.geminiCalls) {
            "Inconsistent API usage totals"
        }
        return MonthlyApiUsage(
            yearMonth = data.yearMonth,
            monthDisplayName = data.monthDisplayName,
            geminiCalls = data.geminiCalls,
            documentsCreatedProxy = data.documentsCreated,
            testSubmissionsProxy = data.testSubmissions,
            lastUpdated = data.lastUpdated,
            countedSince = data.countedSince.orEmpty()
        )
    }
}
