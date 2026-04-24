package com.example.petcare.data.model

import com.google.gson.annotations.SerializedName

data class Vaccination(
    @SerializedName("id")          val id: String = "",          // ← ADD THIS — the _id from backend
    @SerializedName("vaccineId")   val vaccineId: String,
    @SerializedName("dateGiven")   val dateGiven: String,
    @SerializedName("nextDueDate") val nextDueDate: String? = null,
    @SerializedName("lotNumber")   val lotNumber: String = "",
    val status: String = "completed",
    @SerializedName("administeredBy") val administeredBy: String = "",
    @SerializedName("attachedDocuments") val attachedDocuments: List<AttachedDocument> = emptyList()
)

data class AddVaccinationRequest(
    @SerializedName("vaccineId")      val vaccineId: String,
    @SerializedName("dateGiven")      val dateGiven: String,
    @SerializedName("nextDueDate")    val nextDueDate: String? = null,
    @SerializedName("lotNumber")      val lotNumber: String = "",
    val status: String = "completed",
    @SerializedName("administeredBy") val administeredBy: String = ""
)

data class UpdateVaccinationRequest(
    @SerializedName("vaccineId")      val vaccineId: String,
    @SerializedName("dateGiven")      val dateGiven: String,
    @SerializedName("nextDueDate")    val nextDueDate: String? = null,
    @SerializedName("lotNumber")      val lotNumber: String = "",
    @SerializedName("administeredBy") val administeredBy: String = ""
)
