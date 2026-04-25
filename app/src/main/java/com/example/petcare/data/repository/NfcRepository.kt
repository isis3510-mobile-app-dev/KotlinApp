package com.example.petcare.data.repository

import com.example.petcare.data.network.ApiService


class NfcRepository(private val api: ApiService) : INfcRepository {

    override suspend fun fetchWritePayload(
        petId: String,
        firebaseToken: String
    ): Result<NfcPetPayload> = runCatching {
        val response = api.getNfcPayload(petId)
        if (!response.isSuccessful) error("NFC payload failed: ${response.code()}")
        val body = response.body() ?: error("Empty response (${response.code()})")
        NfcPetPayload(
            petId      = body.petId,
            petName    = body.petName,
            species    = body.species,
            breed      = body.breed,
            ownerName  = body.ownerName,
            ownerPhone = body.ownerPhone,
            appDeepLink = "petcare://pet/${body.petId}"
        )
    }

    override suspend fun markNfcSynced(
        petId: String,
        firebaseToken: String
    ): Result<Unit> = runCatching {
        val response = api.syncNfc(petId)
        if (!response.isSuccessful) error("Sync failed: ${response.code()}")
    }

    override suspend fun fetchPublicPetInfo(
        petId: String
    ): Result<NfcPetPayload> = runCatching {
        val response = api.nfcPublicRead(petId)
        if (!response.isSuccessful) error("Public NFC read failed: ${response.code()}")
        val body = response.body() ?: error("Empty response (${response.code()})")
        NfcPetPayload(
            petId           = body.petId,
            petName         = body.petName,
            species         = body.species,
            breed           = body.breed,
            ownerName       = body.ownerName,
            ownerPhone      = body.ownerPhone,
            ownerInitials   = body.ownerInitials,
            photoUrl        = body.photoUrl,
            status          = body.status,
            knownAllergies  = body.knownAllergies,
            defaultVet      = body.defaultVet,
            defaultClinic   = body.defaultClinic
        )
    }
}
