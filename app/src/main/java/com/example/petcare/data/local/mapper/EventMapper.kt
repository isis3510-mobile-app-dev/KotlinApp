package com.example.petcare.data.local.mapper

import com.example.petcare.data.local.entity.EventEntity
import com.example.petcare.data.model.Event

fun Event.toEntity() = EventEntity(
    id           = id,
    petId        = petId,
    ownerId      = ownerId,
    title        = title,
    eventType    = eventType.name,
    date         = date,
    price        = price,
    provider     = provider,
    clinic       = clinic,
    description  = description,
    followUpDate = followUpDate,
    synced       = true  // vino del servidor → ya sincronizado
)