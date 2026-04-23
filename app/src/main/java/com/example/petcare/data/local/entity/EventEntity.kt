package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events_local")
data class EventEntity(
    @PrimaryKey
    val id: String,              // mismo tipo que Event.id (ObjectId string)
    val petId: String,
    val ownerId: String,
    val title: String,
    val eventType: String,       // guardas el name() del enum: "CHECKUP", "DENTAL"...
    val date: String,            // ISO string igual que el modelo de red
    val price: Double? = null,
    val provider: String = "",
    val clinic: String = "",
    val description: String = "",
    val followUpDate: String? = null,
    val synced: Boolean = false,
    val pendingDelete: Boolean = false  // para borrados offline
)