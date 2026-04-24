package com.example.petcare.data.local.mapper

import com.example.petcare.data.local.entity.WeightLogEntity
import com.example.petcare.data.model.WeightLog

fun WeightLog.toEntity(): WeightLogEntity = WeightLogEntity(
    id = id,
    petId = petId,
    ownerId = ownerId,
    weight = weight,
    loggedAt = loggedAt,
    clientMutationId = clientMutationId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pendingSync = false,
    pendingDelete = false
)

fun WeightLogEntity.toWeightLog(): WeightLog = WeightLog(
    id = id,
    petId = petId,
    ownerId = ownerId,
    weight = weight,
    loggedAt = loggedAt,
    clientMutationId = clientMutationId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
