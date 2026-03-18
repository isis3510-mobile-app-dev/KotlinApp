package com.example.petcare.data.model
import com.google.gson.annotations.SerializedName

data class SuggestionDto(
    val type: String,
    val title: String,
    val message: String
)

data class PetSmartResponse(
    @SerializedName("petId")   val petId: String,
    @SerializedName("petName") val petName: String,
    val suggestions: List<SuggestionDto>
)