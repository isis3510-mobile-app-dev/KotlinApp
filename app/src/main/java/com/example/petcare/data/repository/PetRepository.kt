package com.example.petcare.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import com.example.petcare.data.local.dao.PetDao
import com.example.petcare.data.local.dao.VaccineDao
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.data.local.entity.VaccinationEntity
import com.example.petcare.data.local.mapper.toCatalogEntity
import com.example.petcare.data.local.mapper.toEntity
import com.example.petcare.data.local.mapper.toPet
import com.example.petcare.data.local.mapper.toVaccination
import com.example.petcare.data.local.mapper.toVaccine
import com.example.petcare.data.model.*
import com.example.petcare.data.network.ApiService
import com.example.petcare.data.worker.SyncWorker
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first


class PetRepository(
    private val petDao: PetDao,
    private val vaccineDao: VaccineDao,
    private val api: ApiService,
    private val context: Context
) {

    private fun currentUserId(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "pet_sync",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    suspend fun getPets(): Result<List<Pet>> {
        val uid = currentUserId()
        android.util.Log.d("PET_REPO", "isOnline=${isOnline()}, uid=$uid")
        return if (isOnline()) {
            //syncPendingLocalData()
            runCatching {
                val response = api.getPets()
                if (!response.isSuccessful)
                    error("Failed to load pets — HTTP ${response.code()}")
                val pets = response.body().orEmpty()
                android.util.Log.d("PET_REPO", "Caching ${pets.size} pets, uid=$uid")
                val entities = pets.map { pet ->
                    pet.toEntity().copy(owner = uid)
                }
                android.util.Log.d("PET_REPO", "Caching ${entities.size} pets, uid=$uid")
                petDao.insertAll(entities)
                android.util.Log.d("PET_REPO", "Inserted ${entities.size} pet entities")
                pets.forEach { pet ->
                    val vaxEntities = pet.vaccinations.map { it.toEntity(pet.id) }
                    android.util.Log.d("PET_REPO", "Pet ${pet.name} has ${pet.vaccinations.size} vaccinations → inserting ${vaxEntities.size}")
                    if (vaxEntities.isNotEmpty()) {
                        vaccineDao.insertAll(vaxEntities)
                    }
                }

                pets
            }.recoverCatching {
                android.util.Log.d("PET_REPO", "Network failed, falling back to Room")
                petDao.getAllPets(uid).first().map { pet ->
                    val vaccinations = vaccineDao.getVaccinesForPetSync(pet.id)
                    pet.toPet().copy(vaccinations = vaccinations.map { it.toVaccination() })
                }
            }
        } else {
            runCatching {
                val cached = petDao.getAllPetsSync(uid)
                android.util.Log.d("PET_REPO", "Offline: found ${cached.size} cached pets for uid=$uid")
                cached.map { pet ->
                    val vaccinations = vaccineDao.getVaccinesForPetSync(pet.id)
                    android.util.Log.d("PET_REPO", "Pet ${pet.id} has ${vaccinations.size} cached vaccinations")
                    pet.toPet().copy(vaccinations = vaccinations.map { it.toVaccination() })
                }
            }
        }
    }

    suspend fun getPet(petId: String): Result<Pet> {
        val uid = currentUserId()
        return if (isOnline()) {
            runCatching {
                val response = api.getPet(petId)
                val pet = response.body() ?: error("Pet not found")
                petDao.insertPet(pet.toEntity().copy(owner = uid))
                // Cache this pet's vaccinations
                vaccineDao.insertAll(pet.vaccinations.map { it.toEntity(petId) })
                android.util.Log.d("PET_REPO", "Pet ${pet.name} has ${pet.vaccinations.size} vaccinations")
                pet
            }.recoverCatching {
                val cached = petDao.getPetById(uid, petId)
                    ?: error("Pet not found and no cache available")
                val vaccinations = vaccineDao.getVaccinesForPetSync(petId)
                cached.toPet().copy(vaccinations = vaccinations.map { it.toVaccination() })
            }
        } else {
            runCatching {
                val cached = petDao.getPetById(uid, petId)
                    ?: error("Pet not found offline")
                val vaccinations = vaccineDao.getVaccinesForPetSync(petId)
                cached.toPet().copy(vaccinations = vaccinations.map { it.toVaccination() })
            }
        }
    }

    suspend fun createPet(request: CreatePetRequest): Result<Pet> = runCatching {
        val uid = currentUserId()
        return if (isOnline()) {
            runCatching {
                val response = api.createPet(request)
                val pet = response.body() ?: error("Failed to create pet")
                petDao.insertPet(pet.toEntity())
                pet
            }
        } else {
            runCatching {
                if (uid.isEmpty()) error("No authenticated user — cannot create pet offline")
                val tempId = "local_${System.currentTimeMillis()}"
                val entity = PetEntity(
                    id             = tempId,       // ← was wrongly 'remoteId'
                    name           = request.name,
                    species        = request.species,
                    breed          = request.breed,
                    gender         = request.gender,
                    weight         = request.weight,
                    color          = request.color,
                    birthDate      = request.birthDate,
                    photoUrl       = request.photoUrl,
                    status         = "healthy",
                    isNfcSynced    = false,
                    knownAllergies = request.knownAllergies,
                    defaultVet     = request.defaultVet,
                    defaultClinic  = request.defaultClinic,
                    owner          = uid,
                    pendingSync    = true,
                    pendingDelete  = false
                )
                petDao.insertPet(entity)
                enqueueSyncWork()
                entity.toPet()
            }
        }
    }


    suspend fun updatePet(petId: String, request: UpdatePetRequest): Result<Pet> = runCatching {
        val uid = currentUserId()
        return if (isOnline()) {
            runCatching {
                val response = api.updatePet(petId, request)
                val pet = response.body()
                    ?: error("Failed to update pet — HTTP ${response.code()}")
                petDao.insertPet(pet.toEntity())
                pet
            }
        } else {
            runCatching {
                val existing = petDao.getPetById(uid, petId)
                    ?: error("Pet not found offline")
                val updated = existing.copy(
                    name           = request.name          ?: existing.name,
                    species        = request.species       ?: existing.species,
                    breed          = request.breed         ?: existing.breed,
                    gender         = request.gender        ?: existing.gender,
                    weight         = request.weight        ?: existing.weight,
                    color          = request.color         ?: existing.color,
                    birthDate      = request.birthDate     ?: existing.birthDate,
                    photoUrl       = request.photoUrl      ?: existing.photoUrl,
                    status         = request.status        ?: existing.status,
                    isNfcSynced    = request.isNfcSynced   ?: existing.isNfcSynced,
                    knownAllergies = request.knownAllergies ?: existing.knownAllergies,
                    defaultVet     = request.defaultVet    ?: existing.defaultVet,
                    defaultClinic  = request.defaultClinic ?: existing.defaultClinic,
                    pendingSync    = true
                )
                petDao.updatePet(updated)
                enqueueSyncWork()
                updated.toPet()
            }
        }
    }

    suspend fun deletePet(petId: String): Result<Unit> {
        val uid = currentUserId()
        return if (isOnline()) {
            try {
                val response = api.deletePet(petId)
                if (response.isSuccessful || response.code() == 204) {
                    petDao.deletePetById(petId)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete — HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("204") && msg.contains("Content-Length")) {
                    petDao.deletePetById(petId)
                    Result.success(Unit)
                } else {
                    Result.failure(e)
                }
            }
        } else {
            runCatching {
                val existing = petDao.getPetById(uid, petId)
                    ?: error("Pet not found offline")
                petDao.updatePet(existing.copy(pendingDelete = true))
                enqueueSyncWork()
            }
        }
    }

    suspend fun addVaccination(
        petId: String,
        request: AddVaccinationRequest
    ): Result<Pet> {
        val uid = currentUserId()
        return if (isOnline()) {
            runCatching {
                val response = api.addVaccination(petId, request)
                val pet = response.body() ?: error("Failed to add vaccination")
                // Cache the updated pet and all its vaccinations
                petDao.insertPet(pet.toEntity().copy(owner = uid))
                vaccineDao.insertAll(pet.vaccinations.map { it.toEntity(petId) })
                android.util.Log.d("VAX_REPO", "Online: vaccination added, pet has ${pet.vaccinations.size} vaccinations")
                pet
            }
        } else {
            runCatching {
                // Check pet exists first
                val cachedPet = petDao.getPetById(uid, petId)
                android.util.Log.d("PET_REPO", "Cached pet for vaccination: ${cachedPet?.name ?: "NULL"}")

                val tempId = "local_vax_${System.currentTimeMillis()}"
                val entity = VaccinationEntity(
                    id             = tempId,
                    petId          = petId,
                    vaccineId      = request.vaccineId,
                    dateGiven      = request.dateGiven,
                    nextDueDate    = request.nextDueDate,
                    lotNumber      = request.lotNumber,
                    status         = request.status,
                    administeredBy = request.administeredBy,
                    pendingSync    = true,
                    pendingDelete  = false
                )
                vaccineDao.insertVaccine(entity)
                android.util.Log.d("PET_REPO", "Vaccination saved locally: $tempId for pet $petId")
                enqueueSyncWork()

                // Build response manually from cache + new vaccination
                val allVaccinations = vaccineDao.getVaccinesForPetSync(petId)
                android.util.Log.d("PET_REPO", "Total cached vaccinations for pet: ${allVaccinations.size}")

                cachedPet?.toPet()?.copy(
                    vaccinations = allVaccinations.map { it.toVaccination() }
                ) ?: error("Pet not found offline — cannot add vaccination")
            }
        }
    }

    suspend fun addVaccinationDocument(
        petId: String,
        vaccinationId: String,
        request: AddDocumentRequest
    ): Result<Pet> = runCatching {
        val response = api.addVaccinationDocument(petId, vaccinationId, request)
        response.body() ?: error("Failed to add document")
    }

    suspend fun deleteVaccination(petId: String, vaccinationId: String): Result<Pet> {
        val uid = currentUserId()
        return if (isOnline()) {
            runCatching {
                val response = api.deleteVaccination(petId, vaccinationId)
                val pet = response.body()
                    ?: error("Failed to delete vaccination — HTTP ${response.code()}")
                // Remove from local cache too
                vaccineDao.deleteVaccineById(vaccinationId)
                petDao.insertPet(pet.toEntity())
                pet
            }
        } else {
            runCatching {
                // Flag for deletion — don't remove yet, server needs to confirm
                val existing = vaccineDao.getById(vaccinationId)
                    ?: error("Vaccination not found offline")
                vaccineDao.updateVaccine(existing.copy(pendingDelete = true))
                enqueueSyncWork()
                // Return cached pet without the flagged vaccination
                val cachedPet = petDao.getPetById(uid, petId)
                    ?: error("Pet not found offline")
                val cachedVaccinations = vaccineDao.getVaccinesForPetSync(petId)
                cachedPet.toPet().copy(
                    vaccinations = cachedVaccinations.map { it.toVaccination() }
                )
            }
        }
    }

    suspend fun updateVaccination(
        petId: String,
        vaccinationId: String,
        administeredBy: String,
        nextDueDate: String?,
        lotNumber: String
    ): Result<Pet> = runCatching {
        val uid = currentUserId()
        return if (isOnline()) {
            runCatching {
                // Use UpdateVaccinationRequest instead of raw Map
                val existing = vaccineDao.getById(vaccinationId)
                    ?: error("Vaccination not found")
                val body = UpdateVaccinationRequest(
                    vaccineId      = existing.vaccineId,
                    dateGiven      = existing.dateGiven,
                    nextDueDate    = if (nextDueDate != null) toIso(nextDueDate) else existing.nextDueDate,
                    lotNumber      = lotNumber,
                    administeredBy = administeredBy
                )
                val response = api.updateVaccination(petId, vaccinationId, body)
                val pet = response.body()
                    ?: error("Failed to update vaccination — HTTP ${response.code()}")
                // Refresh cache
                petDao.insertPet(pet.toEntity())
                vaccineDao.insertAll(pet.vaccinations.map { it.toEntity(petId) })
                pet
            }
        } else {
            runCatching {
                val existing = vaccineDao.getById(vaccinationId)
                    ?: error("Vaccination not found offline")
                val updated = existing.copy(
                    administeredBy = administeredBy,
                    nextDueDate    = nextDueDate ?: existing.nextDueDate,
                    lotNumber      = lotNumber,
                    pendingSync    = true
                )
                vaccineDao.updateVaccine(updated)
                enqueueSyncWork()
                // Return cached pet with updated vaccinations
                val cachedPet = petDao.getPetById(uid, petId)
                    ?: error("Pet not found offline")
                val cachedVaccinations = vaccineDao.getVaccinesForPetSync(petId)
                cachedPet.toPet().copy(
                    vaccinations = cachedVaccinations.map { it.toVaccination() }
                )
            }
        }
    }


    suspend fun getPetSmart(petId: String): Result<List<SuggestionDto>> = runCatching {
        val response = api.getPetSmart(petId)
        if (response.isSuccessful) {
            response.body()?.suggestions ?: emptyList()
        } else {
            emptyList()
        }
    }

    private suspend fun syncPendingLocalData() {
        val db = AppDatabase.getInstance(context)
        val pendingVax = db.vaccineDao().getPendingSync()
            .filter { it.id.startsWith("local_vax_") }
            .filter { !it.petId.startsWith("local_") }

        android.util.Log.d("PET_REPO", "Pre-fetch sync: ${pendingVax.size} pending vaccinations")

        pendingVax.forEach { entity ->
            try {
                val response = api.addVaccination(
                    entity.petId,
                    AddVaccinationRequest(
                        vaccineId      = entity.vaccineId,
                        dateGiven      = entity.dateGiven,
                        nextDueDate    = entity.nextDueDate,
                        lotNumber      = entity.lotNumber,
                        status         = entity.status,
                        administeredBy = entity.administeredBy
                    )
                )
                val pet = response.body()
                if (pet != null) {
                    db.vaccineDao().deleteVaccineById(entity.id)
                    db.vaccineDao().insertAll(pet.vaccinations.map { it.toEntity(entity.petId) })
                    android.util.Log.d("PET_REPO", "Pre-fetch: synced vaccination ${entity.id}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PET_REPO", "Pre-fetch sync failed for ${entity.id}: ${e.message}")
                // Don't fail — just leave it pending, SyncWorker will handle it
            }
        }
    }


        // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Converts a date string to ISO-8601 format expected by the backend.
     *
     * Handles two input formats:
     *  - dd/MM/yyyy  (from DateTextField picker)  → yyyy-MM-ddT00:00:00Z
     *  - yyyy-MM-dd  (already ISO, from API)       → yyyy-MM-ddT00:00:00Z
     */
    private fun toIso(date: String): String {
        if (date.isBlank()) return date
        return try {
            when {
                // dd/MM/yyyy
                date.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> {
                    val parts = date.split("/")
                    "${parts[2]}-${parts[1]}-${parts[0]}T00:00:00Z"
                }
                // yyyy-MM-dd (with or without time suffix)
                date.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) -> {
                    "${date.take(10)}T00:00:00Z"
                }
                else -> date
            }
        } catch (_: Exception) {
            date
        }
    }

    suspend fun getVaccineCatalog(): Result<List<Vaccine>> {
        return if (isOnline()) {
            runCatching {
                val response = api.getVaccines()
                val vaccines = response.body() ?: emptyList()
                // Cache the catalog for offline use
                val db = AppDatabase.getInstance(context)
                db.vaccineCatalogDao().clearAll()
                db.vaccineCatalogDao().insertAll(vaccines.map { it.toCatalogEntity() })
                android.util.Log.d("PET_REPO", "Cached ${vaccines.size} catalog vaccines")
                vaccines
            }.recoverCatching {
                // Network failed — fall back to cached catalog
                android.util.Log.d("PET_REPO", "Catalog fetch failed, using cache")
                val db = AppDatabase.getInstance(context)
                db.vaccineCatalogDao().getAll().map { it.toVaccine() }
            }
        } else {
            runCatching {
                val db = AppDatabase.getInstance(context)
                val cached = db.vaccineCatalogDao().getAll()
                android.util.Log.d("PET_REPO", "Offline: found ${cached.size} cached catalog vaccines")
                cached.map { it.toVaccine() }
            }
        }
    }

}
