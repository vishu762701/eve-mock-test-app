package com.eve.app.util

import java.time.Instant
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Material date markers are UTC days; the chosen time is a local wall clock. */
class TestSchedulePickerTimeZoneTest {
    private val selectedNewYearDay = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()

    @Test fun indiaNineAmRemainsNineAmNotUtcNineAm() {
        val result = TestScheduleHelper.localOpeningTimeFromPicker(
            selectedNewYearDay, 9, 0, TimeZone.getTimeZone("Asia/Kolkata")
        )
        assertEquals(Instant.parse("2026-01-01T03:30:00Z").toEpochMilli(), result)
    }

    @Test fun newYearDayUsesSelectedCalendarDateAcrossUtcAndAmerica() {
        assertEquals(
            Instant.parse("2026-01-01T09:00:00Z").toEpochMilli(),
            TestScheduleHelper.localOpeningTimeFromPicker(
                selectedNewYearDay, 9, 0, TimeZone.getTimeZone("UTC")
            )
        )
        assertEquals(
            Instant.parse("2026-01-01T17:00:00Z").toEpochMilli(),
            TestScheduleHelper.localOpeningTimeFromPicker(
                selectedNewYearDay, 9, 0, TimeZone.getTimeZone("America/Los_Angeles")
            )
        )
    }

    @Test fun springForwardNonexistentLocalTimeFailsInsteadOfSchedulingWrongHour() {
        val springDate = Instant.parse("2026-03-08T00:00:00Z").toEpochMilli()
        try {
            TestScheduleHelper.localOpeningTimeFromPicker(
                springDate, 2, 30, TimeZone.getTimeZone("America/Los_Angeles")
            )
            fail("Missing DST hour must not become 03:30 silently")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().isNotBlank())
        }
    }

    @Test fun invalidPickerHoursAndMinutesCannotBeSent() {
        for ((hour, minute) in listOf(-1 to 0, 24 to 0, 9 to -1, 9 to 60)) {
            try {
                TestScheduleHelper.localOpeningTimeFromPicker(
                    selectedNewYearDay, hour, minute, TimeZone.getTimeZone("UTC")
                )
                fail("Invalid time must fail")
            } catch (_: IllegalArgumentException) { }
        }
    }
}
