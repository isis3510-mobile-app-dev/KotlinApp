package com.example.petcare.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

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
    val defaultClinic: String
)


