package com.example.petcare.data.local.mapper

import com.example.petcare.data.local.entity.EventEntity
import com.example.petcare.data.model.Event
import com.example.petcare.data.model.EventType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

fun Event.toEntity() = EventEntity(
    id = id,
    petId = petId,
    ownerId = ownerId,
    title = title,
    eventType = eventType?.name ?: "OTHER",
    date = date,
    price = price,
    provider = provider,
    clinic = clinic,
    description = description,
    followUpDate = followUpDate,
    attachedDocumentsJson = Gson().toJson(attachedDocuments),
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
    attachedDocuments = runCatching {
        val type = object : TypeToken<List<com.example.petcare.data.model.AttachedDocument>>() {}.type
        Gson().fromJson<List<com.example.petcare.data.model.AttachedDocument>>(attachedDocumentsJson, type).orEmpty()
    }.getOrElse { emptyList() }
)
