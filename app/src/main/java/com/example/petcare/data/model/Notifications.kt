package com.example.petcare.data.model

data class PetSuggestion(
    val petId: String,
    val petName: String,
    val petPhotoUrl: String?,
    val suggestion: SuggestionDto
)


data class GroupedSuggestion(
    val vaccineTitle: String,
    val type: String,
    val pets: List<String>,
    val message: String,
    val petPhotoUrls: List<String> = emptyList()
)

data class PetFilterChip(
    val petId: String,
    val petName: String,
    val alertCount: Int
)