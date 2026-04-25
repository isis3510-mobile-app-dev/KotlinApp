package com.example.petcare.data.model

data class PendingVaccinationDocument(
    val id: String,
    val petId: String,
    val vaccinationId: String,
    val fileName: String,
    val mimeType: String,
    val localUri: String,
    val createdAt: Long = System.currentTimeMillis()
)
