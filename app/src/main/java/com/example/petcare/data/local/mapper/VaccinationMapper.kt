package com.example.petcare.data.local.mapper

import com.example.petcare.data.local.entity.VaccinationEntity
import com.example.petcare.data.model.Vaccination

fun Vaccination.toEntity(petId: String): VaccinationEntity {
    return VaccinationEntity(
        id             = this.id,
        petId          = petId,
        vaccineId      = this.vaccineId,
        vaccineName    = this.vaccineName,
        dateGiven      = this.dateGiven,
        nextDueDate    = this.nextDueDate,
        lotNumber      = this.lotNumber,
        status         = this.status,
        administeredBy = this.administeredBy,
        clientMutationId = this.clientMutationId,
        pendingSync    = false,
        pendingDelete  = false
    )
}

fun VaccinationEntity.toVaccination(): Vaccination {
    return Vaccination(
        id             = this.id,
        vaccineId      = this.vaccineId,
        vaccineName    = this.vaccineName,
        dateGiven      = this.dateGiven,
        nextDueDate    = this.nextDueDate,
        lotNumber      = this.lotNumber,
        status         = this.status,
        administeredBy = this.administeredBy,
        clientMutationId = this.clientMutationId,
        attachedDocuments = emptyList()
    )
}
