package com.example.petcare.data.repository

import com.example.petcare.data.model.AddDocumentRequest
import com.example.petcare.data.model.AddVaccinationRequest
import com.example.petcare.data.model.CreatePetRequest
import com.example.petcare.data.model.Pet
import com.example.petcare.data.model.UpdatePetRequest
import com.example.petcare.data.model.UpdateVaccinationRequest
import com.example.petcare.data.network.ApiService

class PetRepository(private val api: ApiService) {

    suspend fun getPets(): Result<List<Pet>> = runCatching {
        val response = api.getPets()
        response.body() ?: error("Empty response")
    }

    suspend fun getPet(petId: String): Result<Pet> = runCatching {
        val response = api.getPet(petId)
        response.body() ?: error("Pet not found")
    }

    suspend fun createPet(request: CreatePetRequest): Result<Pet> = runCatching {
        val response = api.createPet(request)
        response.body() ?: error("Failed to create pet")
    }

    suspend fun updatePet(petId: String, request: UpdatePetRequest): Result<Pet> = runCatching {
        val response = api.updatePet(petId, request)
        response.body() ?: error("Failed to update pet")
    }

    suspend fun deletePet(petId: String): Result<Unit> = runCatching {
        api.deletePet(petId)
    }

    suspend fun addVaccination(petId: String, request: AddVaccinationRequest): Result<Pet> = runCatching {
        val response = api.addVaccination(petId, request)
        response.body() ?: error("Failed to add vaccination")
    }

    suspend fun addVaccinationDocument(
        petId: String,
        vaccinationId: String,
        request: AddDocumentRequest
    ): Result<Pet> = runCatching {
        val response = api.addVaccinationDocument(petId, vaccinationId, request)
        response.body() ?: error("Failed to add document")
    }

    suspend fun deleteVaccination(
        petId: String,
        vaccinationId: String
    ): Result<Pet> = runCatching {
        val response = api.deleteVaccination(petId, vaccinationId)
        response.body() ?: error("Failed to delete vaccination — HTTP ${response.code()}")
    }

    suspend fun updateVaccination(
        petId: String,
        vaccinationId: String,
        administeredBy: String,
        nextDueDate: String?,
        lotNumber: String
    ): Result<Pet> = runCatching {
        val body = buildMap<String, Any?> {
            put("administeredBy", administeredBy)
            put("lotNumber", lotNumber)
            // Convert dd/MM/yyyy → yyyy-MM-ddT00:00:00Z before sending to backend.
            // The DateTextField returns dd/MM/yyyy; stored dates from the API are
            // already in ISO format (yyyy-MM-dd...) — handle both.
            if (nextDueDate != null) {
                put("nextDueDate", toIso(nextDueDate))
            }
        }
        val response = api.updateVaccination(petId, vaccinationId, body)
        response.body() ?: error("Failed to update vaccination — HTTP ${response.code()}")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Converts a date string to ISO-8601 format expected by the backend.
     *
     * Handles two input formats:
     *  - dd/MM/yyyy  (from DateTextField picker)  → yyyy-MM-ddT00:00:00Z
     *  - yyyy-MM-dd  (already ISO, from API)       → yyyy-MM-ddT00:00:00Z
     */
    private fun toIso(date: String): String {
        if (date.isBlank()) return date
        return try {
            when {
                // dd/MM/yyyy
                date.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> {
                    val parts = date.split("/")
                    "${parts[2]}-${parts[1]}-${parts[0]}T00:00:00Z"
                }
                // yyyy-MM-dd (with or without time suffix)
                date.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) -> {
                    "${date.take(10)}T00:00:00Z"
                }
                else -> date
            }
        } catch (_: Exception) {
            date
        }
    }
}