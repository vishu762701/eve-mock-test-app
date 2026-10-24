package com.eve.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApiUsageStats(
    val month: String = "",
    val geminiCalls: Long = 0L,
    val documentWrites: Long = 0L,
    val testSubmissions: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getFormattedMonth(): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.US)
            val d = parser.parse(month)
            if (d != null) formatter.format(d) else month
        } catch (e: Exception) {
            month
        }
    }
}
