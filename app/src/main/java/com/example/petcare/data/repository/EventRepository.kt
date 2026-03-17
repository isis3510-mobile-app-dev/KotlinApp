package com.example.petcare.data.repository

import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.Event
import com.example.petcare.data.network.ApiService

class EventRepository(private val api: ApiService) {

    suspend fun getEvents(
        petId: String? = null,
        ownerId: String? = null
    ): Result<List<Event>> = runCatching {
        api.getEvents(petId = petId, ownerId = ownerId).body()
            ?: error("Empty response")
    }

    suspend fun getEvent(eventId: String): Result<Event> = runCatching {
        api.getEvent(eventId).body() ?: error("Event not found")
    }

    suspend fun createEvent(request: CreateEventRequest): Result<Event> = runCatching {
        val response = api.createEvent(request)
        response.body() ?: error("Failed to create event — HTTP ${response.code()}")
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        api.deleteEvent(eventId)
    }
}