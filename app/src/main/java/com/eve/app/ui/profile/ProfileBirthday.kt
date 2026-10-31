package com.eve.app.ui.profile

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/**
 * EVE date-only birthday presentation. The source Telegram profile cells do not
 * supply an EVE DOB age contract; this is an ADAPTED application rule.
 * Leap-day anniversaries fall on March 1 in non-leap years.
 */
internal object ProfileBirthday {
    private val utc = TimeZone.getTimeZone("UTC")
    private val expression = Regex("""(\d{2})/(\d{2})/(\d{4})""")

    private fun parsed(value: String): GregorianCalendar? {
        val match = expression.matchEntire(value) ?: return null
        val (dayText, monthText, yearText) = match.destructured
        val day = dayText.toInt()
        val month = monthText.toInt()
        val year = yearText.toInt()
        if (year !in 1900..9999) return null
        return try {
            GregorianCalendar(utc, Locale.US).apply {
                isLenient = false
                clear()
                set(year, month - 1, day, 12, 0, 0)
                timeInMillis // Force strict validation of non-existent dates.
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun isValid(value: String, today: Calendar = Calendar.getInstance()): Boolean {
        val birthday = parsed(value) ?: return false
        val y = birthday.get(Calendar.YEAR)
        val m = birthday.get(Calendar.MONTH)
        val d = birthday.get(Calendar.DAY_OF_MONTH)
        val currentYear = today.get(Calendar.YEAR)
        if (y > currentYear) return false
        if (y == currentYear) {
            val todayMonth = today.get(Calendar.MONTH)
            if (m > todayMonth || (m == todayMonth && d > today.get(Calendar.DAY_OF_MONTH))) return false
        }
        return true
    }

    fun format(value: String, today: Calendar = Calendar.getInstance()): String {
        if (value.isBlank()) return "Add your birthday"
        val birthday = parsed(value) ?: return value
        if (!isValid(value, today)) return value
        val year = birthday.get(Calendar.YEAR)
        val month = birthday.get(Calendar.MONTH)
        val day = birthday.get(Calendar.DAY_OF_MONTH)
        val beforeAnniversary =
            today.get(Calendar.MONTH) < month ||
                (today.get(Calendar.MONTH) == month && today.get(Calendar.DAY_OF_MONTH) < day)
        val age = today.get(Calendar.YEAR) - year - if (beforeAnniversary) 1 else 0
        val formatted = SimpleDateFormat("MMM d, yyyy", Locale.US).apply { timeZone = utc }
            .format(birthday.time)
        return "$formatted ($age years old)"
    }
}
