package com.example.petcare.data.local.mapper

import com.example.petcare.data.local.entity.UserEntity
import com.example.petcare.data.model.User

fun User.toEntity() = UserEntity(
    id = id,
    name = name,
    initials = initials,
    phone = phone,
    address = address,
    photoUrl = photo_url,
    email = email,
    lastUpdated = System.currentTimeMillis()
)

fun UserEntity.toDomain(): User = User(
    id = id,
    firebase_uid = "",
    name = name,
    email = email ?: "",
    phone = phone,
    address = address,
    photo_url = photoUrl,
    initials = initials,
    pets = emptyList(),
    family_group = emptyList(),
    created_at = "",
    updated_at = ""
)