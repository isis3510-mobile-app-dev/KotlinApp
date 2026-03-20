package com.example.petcare.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
}
