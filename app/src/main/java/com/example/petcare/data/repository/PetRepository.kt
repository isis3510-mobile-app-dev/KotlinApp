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
        vaccinationId: String      // now just the ID, no dateGiven needed
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
            if (nextDueDate != null) put("nextDueDate", nextDueDate)
        }
        val response = api.updateVaccination(petId, vaccinationId, body)
        response.body() ?: error("Failed to update vaccination — HTTP ${response.code()}")
    }
}