package com.example.petcare.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

class EventDateUtilsTest {

    @Test
    fun `parseEventDate handles ISO date and date-only values`() {
        assertTrue(EventDateUtils.parseEventDate("2026-03-20") == LocalDate.of(2026, 3, 20))
        assertNotNull(EventDateUtils.parseEventDate("2026-03-20T15:30:00Z"))
    }

    @Test
    fun `isTodayOrFuture excludes past and includes today or future`() {
        val today = LocalDate.of(2026, 3, 20)
        assertFalse(EventDateUtils.isTodayOrFuture("2026-03-19", today))
        assertTrue(EventDateUtils.isTodayOrFuture("2026-03-20", today))
        assertTrue(EventDateUtils.isTodayOrFuture("2026-03-21", today))
    }

    @Test
    fun `toIsoFromAppDateTime returns null on invalid date`() {
        val iso = EventDateUtils.toIsoFromAppDateTime(
            appDate = "99/99/2026",
            appTime = "10:00 AM",
            fallbackRaw = null
        )
        assertTrue(iso == null)
    }

    @Test
    fun `splitToAppDateTime keeps UTC midnight timestamps as date-only`() {
        val (appDate, appTime) = EventDateUtils.splitToAppDateTime("2026-03-20T00:00:00Z")
        assertEquals("20/03/2026", appDate)
        assertEquals("12:00 AM", appTime)
    }

    @Test
    fun `splitToAppDateTime treats naive backend datetimes as UTC`() {
        val originalTz = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"))
            val (appDate, appTime) = EventDateUtils.splitToAppDateTime("2026-03-20T16:00:00")
            assertEquals("20/03/2026", appDate)
            assertEquals("11:00 AM", appTime)
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }
}
