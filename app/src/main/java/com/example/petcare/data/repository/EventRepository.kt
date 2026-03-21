package com.example.petcare.data.repository

import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.Event
import com.example.petcare.data.network.ApiService
import org.json.JSONObject

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
        if (!response.isSuccessful) {
            error(parseApiError(response.errorBody()?.string(), response.code(), "create event"))
        }
        response.body() ?: error("Failed to create event — empty response")
    }

    suspend fun updateEvent(
        eventId: String,
        title: String,
        description: String,
        provider: String,
        clinic: String,
        price: Double?,
        date: String
    ): Result<Event> = runCatching {
        val body = buildMap<String, Any?> {
            put("title",       title)
            put("description", description)
            put("provider",    provider)
            put("clinic",      clinic)
            put("date",        date)
            if (price != null) put("price", price)
        }
        val response = api.updateEvent(eventId, body)
        if (!response.isSuccessful) {
            error(parseApiError(response.errorBody()?.string(), response.code(), "update event"))
        }
        response.body() ?: error("Failed to update event — empty response")
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        // Retrofit throws "HTTP 204 had non-zero Content-Length" when the backend
        // returns a 204 with a body (e.g. JsonResponse({}, status=204) sends 2 bytes).
        // We catch that specific exception and treat it as success since the deletion
        // did complete on the server (204 = No Content = success).
        return try {
            val response = api.deleteEvent(eventId)
            if (response.isSuccessful || response.code() == 204) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete event — HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            val message = e.message ?: ""
            // "HTTP 204 had non-zero Content-Length" means the delete succeeded but
            // the server incorrectly included a body. Treat as success.
            if (message.contains("204") && message.contains("Content-Length")) {
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        }
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
        response.body() ?: error("Failed to add document — empty response")
    }

    private fun parseApiError(errorBody: String?, code: Int, action: String): String {
        if (errorBody.isNullOrBlank()) {
            return "Failed to $action — HTTP $code"
        }

        return runCatching {
            val json = JSONObject(errorBody)
            json.optString("error")
                .ifBlank { json.optString("message") }
                .ifBlank { errorBody }
        }.getOrDefault(errorBody).let { message ->
            "Failed to $action — HTTP $code: $message"
        }
    }
}