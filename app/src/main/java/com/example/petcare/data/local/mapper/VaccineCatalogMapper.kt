package com.example.petcare.data.local.mapper

import com.example.petcare.data.local.entity.VaccineCatalogEntity
import com.example.petcare.data.model.Vaccine

fun Vaccine.toCatalogEntity(): VaccineCatalogEntity {
    return VaccineCatalogEntity(
        id           = this.id,
        name         = this.name,
        speciesRaw   = this.species.joinToString(","),
        productName  = this.productName,
        manufacturer = this.manufacturer,
        intervalDays = this.intervalDays,
        description  = this.description
    )
}

fun VaccineCatalogEntity.toVaccine(): Vaccine {
    return Vaccine(
        id           = this.id,
        name         = this.name,
        species      = this.species,
        productName  = this.productName,
        manufacturer = this.manufacturer,
        intervalDays = this.intervalDays,
        description  = this.description
    )
}