package com.example.petcare.data.model

import com.google.gson.annotations.SerializedName

data class WeightLog(
    val id: String,
    val schema: Int = 1,
    @SerializedName("petId") val petId: String,
    @SerializedName("ownerId") val ownerId: String,
    val weight: Double,
    @SerializedName("loggedAt") val loggedAt: String,
    @SerializedName("clientMutationId") val clientMutationId: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreateWeightLogRequest(
    val weight: Double,
    @SerializedName("loggedAt") val loggedAt: String,
    @SerializedName("clientMutationId") val clientMutationId: String? = null
)

data class UpdateWeightLogRequest(
    val weight: Double? = null,
    @SerializedName("loggedAt") val loggedAt: String? = null
)
