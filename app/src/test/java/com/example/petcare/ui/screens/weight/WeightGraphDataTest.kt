package com.example.petcare.ui.screens.weight

import com.example.petcare.data.model.WeightLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeightGraphDataTest {

    @Test
    fun `logsForGraph sorts included logs oldest to newest`() {
        val today = LocalDate.of(2026, 4, 24)
        val logs = listOf(
            log("new", "2026-04-24T00:00:00Z", 12.0),
            log("old", "2026-04-18T00:00:00Z", 10.0),
            log("middle", "2026-04-21T00:00:00Z", 11.0)
        )

        val result = logsForGraph(logs, WeightGraphRange.WEEK, today)

        assertEquals(listOf("old", "middle", "new"), result.map { it.id })
    }

    @Test
    fun `logsForGraph filters weekly monthly yearly and all time windows`() {
        val today = LocalDate.of(2026, 4, 24)
        val logs = listOf(
            log("week", "2026-04-18T00:00:00Z", 10.0),
            log("month", "2026-03-25T00:00:00Z", 11.0),
            log("year", "2025-04-25T00:00:00Z", 12.0),
            log("tooOld", "2025-04-24T00:00:00Z", 13.0)
        )

        assertEquals(listOf("week"), logsForGraph(logs, WeightGraphRange.WEEK, today).map { it.id })
        assertEquals(listOf("month", "week"), logsForGraph(logs, WeightGraphRange.MONTH, today).map { it.id })
        assertEquals(listOf("year", "month", "week"), logsForGraph(logs, WeightGraphRange.YEAR, today).map { it.id })
        assertEquals(listOf("tooOld", "year", "month", "week"), logsForGraph(logs, WeightGraphRange.ALL_TIME, today).map { it.id })
    }

    @Test
    fun `latestWeightLog uses loggedAt date instead of input order`() {
        val logs = listOf(
            log("firstInList", "2026-04-20T00:00:00Z", 10.0),
            log("latest", "2026-04-24T00:00:00Z", 12.0),
            log("older", "2026-04-19T00:00:00Z", 9.0)
        )

        assertEquals("latest", latestWeightLog(logs)?.id)
    }

    @Test
    fun `graph windows expose labels for each expected view`() {
        val today = LocalDate.of(2026, 4, 24)

        assertEquals(7, graphWindowFor(WeightGraphRange.WEEK, today).axisLabels.size)
        assertEquals(5, graphWindowFor(WeightGraphRange.MONTH, today).axisLabels.size)
        assertTrue(graphWindowFor(WeightGraphRange.YEAR, today).axisLabels.size <= 5)
        assertEquals(
            5,
            graphWindowFor(
                range = WeightGraphRange.ALL_TIME,
                today = today,
                logs = listOf(
                    log("old", "2024-04-24T00:00:00Z", 10.0),
                    log("new", "2026-04-24T00:00:00Z", 12.0)
                )
            ).axisLabels.size
        )
    }

    private fun log(id: String, loggedAt: String, weight: Double) = WeightLog(
        id = id,
        petId = "pet",
        ownerId = "owner",
        weight = weight,
        loggedAt = loggedAt
    )
}
