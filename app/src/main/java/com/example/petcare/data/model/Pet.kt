package com.example.petcare.data.model

import com.google.gson.annotations.SerializedName

data class Pet(
    val id: String,
    val schema: Int = 1,
    val owners: List<String> = emptyList(),
    val name: String,
    val species: String,
    val breed: String = "",
    val gender: String = "",
    @SerializedName("birthDate") val birthDate: String? = null,
    val weight: Double? = null,
    val color: String = "",
    @SerializedName("photoUrl") val photoUrl: String? = null,
    val status: String = "healthy",
    @SerializedName("isNfcSynced") val isNfcSynced: Boolean = false,
    @SerializedName("knownAllergies") val knownAllergies: String = "",
    @SerializedName("defaultVet") val defaultVet: String = "",
    @SerializedName("defaultClinic") val defaultClinic: String = "",
    val vaccinations: List<Vaccination> = emptyList()
)

data class CreatePetRequest(
    val name: String,
    val species: String,
    val breed: String = "",
    val gender: String = "",
    @SerializedName("birthDate") val birthDate: String? = null,
    val weight: Double? = null,
    val color: String = "",
    @SerializedName("photoUrl") val photoUrl: String? = null,
    @SerializedName("knownAllergies") val knownAllergies: String = "",
    @SerializedName("defaultVet") val defaultVet: String = "",
    @SerializedName("defaultClinic") val defaultClinic: String = ""
)

data class UpdatePetRequest(
    val name: String? = null,
    val species: String? = null,
    val breed: String? = null,
    val gender: String? = null,
    @SerializedName("birthDate") val birthDate: String? = null,
    val weight: Double? = null,
    val color: String? = null,
    @SerializedName("photoUrl") val photoUrl: String? = null,
    val status: String? = null,
    @SerializedName("isNfcSynced") val isNfcSynced: Boolean? = null,
    @SerializedName("knownAllergies") val knownAllergies: String? = null,
    @SerializedName("defaultVet") val defaultVet: String? = null,
    @SerializedName("defaultClinic") val defaultClinic: String? = null
)
