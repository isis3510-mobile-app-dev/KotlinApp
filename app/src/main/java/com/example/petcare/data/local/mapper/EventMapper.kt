package com.example.petcare.data.local.mapper

import com.example.petcare.data.local.entity.EventEntity
import com.example.petcare.data.model.Event
import com.example.petcare.data.model.EventType

fun Event.toEntity() = EventEntity(
    id = id,
    petId = petId,
    ownerId = ownerId,
    title = title,
    eventType = eventType.name,
    date = date,
    price = price,
    provider = provider,
    clinic = clinic,
    description = description,
    followUpDate = followUpDate,
    synced = true,
    pendingDelete = false,
    pendingOperation = null,
    retryCount = 0,
    nextRetryAt = 0L
)

fun EventEntity.toEvent() = Event(
    id = id,
    petId = petId,
    ownerId = ownerId,
    title = title,
    eventType = runCatching {
        EventType.valueOf(eventType.uppercase())
    }.getOrDefault(EventType.OTHER),
    date = date,
    price = price,
    provider = provider,
    clinic = clinic,
    description = description,
    followUpDate = followUpDate,
    attachedDocuments = emptyList()
)
