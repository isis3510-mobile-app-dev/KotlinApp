package com.example.petcare.data.local.mapper

import com.example.petcare.data.local.entity.VaccinationEntity
import com.example.petcare.data.model.Vaccination

fun Vaccination.toEntity(petId: String): VaccinationEntity {
    return VaccinationEntity(
        id             = this.id,
        petId          = petId,
        vaccineId      = this.vaccineId,
        dateGiven      = this.dateGiven,
        nextDueDate    = this.nextDueDate,
        lotNumber      = this.lotNumber,
        status         = this.status,
        administeredBy = this.administeredBy,
        pendingSync    = false,
        pendingDelete  = false
    )
}

fun VaccinationEntity.toVaccination(): Vaccination {
    return Vaccination(
        id             = this.id,
        vaccineId      = this.vaccineId,
        dateGiven      = this.dateGiven,
        nextDueDate    = this.nextDueDate,
        lotNumber      = this.lotNumber,
        status         = this.status,
        administeredBy = this.administeredBy,
        attachedDocuments = emptyList()
    )
}