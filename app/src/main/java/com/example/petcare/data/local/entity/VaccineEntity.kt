package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vaccinations",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("petId")]
)
data class VaccinationEntity(
    @PrimaryKey val id: String,
    val petId: String,
    val vaccineId: String,
    val dateGiven: String,
    val nextDueDate: String? = null,
    val lotNumber: String = "",
    val status: String = "completed",
    val administeredBy: String = "",
    val pendingSync: Boolean = false,
    val pendingDelete: Boolean = false
)