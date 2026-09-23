package com.eve.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Phase 20: Daily GK dates hamesha IST (Asia/Kolkata) me count hote hain —
 * taaki midnight ke aas-paas device timezone se "aaj" change na ho.
 */
object DateUtil {

    private val ist: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    private fun isoFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = ist }

    private fun displayFormat() = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = ist }

    private fun weekdayFormat() = SimpleDateFormat("EEEE", Locale.ENGLISH).apply { timeZone = ist }

    fun todayIso(): String = isoFormat().format(Date())

    fun yesterdayIso(): String {
        val cal = Calendar.getInstance(ist)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return isoFormat().format(cal.time)
    }

    fun display(isoDate: String): String = try {
        val parsed = isoFormat().parse(isoDate) ?: return isoDate
        displayFormat().format(parsed)
    } catch (_: Exception) {
        isoDate
    }

    fun weekday(isoDate: String): String = try {
        val parsed = isoFormat().parse(isoDate) ?: return ""
        weekdayFormat().format(parsed)
    } catch (_: Exception) {
        ""
    }

    fun isToday(isoDate: String): Boolean = isoDate == todayIso()
}
