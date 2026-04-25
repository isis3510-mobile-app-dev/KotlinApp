package com.example.petcare.data.repository

import android.content.Context
import android.util.Log
import android.util.LruCache
import androidx.work.*
import com.example.petcare.data.local.dao.PetDao
import com.example.petcare.data.local.dao.VaccineDao
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.data.local.entity.VaccinationEntity
import com.example.petcare.data.local.hive.HiveCacheManager
import com.example.petcare.data.local.lru.PetLruCache
import com.example.petcare.data.local.mapper.toCatalogEntity
import com.example.petcare.data.local.mapper.toEntity
import com.example.petcare.data.local.mapper.toPet
import com.example.petcare.data.local.mapper.toVaccination
import com.example.petcare.data.local.mapper.toVaccine
import com.example.petcare.data.model.*
import com.example.petcare.data.network.ApiService
import com.example.petcare.data.network.isOnline
import com.example.petcare.data.worker.SyncWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.util.UUID



class PetRepository(
    private val petDao: PetDao,
    private val vaccineDao: VaccineDao,
    private val api: ApiService,
    private val context: Context,
    private val hive: HiveCacheManager,
    private val lru: PetLruCache = PetLruCache()
) {
    private var vaccineCatalogRevision: Int = 0
    private val vaccineResponseCache = object : LruCache<String, List<Vaccination>>(120) {
        override fun sizeOf(key: String, value: List<Vaccination>): Int =
            value.size.coerceAtLeast(1)
    }

    private fun currentUserId(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: ""


    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun vaccineCacheKey(petId: String): String =
        "$petId:vaccineCatalogRevision=$vaccineCatalogRevision"

    private fun cacheVaccinations(petId: String, vaccinations: List<Vaccination>) {
        vaccineResponseCache.put(vaccineCacheKey(petId), vaccinations)
    }

    private suspend fun getCachedVaccinations(petId: String): List<Vaccination> {
        vaccineResponseCache.get(vaccineCacheKey(petId))?.let { return it }
        return vaccineDao.getVaccinesForPetSync(petId)
            .map { it.toVaccination() }
            .also { cacheVaccinations(petId, it) }
    }

    private fun invalidateVaccinationCache(petId: String) {
        vaccineResponseCache.snapshot().keys
            .filter { it.startsWith("$petId:") }
            .forEach { vaccineResponseCache.remove(it) }
    }

    suspend fun getPets(): Result<List<Pet>> {
        val uid = currentUserId()
        Log.d("PET_REPO", "isOnline=${isOnline(context)}, uid=$uid")

        lru.getPets(uid)?.let {
            Log.d("PET_CACHE", "L1 HIT — getPets")
            return Result.success(it)
        }

        if (isOnline(context)) {
            Log.d("Entró", "Deberia guardar Cache")
            val cached = hive.getPets(uid)
            if (cached != null) {
                return try {
                    Log.d("HIVE_CACHE", "HIT - getPets desde Hive")
                    val remotePets = Gson().fromJson(cached, Array<Pet>::class.java).toList()
                    val merged = mergePetsWithLocal(uid, remotePets)
                    lru.putPets(uid, merged)
                    Result.success(merged)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
        Log.d("HIVE_CACHE", "MISS - getPets va a la API")
        return if (isOnline(context)) {
            try {
                val response = api.getPets()
                if (!response.isSuccessful)
                    error("Failed to load pets — HTTP ${response.code()}")
                val remotePets = response.body().orEmpty()
                Log.d("HIVE_CACHE", "Guardando ${remotePets.size} pets en Hive")
                hive.putPets(uid, Gson().toJson(remotePets))
                cacheServerSnapshotPreservingPending(uid, remotePets)
                val merged = mergePetsWithLocal(uid, remotePets)
                lru.putPets(uid, merged)
                Result.success(merged)
            } catch (e: Exception) {
                android.util.Log.d("PET_REPO", "Network failed, falling back to Room")
                runCatching {
                    petDao.getAllPets(uid).first().map { pet ->
                        pet.toPet().copy(vaccinations = getCachedVaccinations(pet.id))
                    }
                }.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { Result.failure(it) }
                )
            }
        } else {
            runCatching {
                val cached = petDao.getAllPetsSync(uid)
                android.util.Log.d("PET_REPO", "Offline: found ${cached.size} cached pets for uid=$uid")
                cached.map { pet ->
                    val vaccinations = getCachedVaccinations(pet.id)
                    android.util.Log.d("PET_REPO", "Pet ${pet.id} has ${vaccinations.size} cached vaccinations")
                    pet.toPet().copy(vaccinations = vaccinations)
                }
            }
        }
    }

    suspend fun getPet(petId: String): Result<Pet> {
        val uid = currentUserId()
        lru.getPet(uid, petId)?.let {
            Log.d("CACHE", "L1 HIT — getPet $petId")
            return Result.success(it)
        }
        return if (isOnline(context)) {
            runCatching {
                val response = api.getPet(petId)
                val pet = response.body() ?: error("Pet not found")
                cacheServerPetPreservingPending(uid, pet)
                val merged = mergePetWithLocal(uid, pet)
                lru.putPet(uid, merged)               // puebla L1
                merged
            }.recoverCatching {
                val cached = petDao.getPetById(uid, petId)
                    ?: error("Pet not found and no cache available")
                cached.toPet().copy(vaccinations = getCachedVaccinations(petId))
            }
        } else {
            runCatching {
                val cached = petDao.getPetById(uid, petId)
                    ?: error("Pet not found offline")
                cached.toPet().copy(vaccinations = getCachedVaccinations(petId))
            }
        }
    }

    suspend fun createPet(request: CreatePetRequest): Result<Pet> = runCatching {
        val uid = currentUserId()
        return if (isOnline(context)) {
            runCatching {
                val response = api.createPet(request)
                val pet = response.body() ?: error("Failed to create pet")
                petDao.insertPet(pet.toEntity().copy(owner = uid))
                Log.d("HIVE_CACHE", "Invalidando cache de pets tras crear")
                lru.invalidateAllPets(uid)
                hive.invalidatePets(uid)
                pet
            }
        } else {
            runCatching {
                if (uid.isEmpty()) error("No authenticated user — cannot create pet offline")
                val tempId = "local_${UUID.randomUUID()}"
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
        return if (isOnline(context)) {
            runCatching {
                val response = api.updatePet(petId, request)
                val pet = response.body()
                    ?: error("Failed to update pet — HTTP ${response.code()}")
                lru.invalidatePet(uid, petId)         // L1 (entrada individual )
                hive.invalidatePets(uid)              // L2
                cacheServerPetPreservingPending(uid, pet)
                val merged = mergePetWithLocal(uid, pet)
                lru.putPet(uid, merged)               // re-puebla L1
                merged
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
        return if (isOnline(context)) {
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
                    lru.invalidatePet(uid, petId)
                    hive.invalidatePets(uid)
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
        return if (isOnline(context)) {
            runCatching {
                val response = api.addVaccination(petId, request)
                val pet = response.body() ?: error("Failed to add vaccination")
                cacheServerPetPreservingPending(uid, pet)
                hive.invalidatePets(uid)
                Log.d("VAX_REPO", "Online: vaccination added, pet has ${pet.vaccinations.size} vaccinations")
                mergePetWithLocal(uid, pet)
            }
        } else {
            runCatching {
                // Check pet exists first
                val cachedPet = petDao.getPetById(uid, petId)
                Log.d("PET_REPO", "Cached pet for vaccination: ${cachedPet?.name ?: "NULL"}")

                val tempId = "local_vax_${UUID.randomUUID()}"
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
                Log.d("PET_REPO", "Vaccination saved locally: $tempId for pet $petId")
                enqueueSyncWork()

                // Build response manually from cache + new vaccination
                invalidateVaccinationCache(petId)
                val allVaccinations = getCachedVaccinations(petId)
                Log.d("PET_REPO", "Total cached vaccinations for pet: ${allVaccinations.size}")

                cachedPet?.toPet()?.copy(
                    vaccinations = allVaccinations
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
        return if (isOnline(context)) {
            runCatching {
                val response = api.deleteVaccination(petId, vaccinationId)
                val pet = response.body()
                    ?: error("Failed to delete vaccination — HTTP ${response.code()}")
                // Remove from local cache too
                vaccineDao.deleteVaccineById(vaccinationId)
                cacheServerPetPreservingPending(uid, pet)
                hive.invalidatePets(uid)
                mergePetWithLocal(uid, pet)
            }
        } else {
            runCatching {
                // Flag for deletion — don't remove yet, server needs to confirm
                val existing = vaccineDao.getById(vaccinationId)
                    ?: error("Vaccination not found offline")
                vaccineDao.updateVaccine(existing.copy(pendingDelete = true))
                invalidateVaccinationCache(petId)
                enqueueSyncWork()
                // Return cached pet without the flagged vaccination
                val cachedPet = petDao.getPetById(uid, petId)
                    ?: error("Pet not found offline")
                cachedPet.toPet().copy(
                    vaccinations = getCachedVaccinations(petId)
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
        return if (isOnline(context)) {
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
                cacheServerPetPreservingPending(uid, pet)
                hive.invalidatePets(uid)
                mergePetWithLocal(uid, pet)
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
                invalidateVaccinationCache(petId)
                enqueueSyncWork()
                // Return cached pet with updated vaccinations
                val cachedPet = petDao.getPetById(uid, petId)
                    ?: error("Pet not found offline")
                cachedPet.toPet().copy(
                    vaccinations = getCachedVaccinations(petId)
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


        // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun cacheServerSnapshotPreservingPending(uid: String, remotePets: List<Pet>) {
        remotePets.forEach { remotePet ->
            cacheServerPetPreservingPending(uid, remotePet)
        }
    }

    private suspend fun cacheServerPetPreservingPending(uid: String, remotePet: Pet) {
        val localPet = petDao.getPetById(uid, remotePet.id)
        if (localPet == null || (!localPet.pendingSync && !localPet.pendingDelete)) {
            petDao.insertPet(remotePet.toEntity().copy(owner = uid))
        }
        upsertServerVaccinationsPreservingPending(remotePet.id, remotePet.vaccinations)
    }

    private suspend fun upsertServerVaccinationsPreservingPending(
        petId: String,
        remoteVaccinations: List<Vaccination>
    ) {
        remoteVaccinations.forEach { remoteVaccination ->
            if (remoteVaccination.id.isBlank()) return@forEach
            val localVaccine = vaccineDao.getById(remoteVaccination.id)
            if (localVaccine == null || (!localVaccine.pendingSync && !localVaccine.pendingDelete)) {
                vaccineDao.insertVaccine(remoteVaccination.toEntity(petId))
            }
        }
    }

    private suspend fun mergePetWithLocal(uid: String, remotePet: Pet): Pet {
        return mergePetsWithLocal(uid, listOf(remotePet)).firstOrNull { it.id == remotePet.id }
            ?: remotePet
    }

    private suspend fun mergePetsWithLocal(uid: String, remotePets: List<Pet>): List<Pet> {
        val localPets = petDao.getAllPetsSync(uid)
        val localPetsById = localPets.associateBy { it.id }
        val mergedPets = remotePets.map { remotePet ->
            val localPet = localPetsById[remotePet.id]
            val localVaccinations = vaccineDao.getVaccinesForPetSync(remotePet.id)
            val mergedVaccinations = mergeVaccinations(remotePet.vaccinations, localVaccinations)
            val mergedPet = if (localPet?.pendingSync == true) {
                localPet.toPet().copy(vaccinations = mergedVaccinations)
            } else {
                remotePet.copy(vaccinations = mergedVaccinations)
            }
            cacheVaccinations(mergedPet.id, mergedPet.vaccinations)
            mergedPet
        }.toMutableList()

        val knownIds = mergedPets.map { it.id }.toHashSet()
        localPets
            .asSequence()
            .filter { (it.pendingSync || it.id.startsWith("local_")) && !knownIds.contains(it.id) }
            .forEach { localPet ->
                val localVaccinations = vaccineDao.getVaccinesForPetSync(localPet.id)
                    .map { it.toVaccination() }
                val pendingPet = localPet.toPet().copy(vaccinations = localVaccinations)
                cacheVaccinations(pendingPet.id, pendingPet.vaccinations)
                mergedPets += pendingPet
            }

        return mergedPets
    }

    private fun mergeVaccinations(
        remoteVaccinations: List<Vaccination>,
        localVaccinations: List<VaccinationEntity>
    ): List<Vaccination> {
        val merged = linkedMapOf<String, Vaccination>()
        remoteVaccinations.forEach { vaccination ->
            if (vaccination.id.isNotBlank()) {
                merged[vaccination.id] = vaccination
            }
        }
        localVaccinations.forEach { local ->
            val keepLocalCopy = local.pendingSync || local.id.startsWith("local_vax_") || !merged.containsKey(local.id)
            if (keepLocalCopy) {
                merged[local.id] = local.toVaccination()
            }
        }
        return merged.values.toList()
    }

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
        return if (isOnline(context)) {
            runCatching {
                val response = api.getVaccines()
                val vaccines = response.body() ?: emptyList()
                vaccineCatalogRevision++
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
