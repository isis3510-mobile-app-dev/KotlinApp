package com.example.petcare.data.model

data class PendingEventDocument(
    val id: String,
    val petId: String,
    val eventId: String,
    val fileName: String,
    val mimeType: String,
    val localUri: String,
    val createdAt: Long = System.currentTimeMillis()
)
