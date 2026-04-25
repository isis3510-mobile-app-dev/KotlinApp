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
import com.example.petcare.data.local.entity.VaccineCatalogEntity
import com.example.petcare.data.local.hive.HiveCacheManager
import com.example.petcare.data.local.lru.VaccineCatalogLruCache
import com.example.petcare.data.local.mapper.toCatalogEntity
import com.example.petcare.data.local.mapper.toEntity
import com.example.petcare.data.local.mapper.toPet
import com.example.petcare.data.local.mapper.toVaccination
import com.example.petcare.data.local.mapper.toVaccine
import com.example.petcare.data.model.*
import com.example.petcare.data.network.ApiService
import com.example.petcare.data.network.isOnline
import com.example.petcare.data.worker.SyncWorker
import com.example.petcare.util.FirebaseDocumentUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import android.net.Uri
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.UUID



class PetRepository(
    private val petDao: PetDao,
    private val vaccineDao: VaccineDao,
    private val api: ApiService,
    private val context: Context,
    private val hive: HiveCacheManager
) {

    private var vaccineCatalogRevision: Int = 0
    private val vaccineResponseCache = object : LruCache<String, List<Vaccination>>(120) {
        override fun sizeOf(key: String, value: List<Vaccination>): Int =
            value.size.coerceAtLeast(1)
    }

    private val vaccineCatalogCache = VaccineCatalogLruCache()

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
        Log.d(
            "VAX_ROOM",
            "L1 PUT vaccinations petId=$petId count=${vaccinations.size} revision=$vaccineCatalogRevision"
        )
    }

    private suspend fun getCachedVaccinations(petId: String): List<Vaccination> {
        vaccineResponseCache.get(vaccineCacheKey(petId))?.let {
            Log.d("VAX_ROOM", "L1 HIT vaccinations petId=$petId count=${it.size}")
            return it
        }
        return vaccineDao.getVaccinesForPetSync(petId)
            .map { it.toVaccination() }
            .also {
                Log.d("VAX_ROOM", "ROOM READ vaccinations petId=$petId count=${it.size}")
                cacheVaccinations(petId, it)
            }
    }

    private fun invalidateVaccinationCache(petId: String) {
        val keys = vaccineResponseCache.snapshot().keys
            .filter { it.startsWith("$petId:") }
        keys.forEach { vaccineResponseCache.remove(it) }
        Log.d("VAX_ROOM", "L1 INVALIDATE vaccinations petId=$petId removedKeys=${keys.size}")
    }

    suspend fun getPets(): Result<List<Pet>> {
        val uid = currentUserId()
        Log.d("PET_REPO", "isOnline=${isOnline(context)}, uid=$uid")

        if (isOnline(context)) {
            Log.d("Entró", "Deberia guardar Cache")
            val cached = hive.getPets(uid)
            if (cached != null) {
                return try {
                    Log.d("HIVE_CACHE", "HIT - getPets desde Hive")
                    val remotePets = Gson().fromJson(cached, Array<Pet>::class.java).toList()
                    val merged = mergePetsWithLocal(uid, remotePets)
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
        return if (isOnline(context)) {
            runCatching {
                val response = api.getPet(petId)
                val pet = response.body() ?: error("Pet not found")
                cacheServerPetPreservingPending(uid, pet)
                val merged = mergePetWithLocal(uid, pet)
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
                hive.invalidatePets(uid)              // L2
                cacheServerPetPreservingPending(uid, pet)
                val merged = mergePetWithLocal(uid, pet)
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
                Log.d("VAX_ROOM", "Online add vaccination -> Room snapshot updated petId=$petId")
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
                Log.d("VAX_ROOM", "ROOM PENDING_CREATE vaccinationId=$tempId petId=$petId")
                hive.invalidatePets(uid)
                Log.d("VAX_ROOM", "Invalidated pet Hive cache after offline vaccination create petId=$petId")
                enqueueSyncWork()
                Log.d("VAX_SYNC", "Queued SyncWorker after offline vaccination create id=$tempId")

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
        val uid = currentUserId()
        Log.d(
            "DOC_UPLOAD",
            "Saving vaccination document metadata petId=$petId vaccinationId=$vaccinationId fileName=${request.fileName}"
        )
        val response = api.addVaccinationDocument(petId, vaccinationId, request)
        val pet = response.body() ?: error("Failed to add document")
        invalidateVaccinationCache(petId)
        hive.invalidatePets(uid)
        Log.d(
            "DOC_UPLOAD",
            "Backend document metadata saved petId=$petId vaccinationId=$vaccinationId totalVaccinations=${pet.vaccinations.size}"
        )
        pet
    }

    suspend fun queueVaccinationDocument(
        sourceUri: Uri,
        petId: String,
        vaccinationId: String,
        fileName: String,
        mimeType: String
    ): Result<PendingVaccinationDocument> = runCatching {
        val id = UUID.randomUUID().toString()
        val safeFileName = fileName.replace(Regex("""[^\w.\-]"""), "_")
        val dir = File(context.filesDir, "pending_vaccination_documents/$petId/$vaccinationId")
        dir.mkdirs()
        val localFile = File(dir, "${id}_$safeFileName")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            localFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read selected document")

        val pending = PendingVaccinationDocument(
            id = id,
            petId = petId,
            vaccinationId = vaccinationId,
            fileName = fileName,
            mimeType = mimeType,
            localUri = Uri.fromFile(localFile).toString()
        )
        val updated = getPendingVaccinationDocuments(petId, vaccinationId) + pending
        hive.putPendingVaccinationDocuments(petId, vaccinationId, Gson().toJson(updated))
        Log.d(
            "DOC_UPLOAD",
            "Queued pending vaccination document id=$id petId=$petId vaccinationId=$vaccinationId file=${localFile.absolutePath} bytes=${localFile.length()}"
        )
        enqueueSyncWork()
        pending
    }

    fun getPendingVaccinationDocuments(
        petId: String,
        vaccinationId: String
    ): List<PendingVaccinationDocument> {
        val json = hive.getPendingVaccinationDocuments(petId, vaccinationId) ?: return emptyList()
        return runCatching {
            Gson().fromJson(json, Array<PendingVaccinationDocument>::class.java).toList()
        }.getOrElse {
            Log.e("DOC_UPLOAD", "Failed to parse pending vaccination documents: ${it.message}", it)
            emptyList()
        }
    }

    suspend fun syncPendingVaccinationDocuments(
        petId: String,
        vaccinationId: String
    ): Result<Int> = runCatching {
        if (!isOnline(context)) {
            Log.d("DOC_UPLOAD", "Pending document sync skipped offline petId=$petId vaccinationId=$vaccinationId")
            return@runCatching 0
        }

        val pendingDocs = getPendingVaccinationDocuments(petId, vaccinationId)
        if (pendingDocs.isEmpty()) return@runCatching 0

        Log.d(
            "DOC_UPLOAD",
            "Pending document sync start petId=$petId vaccinationId=$vaccinationId count=${pendingDocs.size}"
        )
        val remaining = pendingDocs.toMutableList()
        var synced = 0

        pendingDocs.forEach { pending ->
            runCatching {
                val uploaded = FirebaseDocumentUploader
                    .uploadVaccinationDocument(context, Uri.parse(pending.localUri), petId, vaccinationId)
                    .getOrThrow()

                addVaccinationDocument(
                    petId,
                    vaccinationId,
                    AddDocumentRequest(
                        fileName = pending.fileName,
                        fileUri = uploaded.downloadUrl
                    )
                ).getOrThrow()

                remaining.remove(pending)
                deleteLocalPendingFile(pending.localUri)
                synced++
                Log.d("DOC_UPLOAD", "Pending document synced id=${pending.id} fileName=${pending.fileName}")
            }.onFailure {
                Log.e("DOC_UPLOAD", "Pending document sync failed id=${pending.id}: ${it.message}", it)
            }
        }

        if (remaining.isEmpty()) {
            hive.invalidatePendingVaccinationDocuments(petId, vaccinationId)
        } else {
            hive.putPendingVaccinationDocuments(petId, vaccinationId, Gson().toJson(remaining))
        }
        if (synced > 0) {
            val uid = currentUserId()
            invalidateVaccinationCache(petId)
            hive.invalidatePets(uid)
            Log.d("DOC_UPLOAD", "Invalidated pet caches after pending document sync petId=$petId synced=$synced")
        }
        synced
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
                Log.d("VAX_ROOM", "ROOM DELETE vaccinationId=$vaccinationId after online delete")
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
                Log.d("VAX_ROOM", "ROOM PENDING_DELETE vaccinationId=$vaccinationId petId=$petId")
                hive.invalidatePets(uid)
                Log.d("VAX_ROOM", "Invalidated pet Hive cache after offline vaccination delete petId=$petId")
                invalidateVaccinationCache(petId)
                enqueueSyncWork()
                Log.d("VAX_SYNC", "Queued SyncWorker after offline vaccination delete id=$vaccinationId")
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
                Log.d("VAX_ROOM", "Online update vaccination -> Room snapshot updated vaccinationId=$vaccinationId")
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
                Log.d("VAX_ROOM", "ROOM PENDING_UPDATE vaccinationId=$vaccinationId petId=$petId")
                hive.invalidatePets(uid)
                Log.d("VAX_ROOM", "Invalidated pet Hive cache after offline vaccination update petId=$petId")
                invalidateVaccinationCache(petId)
                enqueueSyncWork()
                Log.d("VAX_SYNC", "Queued SyncWorker after offline vaccination update id=$vaccinationId")
                // Return cached pet with updated vaccinations
                val cachedPet = petDao.getPetById(uid, petId)
                    ?: error("Pet not found offline")
                cachedPet.toPet().copy(
                    vaccinations = getCachedVaccinations(petId)
                )
            }
        }
    }

    private fun deleteLocalPendingFile(localUri: String) {
        runCatching {
            val path = Uri.parse(localUri).path ?: return
            File(path).delete()
        }
    }


    suspend fun getPetSmart(petId: String): Result<List<SuggestionDto>> = runCatching {
        val online = isOnline(context)
        Log.d("SMART_CACHE", "getPetSmart start petId=$petId online=$online")

        hive.getSuggestions(petId)?.let { cachedJson ->
            val cached = parseSuggestions(cachedJson)
            Log.d("SMART_CACHE", "HIT Hive suggestions petId=$petId count=${cached.size}")
            return@runCatching cached
        }

        if (!online) {
            Log.d("SMART_CACHE", "MISS offline suggestions petId=$petId -> emptyList")
            return@runCatching emptyList()
        }

        Log.d("SMART_CACHE", "MISS online suggestions petId=$petId -> API")
        val response = api.getPetSmart(petId)
        if (!response.isSuccessful) {
            Log.w("SMART_CACHE", "API failed suggestions petId=$petId http=${response.code()}")
            return@runCatching emptyList()
        }

        val suggestions = response.body()?.suggestions.orEmpty()
        hive.putSuggestions(petId, Gson().toJson(suggestions))
        Log.d("SMART_CACHE", "API success suggestions petId=$petId count=${suggestions.size} -> Hive saved")
        suggestions
    }

    private fun parseSuggestions(json: String): List<SuggestionDto> =
        Gson().fromJson(json, Array<SuggestionDto>::class.java).toList()


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

        vaccineCatalogCache.get()?.let { cached ->
            Log.d("VAX_CACHE", "LRU HIT - ${cached.size} vaccines")
            return Result.success(cached)
        }

        Log.d("VAX_CACHE", "LRU MISS")

        val db = AppDatabase.getInstance(context)

        return if (isOnline(context)) {

            runCatching {

                Log.d("VAX_CACHE", "API call")

                val response = api.getVaccines()
                val vaccines = response.body().orEmpty()

                Log.d("VAX_CACHE", "API returned ${vaccines.size}")

                db.vaccineCatalogDao().clearAll()
                db.vaccineCatalogDao().insertAll(vaccines.map { it.toCatalogEntity() })
                vaccineCatalogCache.put(vaccines)
                Log.d("VAX_CACHE", "LRU stored")
                android.util.Log.d("PET_REPO", "Cached ${vaccines.size} catalog vaccines")
                android.util.Log.d("VAX_ROOM", "ROOM UPSERT vaccine catalog count=${vaccines.size} revision=$vaccineCatalogRevision")
                vaccines

            }.recoverCatching {
                Log.e("VAX_CACHE", "API failed, fallback to Room")

                val cached = db.vaccineCatalogDao().getAll()
                Log.d("VAX_CACHE", "Room returned ${cached.size}")
                Log.d("VAX_ROOM", "ROOM FALLBACK vaccine catalog count=${cached.size}")

                val domain = cached.map { it.toVaccine() }

                vaccineCatalogCache.put(domain)

                domain
            }

        } else {

            Log.d("VAX_CACHE", "OFFLINE")

            val cached = db.vaccineCatalogDao().getAll()
            Log.d("VAX_CACHE", "Offline Room ${cached.size}")
            Log.d("VAX_ROOM", "OFFLINE ROOM READ vaccine catalog count=${cached.size}")

            val domain = cached.map { it.toVaccine() }

            vaccineCatalogCache.put(domain)

            return Result.success(domain)
        }
    }


}
