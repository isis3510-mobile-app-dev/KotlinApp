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

    suspend fun updateEvent(
        eventId: String,
        title: String,
        description: String,
        provider: String,
        clinic: String,
        price: Double?
    ): Result<Event> = runCatching {
        val body = buildMap<String, Any?> {
            put("title",       title)
            put("description", description)
            put("provider",    provider)
            put("clinic",      clinic)
            if (price != null) put("price", price)
        }
        val response = api.updateEvent(eventId, body)
        response.body() ?: error("Failed to update event — HTTP ${response.code()}")
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        api.deleteEvent(eventId)
    }

    suspend fun addDocument(
        eventId: String,
        fileName: String,
        fileUri: String?
    ): Result<Event> = runCatching {
        val body = buildMap<String, Any?> {
            put("fileName", fileName)
            if (fileUri != null) put("fileUri", fileUri)
        }
        val response = api.addEventDocument(eventId, body)
        response.body() ?: error("Failed to add document — HTTP ${response.code()}")
    }
}