package com.example.petcare.data.notifications

import com.example.petcare.data.model.Event
import com.example.petcare.data.model.Pet
import com.example.petcare.data.model.ReminderWindow
import com.example.petcare.data.model.SuggestionDto
import com.example.petcare.data.model.VaccineUrgencyLevel
import com.example.petcare.util.EventDateUtils
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

private const val WEEK_HOURS = 24L * 7L
private const val DAY_HOURS = 24L
private const val H2_HOURS = 2L
private const val H12_HOURS = 12L

data class ReminderCandidate(
    val dedupeKey: String,
    val backendType: String,
    val header: String,
    val text: String,
    val targetRoute: String
)

class ReminderEvaluator {

    fun evaluate(
        pets: List<Pet>,
        eventsByPetId: Map<String, List<Event>>,
        suggestionsByPetId: Map<String, List<SuggestionDto>>,
        urgencyLevel: VaccineUrgencyLevel,
        sentKeys: Set<String>,
        now: Instant = Instant.now()
    ): List<ReminderCandidate> {
        val petsById = pets.associateBy { it.id }
        val candidates = mutableListOf<ReminderCandidate>()

        eventsByPetId.forEach { (petId, events) ->
            val petName = petsById[petId]?.name ?: "Your pet"
            events.forEach { event ->
                val window = resolveCurrentWindow(event.date, now) ?: return@forEach
                val key = "event:${event.id}:window:${window.name}"
                if (key in sentKeys) return@forEach

                val (header, type) = when (window) {
                    ReminderWindow.WEEK -> "Upcoming event in 1 week" to "EVENT_WEEK"
                    ReminderWindow.DAY -> "Upcoming event in 1 day" to "EVENT_DAY"
                    ReminderWindow.H2 -> "Upcoming event in 2 hours" to "EVENT_H2"
                    ReminderWindow.H12 -> "Upcoming event in 12 hours" to "EVENT_H12"
                }

                candidates += ReminderCandidate(
                    dedupeKey = key,
                    backendType = type,
                    header = header,
                    text = "${event.title} for $petName is coming up soon.",
                    targetRoute = "eventDetails/${event.petId}/${event.id}"
                )
            }
        }

        suggestionsByPetId.forEach { (petId, suggestions) ->
            val petName = petsById[petId]?.name ?: "Your pet"
            suggestions
                .filter { isUrgentSuggestion(it, urgencyLevel) }
                .forEach { suggestion ->
                    val digest = shortHash("${suggestion.title}|${suggestion.message}")
                    val key = "vaccine:$petId:urgency:${urgencyLevel.name}:$digest"
                    if (key in sentKeys) return@forEach

                    candidates += ReminderCandidate(
                        dedupeKey = key,
                        backendType = "VACCINE_URGENT",
                        header = "Urgent vaccine recommendation",
                        text = "$petName: ${suggestion.title}",
                        targetRoute = "petProfile/$petId"
                    )
                }
        }

        return candidates
    }

    private fun resolveCurrentWindow(rawDate: String?, now: Instant): ReminderWindow? {
        val eventInstant = EventDateUtils.parseEventInstant(rawDate) ?: return null
        if (!eventInstant.isAfter(now)) return null

        val hours = Duration.between(now, eventInstant).toHours()
        return when {
            hours <= H2_HOURS -> ReminderWindow.H2
            hours <= H12_HOURS -> ReminderWindow.H12
            hours <= DAY_HOURS -> ReminderWindow.DAY
            hours <= WEEK_HOURS -> ReminderWindow.WEEK
            else -> null
        }
    }

    private fun isUrgentSuggestion(
        suggestion: SuggestionDto,
        urgencyLevel: VaccineUrgencyLevel
    ): Boolean = when (urgencyLevel) {
        VaccineUrgencyLevel.DANGER_ONLY -> suggestion.type.equals("danger", ignoreCase = true)
        VaccineUrgencyLevel.DANGER_AND_WARNING -> {
            suggestion.type.equals("danger", ignoreCase = true) ||
                suggestion.type.equals("warning", ignoreCase = true)
        }
        VaccineUrgencyLevel.MISSING_ONLY -> suggestion.title.startsWith("Missing vaccine", ignoreCase = true)
    }

    private fun shortHash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString(separator = "") { "%02x".format(it) }.take(12)
    }
}
