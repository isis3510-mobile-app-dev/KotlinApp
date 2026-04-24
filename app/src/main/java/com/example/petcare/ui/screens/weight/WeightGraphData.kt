package com.example.petcare.ui.screens.weight

import com.example.petcare.data.model.WeightLog
import com.example.petcare.util.EventDateUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class WeightGraphRange(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL_TIME("All")
}

data class WeightGraphWindow(
    val start: LocalDate,
    val end: LocalDate,
    val axisLabels: List<String>
)

fun graphWindowFor(
    range: WeightGraphRange,
    today: LocalDate = LocalDate.now(),
    logs: List<WeightLog> = emptyList()
): WeightGraphWindow {
    return when (range) {
        WeightGraphRange.WEEK -> {
            val start = today.minusDays(6)
            WeightGraphWindow(
                start = start,
                end = today,
                axisLabels = (0..6).map { start.plusDays(it.toLong()).format(DateTimeFormatter.ofPattern("EEE", Locale.US)) }
            )
        }
        WeightGraphRange.MONTH -> {
            val start = today.minusMonths(1).plusDays(1)
            val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
            WeightGraphWindow(
                start = start,
                end = today,
                axisLabels = (0..4).map { index ->
                    start.plusDays(((today.toEpochDay() - start.toEpochDay()) * index / 4).coerceAtLeast(0)).format(formatter)
                }
            )
        }
        WeightGraphRange.YEAR -> {
            val start = today.minusYears(1).plusDays(1)
            val formatter = DateTimeFormatter.ofPattern("MMM", Locale.US)
            val months = generateSequence(YearMonth.from(start)) { it.plusMonths(1) }
                .takeWhile { !it.atDay(1).isAfter(today) }
                .filterIndexed { index, _ -> index % 3 == 0 }
                .map { it.atDay(1).format(formatter) }
                .toList()
            WeightGraphWindow(start = start, end = today, axisLabels = months)
        }
        WeightGraphRange.ALL_TIME -> {
            val dates = logs.mapNotNull { parseWeightLogDate(it.loggedAt) }
            val start = dates.minOrNull() ?: today
            val end = dates.maxOrNull() ?: today
            WeightGraphWindow(
                start = start,
                end = end,
                axisLabels = allTimeAxisLabels(start, end)
            )
        }
    }
}

fun logsForGraph(
    logs: List<WeightLog>,
    range: WeightGraphRange,
    today: LocalDate = LocalDate.now()
): List<WeightLog> {
    val sortedLogs = logs
        .mapNotNull { log -> parseWeightLogDate(log.loggedAt)?.let { date -> log to date } }
        .sortedWith(compareBy<Pair<WeightLog, LocalDate>> { it.second }.thenBy { it.first.id })
    if (range == WeightGraphRange.ALL_TIME) {
        return sortedLogs.map { it.first }
    }

    val window = graphWindowFor(range, today, logs)
    return sortedLogs
        .filter { (_, date) -> !date.isBefore(window.start) && !date.isAfter(window.end) }
        .map { it.first }
}

fun latestWeightLog(logs: List<WeightLog>): WeightLog? {
    return logs
        .mapNotNull { log -> parseWeightLogDate(log.loggedAt)?.let { date -> log to date } }
        .maxWithOrNull(compareBy<Pair<WeightLog, LocalDate>> { it.second }.thenBy { it.first.loggedAt })
        ?.first
}

fun parseWeightLogDate(raw: String): LocalDate? {
    runCatching {
        if (raw.length >= 10 && raw.take(10).matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            return LocalDate.parse(raw.take(10))
        }
    }
    EventDateUtils.parseEventDate(raw)?.let { return it }
    val appDate = EventDateUtils.splitToAppDateTime(raw).first
    return runCatching {
        if (appDate.matches(Regex("""\d{2}/\d{2}/\d{4}"""))) {
            val parts = appDate.split("/")
            LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
        } else {
            null
        }
    }.getOrNull()
}

private fun allTimeAxisLabels(start: LocalDate, end: LocalDate): List<String> {
    if (start == end) {
        return listOf(start.format(DateTimeFormatter.ofPattern("MMM d", Locale.US)))
    }
    val days = (end.toEpochDay() - start.toEpochDay()).coerceAtLeast(1)
    val formatter = if (days > 370) {
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
    } else {
        DateTimeFormatter.ofPattern("MMM d", Locale.US)
    }
    return (0..4).map { index ->
        start.plusDays(days * index / 4).format(formatter)
    }.distinct()
}
