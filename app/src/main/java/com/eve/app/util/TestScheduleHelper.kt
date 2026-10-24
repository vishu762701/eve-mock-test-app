package com.eve.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    fun formatOpensAt(availableFrom: Long, locale: Locale = Locale.getDefault()): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", locale)
        return "Opens " + sdf.format(Date(availableFrom))
    }
}
