package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val initials: String,
    val phone: String?,
    val address: String?,
    val photoUrl: String?,
    val lastUpdated: Long,
    val email: String?,
    val pendingSync: Boolean = false
)