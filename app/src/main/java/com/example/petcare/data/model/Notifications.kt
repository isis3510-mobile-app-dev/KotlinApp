package com.example.petcare.data.model

data class PetSuggestion(
    val petId: String,
    val petName: String,
    val suggestion: SuggestionDto
)


data class GroupedSuggestion(
    val vaccineTitle: String,
    val type: String,
    val pets: List<String>,
    val message: String
)

data class PetFilterChip(
    val petId: String,
    val petName: String,
    val alertCount: Int
)