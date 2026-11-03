package com.eve.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import java.util.TimeZone

object TestScheduleHelper {

    fun calculateSkew(serverNow: Long, localNow: Long = System.currentTimeMillis()): Long {
        return serverNow - localNow
    }

    fun isLocked(availableFrom: Long, serverNow: Long, localNow: Long = System.currentTimeMillis()): Boolean {
        if (availableFrom <= 0) return false
        val skew = calculateSkew(serverNow, localNow)
        val adjustedNow = localNow + skew
        return availableFrom > adjustedNow
    }

    /**
     * MaterialDatePicker emits a UTC date marker, not a local wall-clock instant.
     * Preserve the chosen calendar day while interpreting TimePicker's hour and
     * minute in the device's local zone. Strict calendar arithmetic rejects
     * nonexistent daylight-saving wall times instead of silently shifting hours.
     */
    fun localOpeningTimeFromPicker(
        selectedUtcDateMillis: Long,
        hourOfDay: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        require(hourOfDay in 0..23 && minute in 0..59) { "Invalid opening time" }
        val pickedUtcDay = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = selectedUtcDateMillis
        }
        val local = Calendar.getInstance(timeZone).apply {
            clear()
            isLenient = false
            set(
                pickedUtcDay.get(Calendar.YEAR),
                pickedUtcDay.get(Calendar.MONTH),
                pickedUtcDay.get(Calendar.DAY_OF_MONTH),
                hourOfDay,
                minute,
                0
            )
        }
        return local.timeInMillis
    }

    fun formatOpensAt(availableFrom: Long, locale: Locale = Locale.getDefault()): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", locale)
        return "Opens " + sdf.format(Date(availableFrom))
    }
}
