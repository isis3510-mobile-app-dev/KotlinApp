package com.example.petcare.data.model

import com.google.gson.annotations.SerializedName

data class Vaccine(
    val id: String,
    val name: String,
    val species: List<String> = emptyList(),
    @SerializedName("productName")  val productName: String = "",
    val manufacturer: String = "",
    @SerializedName("intervalDays") val intervalDays: Int = 0,
    val description: String = ""
)