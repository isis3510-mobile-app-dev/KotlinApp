package com.example.petcare.data.model

import com.google.gson.annotations.SerializedName

data class NfcPayloadResponse(
    @SerializedName("petId") val petId: String,
    @SerializedName("petName") val petName: String,
    val species: String,
    val breed: String,
    @SerializedName("ownerName") val ownerName: String,
    @SerializedName("ownerPhone") val ownerPhone: String
)

data class NfcPublicReadResponse(
    @SerializedName("petId") val petId: String,
    @SerializedName("petName") val petName: String,
    val species: String,
    val breed: String,
    val status: String = "Unknown",
    @SerializedName("photoUrl") val photoUrl: String = "",
    @SerializedName("ownerName") val ownerName: String,
    @SerializedName("ownerPhone") val ownerPhone: String,
    @SerializedName("ownerInitials") val ownerInitials: String = "",
    @SerializedName("knownAllergies") val knownAllergies: String = "",
    @SerializedName("defaultVet") val defaultVet: String = "",
    @SerializedName("defaultClinic") val defaultClinic: String = ""
)

data class NfcSyncResponse(
    val success: Boolean,
    @SerializedName("petId") val petId: String,
    @SerializedName("isNfcSynced") val isNfcSynced: Boolean
)