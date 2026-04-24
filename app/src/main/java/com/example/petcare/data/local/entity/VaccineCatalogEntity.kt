package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaccine_catalog")
data class VaccineCatalogEntity(
    @PrimaryKey val id: String,
    val name: String,
    val speciesRaw: String = "",
    val productName: String = "",
    val manufacturer: String = "",
    val intervalDays: Int = 0,
    val description: String = ""
) {
    val species: List<String>
        get() = if (speciesRaw.isBlank()) emptyList()
        else speciesRaw.split(",").map { it.trim() }
}