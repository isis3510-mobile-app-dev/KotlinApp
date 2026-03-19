package com.example.petcare.data.model

data class User(
    val id: String,
    val firebase_uid: String,
    val name: String,
    val email: String,
    val phone: String?,
    val address: String?,
    val photo_url: String?,
    val initials: String,
    val pets: List<String> = emptyList(),
    val family_group: List<String> = emptyList(),
    val created_at: String,
    val updated_at: String
)

data class UpdateUserRequest(
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val photo_url: String? = null,
    val initials: String? = null,
    val email: String? = null
)
