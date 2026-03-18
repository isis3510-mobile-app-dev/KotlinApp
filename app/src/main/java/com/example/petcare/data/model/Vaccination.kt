package com.example.petcare.data.model

import com.google.gson.annotations.SerializedName

data class Vaccination(
    @SerializedName("vaccineId") val vaccineId: String,
    @SerializedName("dateGiven") val dateGiven: String,
    @SerializedName("nextDueDate") val nextDueDate: String? = null,
    @SerializedName("lotNumber") val lotNumber: String = "",
    val status: String = "completed",
    @SerializedName("administeredBy") val administeredBy: String = "",
    @SerializedName("attachedDocuments") val attachedDocuments: List<AttachedDocument> = emptyList()
)

data class AddVaccinationRequest(
    @SerializedName("vaccineId") val vaccineId: String,
    @SerializedName("dateGiven") val dateGiven: String,
    @SerializedName("nextDueDate") val nextDueDate: String? = null,
    @SerializedName("lotNumber") val lotNumber: String = "",
    val status: String = "completed",
    @SerializedName("administeredBy") val administeredBy: String = ""
)