package com.example.petcare.data.model

import com.google.gson.annotations.SerializedName

enum class EventType {
    @SerializedName("checkup") CHECKUP,
    @SerializedName("dental")  DENTAL,
    @SerializedName("surgery") SURGERY,
    @SerializedName("vaccine") VACCINE,
    @SerializedName("other")   OTHER
}

data class Event(
    val id: String,
    val schema: Int = 1,
    @SerializedName("petId")       val petId: String,
    @SerializedName("ownerId")     val ownerId: String,
    val title: String,
    @SerializedName("eventType")   val eventType: EventType,
    val date: String,
    val price: Double? = null,
    val provider: String = "",
    val clinic: String = "",
    val description: String = "",
    @SerializedName("followUpDate")      val followUpDate: String? = null,
    @SerializedName("attachedDocuments") val attachedDocuments: List<AttachedDocument> = emptyList()
)

data class CreateEventRequest(
    @SerializedName("petId")       val petId: String,
    @SerializedName("ownerId")     val ownerId: String,
    val title: String,
    @SerializedName("eventType")   val eventType: String,  // ← Send as String, not enum
    val date: String,
    val price: Double? = null,
    val provider: String = "",
    val clinic: String = "",
    val description: String = "",
    @SerializedName("followUpDate") val followUpDate: String? = null
)