package com.example.petcare.data.repository

data class NfcPetPayload(
    val petId: String,
    val petName: String,
    val species: String,
    val breed: String,
    val ownerName: String,
    val ownerPhone: String,
    val ownerInitials: String = "",
    val photoUrl: String = "",
    val status: String = "Unknown",
    val appDeepLink: String = "",
    val knownAllergies: String = "",
    val defaultVet: String = "",
    val defaultClinic: String = ""
)