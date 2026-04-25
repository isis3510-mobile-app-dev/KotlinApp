package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE  // si borras el evento, se borra el reminder
    )],
    indices = [Index("eventId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String,        // FK a EventEntity.id
    val triggerMs: Long,        // epoch ms de cuándo disparar la notificación
    val windowType: String,     // "WEEK", "DAY", "H2", "H12" — nombre del enum
    val fired: Boolean = false  // ya se disparó o no
)