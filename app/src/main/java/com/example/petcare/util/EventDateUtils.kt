package com.example.petcare.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

object EventDateUtils {

    private val appDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val appTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    fun parseEventDate(raw: String?): LocalDate? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null

        parseEventInstant(value)?.let {
            return it.atZone(ZoneId.systemDefault()).toLocalDate()
        }
        return parseAsDateOnly(value)
    }

    fun parseEventInstant(raw: String?): Instant? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null

        runCatching { return Instant.parse(value) }
        runCatching { return OffsetDateTime.parse(value).toInstant() }
        runCatching { return ZonedDateTime.parse(value).toInstant() }
        runCatching {
            val local = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            return local.atZone(ZoneId.systemDefault()).toInstant()
        }
        return null
    }

    fun isTodayOrFuture(raw: String?, today: LocalDate = LocalDate.now()): Boolean {
        val localDate = parseEventDate(raw) ?: return false
        return !localDate.isBefore(today)
    }

    fun splitToAppDateTime(raw: String?): Pair<String, String> {
        val instant = parseEventInstant(raw)
        if (instant != null) {
            val localDateTime = instant.atZone(ZoneId.systemDefault())
            val date = localDateTime.toLocalDate().format(appDateFormatter)
            val time = localDateTime.toLocalTime()
                .withSecond(0)
                .withNano(0)
                .format(appTimeFormatter)
            return date to time
        }

        val dateOnly = parseAsDateOnly(raw?.trim().orEmpty())
        if (dateOnly != null) {
            return dateOnly.format(appDateFormatter) to "12:00 AM"
        }
        return "" to ""
    }

    fun toIsoFromAppDateTime(
        appDate: String,
        appTime: String,
        fallbackRaw: String? = null
    ): String? {
        val dateValue = appDate.trim()
        if (dateValue.isBlank()) {
            return normalizeRawToIso(fallbackRaw)
        }

        val parsedDate = runCatching {
            LocalDate.parse(dateValue, appDateFormatter)
        }.getOrNull() ?: return null

        val parsedTime = parseAppTimeOrDefault(appTime)
        val localDateTime = LocalDateTime.of(parsedDate, parsedTime)
        return localDateTime
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    fun normalizeRawToIso(raw: String?): String? {
        val instant = parseEventInstant(raw)
        if (instant != null) {
            return instant.atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

        val dateOnly = parseAsDateOnly(raw?.trim().orEmpty()) ?: return null
        return dateOnly.atStartOfDay()
            .atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun parseAsDateOnly(value: String): LocalDate? {
        if (value.isBlank()) return null
        runCatching { return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) }
        if (value.length >= 10) {
            runCatching { return LocalDate.parse(value.take(10), DateTimeFormatter.ISO_LOCAL_DATE) }
        }
        runCatching { return LocalDate.parse(value, appDateFormatter) }
        return null
    }

    private fun parseAppTimeOrDefault(value: String): LocalTime {
        val raw = value.trim()
        if (raw.isBlank()) return LocalTime.MIDNIGHT
        return runCatching {
            LocalTime.parse(raw.uppercase(Locale.US), appTimeFormatter)
        }.getOrElse { LocalTime.MIDNIGHT }
    }
}
