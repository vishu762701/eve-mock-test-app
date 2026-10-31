package com.eve.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class ProfileBirthdayTest {
    private fun at(year: Int, month: Int, day: Int): Calendar =
        GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day, 12, 0)
        }

    @Test fun anniversaryCalculationDoesNotCompareDifferentYearDaysOfYear() {
        assertEquals("Mar 1, 2004 (20 years old)", ProfileBirthday.format("01/03/2004", at(2025, 2, 28)))
        assertEquals("Mar 1, 2004 (21 years old)", ProfileBirthday.format("01/03/2004", at(2025, 3, 1)))
        assertEquals("Feb 29, 2000 (24 years old)", ProfileBirthday.format("29/02/2000", at(2025, 2, 28)))
        assertEquals("Feb 29, 2000 (25 years old)", ProfileBirthday.format("29/02/2000", at(2025, 3, 1)))
    }

    @Test fun strictDateValidationAndMissingValues() {
        assertFalse(ProfileBirthday.isValid("31/02/2020", at(2025, 1, 1)))
        assertFalse(ProfileBirthday.isValid("29/02/2023", at(2025, 1, 1)))
        assertFalse(ProfileBirthday.isValid("02/01/2025", at(2025, 1, 1)))
        assertTrue(ProfileBirthday.isValid("29/02/2024", at(2025, 1, 1)))
        assertEquals("Add your birthday", ProfileBirthday.format(""))
        assertEquals("31/02/2020", ProfileBirthday.format("31/02/2020"))
    }

    @Test fun profileDoesNotClaimSaveUntilOwnerReadbackMatches() {
        val source = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        val save = source.substringAfter("private fun saveBirthday(dob: String)")
            .substringBefore("private fun formatTelegramBirthday(")
        assertTrue(save.indexOf("ApiClient.apiService.getProfile(ProfileRequestOwner(userId))") < save.indexOf("currentDob = dob"))
        assertTrue(save.contains("confirmed.data?.id == userId"))
        assertTrue(save.contains("confirmed.data.dob == dob"))
        assertTrue(save.contains("FirebaseAuth.getInstance().currentUser?.uid == userId"))
        assertTrue(save.contains("birthdaySaveInFlight = false"))
    }
}
