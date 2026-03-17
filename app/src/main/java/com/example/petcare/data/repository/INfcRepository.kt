package com.example.petcare.data.repository


interface INfcRepository {
    suspend fun fetchWritePayload(petId: String, firebaseToken: String): Result<NfcPetPayload>
    suspend fun markNfcSynced(petId: String, firebaseToken: String): Result<Unit>
    suspend fun fetchPublicPetInfo(petId: String): Result<NfcPetPayload>
}