package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_logs",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("petId"),
        Index("ownerId"),
        Index(value = ["clientMutationId"], unique = false)
    ]
)
data class WeightLogEntity(
    @PrimaryKey val id: String,
    val petId: String,
    val ownerId: String,
    val weight: Double,
    val loggedAt: String,
    val clientMutationId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val pendingSync: Boolean = false,
    val pendingDelete: Boolean = false
)
