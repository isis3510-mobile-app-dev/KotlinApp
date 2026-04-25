package com.example.petcare.data.local.mapper

import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.data.model.Pet
import com.google.firebase.auth.FirebaseAuth

fun Pet.toEntity(): PetEntity {
    val ownerId = this.owners.firstOrNull()
        ?: FirebaseAuth.getInstance().currentUser?.uid
        ?: ""
    android.util.Log.d("PET_MAPPER", "Pet '${this.name}' owners=${this.owners} → stored owner='$ownerId'")
    return PetEntity(
        id             = this.id,
        name           = this.name,
        species        = this.species,
        breed          = this.breed,
        gender         = this.gender,
        weight         = this.weight,
        color          = this.color,
        birthDate      = this.birthDate,
        photoUrl       = this.photoUrl,
        status         = this.status,
        isNfcSynced    = this.isNfcSynced,
        knownAllergies = this.knownAllergies,
        defaultVet     = this.defaultVet,
        defaultClinic  = this.defaultClinic,
        owner          = ownerId,
        pendingSync    = false,
        pendingDelete  = false
    )
}

fun PetEntity.toPet() = Pet(
    id             = this.id,
    name           = this.name,
    species        = this.species,
    breed          = this.breed,
    gender         = this.gender,
    birthDate      = this.birthDate,
    weight         = this.weight,
    color          = this.color,
    photoUrl       = this.photoUrl,
    status         = this.status,
    isNfcSynced    = this.isNfcSynced,
    knownAllergies = this.knownAllergies,
    defaultVet     = this.defaultVet,
    defaultClinic  = this.defaultClinic,
    owners         = listOf(this.owner),
    vaccinations   = emptyList()
)
