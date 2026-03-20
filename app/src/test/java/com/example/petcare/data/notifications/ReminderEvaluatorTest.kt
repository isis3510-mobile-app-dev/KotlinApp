package com.example.petcare.data.notifications

import com.example.petcare.data.model.Event
import com.example.petcare.data.model.EventType
import com.example.petcare.data.model.Pet
import com.example.petcare.data.model.SuggestionDto
import com.example.petcare.data.model.VaccineUrgencyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ReminderEvaluatorTest {

    private val evaluator = ReminderEvaluator()
    private val now = Instant.parse("2026-03-20T10:00:00Z")

    @Test
    fun `event within 12 hours creates H12 reminder`() {
        val pet = makePet(id = "pet-1", name = "Luna")
        val event = makeEvent(id = "evt-1", petId = pet.id, date = now.plus(10, ChronoUnit.HOURS).toString())

        val results = evaluator.evaluate(
            pets = listOf(pet),
            eventsByPetId = mapOf(pet.id to listOf(event)),
            suggestionsByPetId = emptyMap(),
            urgencyLevel = VaccineUrgencyLevel.DANGER_ONLY,
            sentKeys = emptySet(),
            now = now
        )

        assertEquals(1, results.size)
        assertEquals("EVENT_H12", results.first().backendType)
    }

    @Test
    fun `event within 2 hours creates H2 reminder`() {
        val pet = makePet(id = "pet-1", name = "Luna")
        val event = makeEvent(id = "evt-2", petId = pet.id, date = now.plus(2, ChronoUnit.HOURS).toString())

        val results = evaluator.evaluate(
            pets = listOf(pet),
            eventsByPetId = mapOf(pet.id to listOf(event)),
            suggestionsByPetId = emptyMap(),
            urgencyLevel = VaccineUrgencyLevel.DANGER_ONLY,
            sentKeys = emptySet(),
            now = now
        )

        assertEquals(1, results.size)
        assertEquals("EVENT_H2", results.first().backendType)
    }

    @Test
    fun `event within one day creates DAY reminder`() {
        val pet = makePet(id = "pet-1", name = "Luna")
        val event = makeEvent(id = "evt-1", petId = pet.id, date = now.plus(20, ChronoUnit.HOURS).toString())

        val results = evaluator.evaluate(
            pets = listOf(pet),
            eventsByPetId = mapOf(pet.id to listOf(event)),
            suggestionsByPetId = emptyMap(),
            urgencyLevel = VaccineUrgencyLevel.DANGER_ONLY,
            sentKeys = emptySet(),
            now = now
        )

        assertEquals("EVENT_DAY", results.first().backendType)
    }

    @Test
    fun `event within one week creates WEEK reminder`() {
        val pet = makePet(id = "pet-1", name = "Luna")
        val event = makeEvent(id = "evt-1", petId = pet.id, date = now.plus(6, ChronoUnit.DAYS).toString())

        val results = evaluator.evaluate(
            pets = listOf(pet),
            eventsByPetId = mapOf(pet.id to listOf(event)),
            suggestionsByPetId = emptyMap(),
            urgencyLevel = VaccineUrgencyLevel.DANGER_ONLY,
            sentKeys = emptySet(),
            now = now
        )

        assertEquals("EVENT_WEEK", results.first().backendType)
    }

    @Test
    fun `sent keys prevent duplicate reminder emission`() {
        val pet = makePet(id = "pet-1", name = "Luna")
        val event = makeEvent(id = "evt-1", petId = pet.id, date = now.plus(10, ChronoUnit.HOURS).toString())
        val sent = setOf("event:evt-1:window:H12")

        val results = evaluator.evaluate(
            pets = listOf(pet),
            eventsByPetId = mapOf(pet.id to listOf(event)),
            suggestionsByPetId = emptyMap(),
            urgencyLevel = VaccineUrgencyLevel.DANGER_ONLY,
            sentKeys = sent,
            now = now
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun `danger and warning urgency level includes warning smart suggestions`() {
        val pet = makePet(id = "pet-1", name = "Luna")

        val results = evaluator.evaluate(
            pets = listOf(pet),
            eventsByPetId = emptyMap(),
            suggestionsByPetId = mapOf(
                pet.id to listOf(
                    SuggestionDto(type = "warning", title = "Missing vaccine: Rabies", message = "m")
                )
            ),
            urgencyLevel = VaccineUrgencyLevel.DANGER_AND_WARNING,
            sentKeys = emptySet(),
            now = now
        )

        assertEquals(1, results.size)
        assertEquals("VACCINE_URGENT", results.first().backendType)
        assertEquals("petProfile/pet-1", results.first().targetRoute)
    }

    @Test
    fun `stale vet visit older than 60 days creates reminder`() {
        val pet = makePet(id = "pet-1", name = "Luna")
        val eventDate = now.minus(70, ChronoUnit.DAYS).toString()
        val event = makeEvent(
            id = "evt-vet-old",
            petId = pet.id,
            date = eventDate,
            eventType = EventType.CHECKUP
        )

        val results = evaluator.evaluate(
            pets = listOf(pet),
            eventsByPetId = mapOf(pet.id to listOf(event)),
            suggestionsByPetId = emptyMap(),
            urgencyLevel = VaccineUrgencyLevel.DANGER_ONLY,
            sentKeys = emptySet(),
            now = now
        )

        assertEquals(1, results.size)
        assertEquals("VET_VISIT_STALE", results.first().backendType)
        assertEquals("petProfile/pet-1", results.first().targetRoute)
    }

    @Test
    fun `recent vet visit does not create stale reminder`() {
        val pet = makePet(id = "pet-1", name = "Luna")
        val event = makeEvent(
            id = "evt-vet-recent",
            petId = pet.id,
            date = now.minus(20, ChronoUnit.DAYS).toString(),
            eventType = EventType.CHECKUP
        )

        val results = evaluator.evaluate(
            pets = listOf(pet),
            eventsByPetId = mapOf(pet.id to listOf(event)),
            suggestionsByPetId = emptyMap(),
            urgencyLevel = VaccineUrgencyLevel.DANGER_ONLY,
            sentKeys = emptySet(),
            now = now
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun `vet visit exactly 60 days ago does not create stale reminder`() {
        val pet = makePet(id = "pet-1", name = "Luna")
        val event = makeEvent(
            id = "evt-vet-60",
            petId = pet.id,
            date = now.minus(60, ChronoUnit.DAYS).toString(),
            eventType = EventType.CHECKUP
        )

        val results = evaluator.evaluate(
            pets = listOf(pet),
            eventsByPetId = mapOf(pet.id to listOf(event)),
            suggestionsByPetId = emptyMap(),
            urgencyLevel = VaccineUrgencyLevel.DANGER_ONLY,
            sentKeys = emptySet(),
            now = now
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun `sent key prevents duplicate stale vet reminder`() {
        val pet = makePet(id = "pet-1", name = "Luna")
        val oldVisitInstant = now.minus(90, ChronoUnit.DAYS)
        val event = makeEvent(
            id = "evt-vet",
            petId = pet.id,
            date = oldVisitInstant.toString(),
            eventType = EventType.CHECKUP
        )
        val sent = setOf("vetVisit:${pet.id}:last:${event.id}:${oldVisitInstant.epochSecond}")

        val results = evaluator.evaluate(
            pets = listOf(pet),
            eventsByPetId = mapOf(pet.id to listOf(event)),
            suggestionsByPetId = emptyMap(),
            urgencyLevel = VaccineUrgencyLevel.DANGER_ONLY,
            sentKeys = sent,
            now = now
        )

        assertTrue(results.isEmpty())
    }

    private fun makePet(id: String, name: String): Pet = Pet(
        id = id,
        name = name,
        species = "dog"
    )

    private fun makeEvent(
        id: String,
        petId: String,
        date: String,
        eventType: EventType = EventType.CHECKUP,
        title: String = "Vet Visit"
    ): Event = Event(
        id = id,
        petId = petId,
        ownerId = "owner-1",
        title = title,
        eventType = eventType,
        date = date
    )
}
