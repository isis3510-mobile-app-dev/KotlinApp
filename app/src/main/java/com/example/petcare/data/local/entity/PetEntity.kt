package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val species: String,
    val breed: String,
    val gender: String,
    val weight: Double? = null,
    val color: String,
    val birthDate: String? = null,
    val photoUrl: String? = null,
    val status: String,
    val isNfcSynced: Boolean,
    val knownAllergies: String,
    val defaultVet: String,
    val defaultClinic: String,
    val pendingSync: Boolean = false,
    val pendingDelete: Boolean = false,
    val owner: String = ""
)


