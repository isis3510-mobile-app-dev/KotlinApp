package com.example.petcare.data.model

enum class EventType { CHECKUP, DENTAL, SURGERY, VACCINE, OTHER }

data class MedicalEvent(
    val id: String,
    val petId: String,
    val title: String,
    val eventType: EventType,
    val price: Double? = null,
    val provider: String,
    val clinic: String,
    val date: String,
    val description: String,
    val followUpDate: String? = null,
    val attachedDocuments: List<AttachedDocument> = emptyList()
)
