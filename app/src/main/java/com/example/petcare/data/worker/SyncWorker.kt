package com.example.petcare.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.hive.HiveCacheManager
import com.example.petcare.data.local.mapper.toEntity
import com.example.petcare.data.model.AddDocumentRequest
import com.example.petcare.data.model.AddVaccinationRequest
import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.CreatePetRequest
import com.example.petcare.data.model.PendingEventDocument
import com.example.petcare.data.model.PendingVaccinationDocument
import com.example.petcare.data.model.CreateWeightLogRequest
import com.example.petcare.data.model.UpdatePetRequest
import com.example.petcare.data.model.UpdateVaccinationRequest
import com.example.petcare.data.model.UpdateWeightLogRequest
import com.example.petcare.data.network.ApiService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.storage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "pet_sync"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO){
        android.util.Log.d("SYNC_WORKER", "doWork started")
        delay(1500)

        val db = AppDatabase.getInstance(applicationContext)

        val user = FirebaseAuth.getInstance().currentUser
        android.util.Log.d("SYNC_WORKER", "Current Firebase user: ${user?.uid ?: "NULL"}")
        if (user == null) return@withContext Result.retry()

        // Fetch token once using coroutine-native approach — avoids runBlocking deadlock
        val token = try {
            user.getIdToken(true).await().token
        } catch (e: Exception) {
            android.util.Log.e("SYNC_WORKER", "Token fetch failed: ${e.message}")
            null
        }

        if (token.isNullOrBlank()) {
            android.util.Log.e("SYNC_WORKER", "No token — retrying later")
            return@withContext Result.retry()
        }

        android.util.Log.d("SYNC_WORKER", "Token fetched: ${token.take(20)}...")

        // Build a one-off API client with the token baked in — no interceptor needed
        val api = buildAuthenticatedApi(token)

        return@withContext try{
            syncPendingCreates(db, api)
            syncPendingUpdates(db, api)
            syncPendingDeletes(db, api)

            coroutineScope {
                launch(Dispatchers.IO + CoroutineName("vaccination-sync")) {
                    android.util.Log.d("VAX_SYNC", "vaccination-sync coroutine started thread=${Thread.currentThread().name}")
                    syncPendingVaccinationCreates(db, api)
                    syncPendingVaccinationUpdates(db, api)
                    syncPendingVaccinationDeletes(db, api)
                    syncPendingVaccinationDocuments(api)
                    android.util.Log.d("VAX_SYNC", "vaccination-sync coroutine finished thread=${Thread.currentThread().name}")
                }
                launch(Dispatchers.IO + CoroutineName("events-sync")) {
                    android.util.Log.d("EVENT_SYNC", "event-sync coroutine started thread=${Thread.currentThread().name}")
                    syncPendingEventCreates(db, api)
                    syncPendingEventUpdates(db, api)
                    syncPendingEventDeletes(db, api)
                    //syncPendingVaccinationDocuments(api)
                    android.util.Log.d("EVENT_SYNC", "event-sync coroutine finished thread=${Thread.currentThread().name}")
                }
                launch(Dispatchers.IO + CoroutineName("weight-sync")) {
                    syncPendingWeightLogCreates(db, api)
                    syncPendingWeightLogUpdates(db, api)
                    syncPendingWeightLogDeletes(db, api)
                }
            }
            HiveCacheManager(applicationContext).invalidatePets(user.uid)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("SYNC_WORKER", "Sync failed: ${e.message}")
            Result.retry()
        }
    }

    // Builds a plain Retrofit client with the token as a static header
    // Bypasses AuthInterceptor entirely — no runBlocking, no deadlock
    private fun buildAuthenticatedApi(token: String): ApiService {
        val BASE_URL = com.example.petcare.BuildConfig.BASE_URL
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Pets created offline with a "local_" temp ID — POST to server
    private suspend fun syncPendingCreates(db: AppDatabase, api: ApiService) {
        val pending = db.petDao().getPendingSync()
            .filter { it.id.startsWith("local_") }

        android.util.Log.d("SYNC_WORKER", "Pending creates: ${pending.size}")
        if (pending.isEmpty()) return

        pending.forEach { entity ->
            android.util.Log.d("SYNC_WORKER", "Uploading pet: ${entity.name}, id=${entity.id}")
            try {
                val resolvedPhotoUrl = uploadLocalPhotoIfNeeded(entity.photoUrl)
                android.util.Log.d("SYNC_WORKER", "About to call api.createPet()")
                val response = api.createPet(
                    CreatePetRequest(
                        name           = entity.name,
                        species        = entity.species,
                        breed          = entity.breed,
                        gender         = entity.gender,
                        weight         = entity.weight,
                        color          = entity.color,
                        birthDate      = entity.birthDate,
                        photoUrl       = resolvedPhotoUrl,
                        knownAllergies = entity.knownAllergies,
                        defaultVet     = entity.defaultVet,
                        defaultClinic  = entity.defaultClinic
                    )
                )
                android.util.Log.d("SYNC_WORKER", "Response: ${response.code()}, id=${response.body()?.id}")
                val created = response.body()
                if (created != null) {
                    db.petDao().insertPet(
                        created.toEntity().copy(
                            owner = FirebaseAuth.getInstance().currentUser?.uid ?: entity.owner,
                            pendingSync = false
                        )
                    )

                    // Update any pending vaccinations for this pet to use the new server ID
                    db.vaccineDao().getVaccinesForPetSync(entity.id).forEach { vax ->
                        db.vaccineDao().deleteVaccineById(vax.id)
                        db.vaccineDao().insertVaccine(vax.copy(petId = created.id))
                    }
                    db.weightLogDao().moveToServerPet(entity.id, created.id)

                    db.eventDao().moveToServerPet(entity.id, created.id)

                    movePendingDocumentsToServerPet(
                        oldPetId = entity.id,
                        newPetId = created.id
                    )

                    // Remove the local pet after children moved to the server pet id.
                    db.petDao().deletePetById(entity.id)

                    android.util.Log.d("SYNC_WORKER", "Pet synced: ${entity.id} → ${created.id}")
                } else {
                    android.util.Log.e("SYNC_WORKER", "Null body, code=${response.code()}, error=${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SYNC_WORKER", "Exception: ${e::class.simpleName} — ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun uploadLocalPhotoIfNeeded(url: String?): String? {
        if (url == null) return null
        if (!url.startsWith("file")) return url   // already a remote URL, nothing to do

        return try {
            val uri = url.toUri()
            val filename = "pets/${java.util.UUID.randomUUID()}.jpg"
            val ref = Firebase.storage.reference.child(filename)
            ref.putFile(uri).await()
            val bucket = ref.bucket
            val encodedPath = filename.replace("/", "%2F")
            "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedPath?alt=media"
                .also { android.util.Log.d("SYNC_WORKER", "Photo uploaded → $it") }
        } catch (e: Exception) {
            android.util.Log.e("SYNC_WORKER", "Photo upload failed: ${e.message}")
            null   // pet syncs without a photo rather than blocking the whole sync
        }
    }

    private fun deleteLocalPhotoFile(url: String?) {
        if (url == null || !url.startsWith("file://")) return
        try {
            val file = java.io.File(android.net.Uri.parse(url).path ?: return)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    // Pets edited offline — PATCH to server
    private suspend fun syncPendingUpdates(db: AppDatabase, api: ApiService) {
        val pending = db.petDao().getPendingSync()
            .filter { !it.id.startsWith("local_") }   // real ID = already exists on server

        pending.forEach { entity ->
            try {
                (api).updatePet(
                    entity.id,
                    UpdatePetRequest(
                        name           = entity.name,
                        species        = entity.species,
                        breed          = entity.breed,
                        gender         = entity.gender,
                        weight         = entity.weight,
                        color          = entity.color,
                        birthDate      = entity.birthDate,
                        photoUrl       = entity.photoUrl,
                        status         = entity.status,
                        isNfcSynced    = entity.isNfcSynced,
                        knownAllergies = entity.knownAllergies,
                        defaultVet     = entity.defaultVet,
                        defaultClinic  = entity.defaultClinic
                    )
                )
                db.petDao().updatePet(entity.copy(pendingSync = false))
            } catch (e: Exception) {
                // Skip — retry next time
            }
        }
    }

    // Pets deleted offline — DELETE on server then remove from Room
    private suspend fun syncPendingDeletes(db: AppDatabase, api: ApiService) {
        val pending = db.petDao().getPendingDelete()

        pending.forEach { entity ->
            try {
                val response = (api)
                    .deletePet(entity.id)
                if (response.isSuccessful || response.code() == 204) {
                    db.petDao().deletePetById(entity.id)
                }
            } catch (e: Exception) {
                // Skip — retry next time
            }
        }
    }

    private suspend fun syncPendingVaccinationCreates(db: AppDatabase, api: ApiService) {
        val allPending = db.vaccineDao().getPendingSync()
        val pending = allPending
            .filter { it.id.startsWith("local_vax_") }
            // Skip any whose petId is still a temp — pet must sync first
            .filter { !it.petId.startsWith("local_") }

        android.util.Log.d(
            "VAX_SYNC",
            "Pending vaccination creates total=${allPending.count { it.id.startsWith("local_vax_") }} eligible=${pending.size}"
        )
        pending.forEach { entity ->
            try {
                android.util.Log.d(
                    "VAX_SYNC",
                    "POST vaccination create localId=${entity.id} petId=${entity.petId}"
                )
                val response = api.addVaccination(
                    entity.petId,
                    AddVaccinationRequest(
                        vaccineId = entity.vaccineId,
                        dateGiven = entity.dateGiven,
                        nextDueDate = entity.nextDueDate,
                        lotNumber = entity.lotNumber,
                        status = entity.status,
                        administeredBy = entity.administeredBy
                    )
                )
                android.util.Log.d("VAX_SYNC", "POST vaccination create response=${response.code()} localId=${entity.id}")
                val pet = response.body() ?: run {
                    android.util.Log.w("VAX_SYNC", "POST vaccination create null body localId=${entity.id}")
                    return@forEach
                }
                val createdVaccination = pet.vaccinations.lastOrNull()
                if (createdVaccination?.id?.isNotBlank() == true) {
                    movePendingDocumentsToServerVaccination(
                        oldPetId = entity.petId,
                        oldVaccinationId = entity.id,
                        newPetId = entity.petId,
                        newVaccinationId = createdVaccination.id
                    )
                } else {
                    android.util.Log.w("DOC_UPLOAD", "Could not map pending docs for local vaccination ${entity.id}")
                }
                // Remove the local vaccination we just synced
                db.vaccineDao().deleteVaccineById(entity.id)
                // Insert all vaccinations from server (will update/replace other remote ones)
                val serverEntities = pet.vaccinations.map { it.toEntity(entity.petId) }
                db.vaccineDao().insertAll(serverEntities)
                android.util.Log.d(
                    "VAX_SYNC",
                    "Vaccination create synced localId=${entity.id} serverVaccinations=${serverEntities.size}"
                )
            } catch (e: Exception) {
                android.util.Log.e("VAX_SYNC", "Vaccination create sync failed id=${entity.id}: ${e.message}", e)
            }
        }
    }

    private suspend fun syncPendingVaccinationUpdates(db: AppDatabase, api: ApiService) {
        val pending = db.vaccineDao().getPendingSync()
            .filter { !it.id.startsWith("local_vax_") }
            .filter { !it.petId.startsWith("local_") }
        android.util.Log.d("VAX_SYNC", "Pending vaccination updates eligible=${pending.size}")
        pending.forEach { entity ->
            try {
                android.util.Log.d("VAX_SYNC", "PUT vaccination update id=${entity.id} petId=${entity.petId}")
                val response = api.updateVaccination(
                    entity.petId,
                    entity.id,
                    UpdateVaccinationRequest(
                        vaccineId = entity.vaccineId,
                        dateGiven = entity.dateGiven,
                        nextDueDate = entity.nextDueDate,
                        lotNumber = entity.lotNumber,
                        administeredBy = entity.administeredBy
                    )
                )
                android.util.Log.d("VAX_SYNC", "PUT vaccination update response=${response.code()} id=${entity.id}")
                db.vaccineDao().updateVaccine(entity.copy(pendingSync = false))
            } catch (e: Exception) {
                android.util.Log.e("VAX_SYNC", "Vaccination update sync failed id=${entity.id}: ${e.message}", e)
            }
        }
    }

    private suspend fun syncPendingVaccinationDeletes(db: AppDatabase, api: ApiService) {
        val pending = db.vaccineDao().getPendingDelete()
            .filter { !it.petId.startsWith("local_") }
        android.util.Log.d("VAX_SYNC", "Pending vaccination deletes eligible=${pending.size}")
        pending.forEach { entity ->
            try {
                android.util.Log.d("VAX_SYNC", "DELETE vaccination id=${entity.id} petId=${entity.petId}")
                val response = api.deleteVaccination(entity.petId, entity.id)
                android.util.Log.d("VAX_SYNC", "DELETE vaccination response=${response.code()} id=${entity.id}")
                if (response.isSuccessful || response.code() == 204) {
                    db.vaccineDao().deleteVaccineById(entity.id)
                    android.util.Log.d("VAX_SYNC", "Vaccination delete synced id=${entity.id}")
                } else {
                    android.util.Log.w("VAX_SYNC", "Vaccination delete rejected id=${entity.id} http=${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("VAX_SYNC", "Vaccination delete sync failed id=${entity.id}: ${e.message}", e)
            }
        }
    }

    private suspend fun syncPendingVaccinationDocuments(api: ApiService) {
        val hive = HiveCacheManager(applicationContext)
        val gson = Gson()
        val pendingByKey = hive.getAllPendingVaccinationDocumentJson()
        android.util.Log.d("DOC_UPLOAD", "Worker pending vaccination documents groups=${pendingByKey.size}")

        pendingByKey.forEach { (key, json) ->
            val pendingDocs = runCatching {
                gson.fromJson(json, Array<PendingVaccinationDocument>::class.java).toList()
            }.getOrElse {
                android.util.Log.e("DOC_UPLOAD", "Worker failed to parse pending docs key=$key: ${it.message}", it)
                emptyList()
            }
            if (pendingDocs.isEmpty()) return@forEach

            val remaining = pendingDocs.toMutableList()
            pendingDocs.forEach { pending ->
                try {
                    val safeFileName = pending.fileName.replace(Regex("""[^\w.\-]"""), "_")
                    val path = "pets/${pending.petId}/documents/vaccinations/${pending.vaccinationId}/${java.util.UUID.randomUUID()}_$safeFileName"
                    val ref = Firebase.storage.reference.child(path)
                    android.util.Log.d(
                        "DOC_UPLOAD",
                        "Worker Firebase upload pendingDoc=${pending.id} path=$path"
                    )
                    ref.putFile(pending.localUri.toUri()).await()
                    val downloadUrl = ref.downloadUrl.await().toString()
                    val response = api.addVaccinationDocument(
                        pending.petId,
                        pending.vaccinationId,
                        AddDocumentRequest(
                            fileName = pending.fileName,
                            fileUri = downloadUrl
                        )
                    )
                    if (!response.isSuccessful || response.body() == null) {
                        android.util.Log.w(
                            "DOC_UPLOAD",
                            "Worker backend document rejected pendingDoc=${pending.id} http=${response.code()}"
                        )
                        return@forEach
                    }

                    remaining.remove(pending)
                    deleteLocalPendingDocument(pending.localUri)
                    com.example.petcare.data.repository.RepositoryProvider.petRepository
                        .invalidatePetLru(pending.petId)
                    android.util.Log.d("DOC_UPLOAD", "Worker synced pending document id=${pending.id}")
                } catch (e: Exception) {
                    android.util.Log.e("DOC_UPLOAD", "Worker pending document sync failed id=${pending.id}: ${e.message}", e)
                }
            }

            if (remaining.isEmpty()) {
                hive.invalidatePendingVaccinationDocumentsByKey(key)
            } else {
                hive.putPendingVaccinationDocumentsByKey(key, gson.toJson(remaining))
            }
        }
    }

    private fun movePendingDocumentsToServerVaccination(
        oldPetId: String,
        oldVaccinationId: String,
        newPetId: String,
        newVaccinationId: String
    ) {
        val hive = HiveCacheManager(applicationContext)
        val gson = Gson()
        hive.movePendingVaccinationDocuments(
            oldPetId = oldPetId,
            oldVaccinationId = oldVaccinationId,
            newPetId = newPetId,
            newVaccinationId = newVaccinationId
        ) { json ->
            val updated = gson.fromJson(json, Array<PendingVaccinationDocument>::class.java)
                .map {
                    it.copy(
                        petId = newPetId,
                        vaccinationId = newVaccinationId
                    )
                }
            gson.toJson(updated)
        }
        android.util.Log.d(
            "DOC_UPLOAD",
            "Moved pending docs from vaccination=$oldVaccinationId to server vaccination=$newVaccinationId"
        )
    }

    private fun deleteLocalPendingDocument(localUri: String) {
        runCatching {
            val path = android.net.Uri.parse(localUri).path ?: return
            java.io.File(path).delete()
        }
    }

    private suspend fun syncPendingWeightLogCreates(db: AppDatabase, api: ApiService) {
        db.weightLogDao().getPendingSync()
            .filter { it.id.startsWith("local_weight_") }
            .filter { !it.petId.startsWith("local_") }
            .forEach { entity ->
                try {
                    val response = api.createWeightLog(
                        entity.petId,
                        CreateWeightLogRequest(
                            weight = entity.weight,
                            loggedAt = entity.loggedAt,
                            clientMutationId = entity.clientMutationId ?: entity.id
                        )
                    )
                    val created = response.body() ?: return@forEach
                    db.weightLogDao().deleteById(entity.id)
                    db.weightLogDao().insert(created.toEntity())
                } catch (e: Exception) { /* retry next worker run */ }
            }
    }

    private suspend fun syncPendingWeightLogUpdates(db: AppDatabase, api: ApiService) {
        db.weightLogDao().getPendingSync()
            .filter { !it.id.startsWith("local_weight_") }
            .filter { !it.petId.startsWith("local_") }
            .forEach { entity ->
                try {
                    val response = api.updateWeightLog(
                        entity.petId,
                        entity.id,
                        UpdateWeightLogRequest(
                            weight = entity.weight,
                            loggedAt = entity.loggedAt
                        )
                    )
                    val updated = response.body()
                    if (updated != null) {
                        db.weightLogDao().insert(updated.toEntity())
                    } else {
                        db.weightLogDao().update(entity.copy(pendingSync = false))
                    }
                } catch (e: Exception) { /* retry next worker run */ }
            }
    }

    private suspend fun syncPendingWeightLogDeletes(db: AppDatabase, api: ApiService) {
        db.weightLogDao().getPendingDelete()
            .filter { !it.petId.startsWith("local_") }
            .forEach { entity ->
                try {
                    val response = api.deleteWeightLog(entity.petId, entity.id)
                    if (response.isSuccessful || response.code() == 204) {
                        db.weightLogDao().deleteById(entity.id)
                    }
                } catch (e: Exception) { /* retry next worker run */ }
            }
    }



    private fun movePendingDocumentsToServerPet(
        oldPetId: String,
        newPetId: String
    ) {
        val hive = HiveCacheManager(applicationContext)
        val gson = Gson()
        hive.getAllPendingEventDocumentJson().forEach { (_, json) ->
            val docs = runCatching {
                gson.fromJson(json, Array<PendingEventDocument>::class.java).toList()
            }.getOrElse {
                android.util.Log.e("EVENT_DOC_UPLOAD", "Failed to inspect pending event docs during pet remap: ${it.message}", it)
                emptyList()
            }
            docs.firstOrNull { it.petId == oldPetId }?.let { first ->
                hive.movePendingEventDocuments(
                    oldPetId = oldPetId,
                    oldEventId = first.eventId,
                    newPetId = newPetId,
                    newEventId = first.eventId
                ) { pendingJson ->
                    val updated = gson.fromJson(pendingJson, Array<PendingEventDocument>::class.java)
                        .map { pending -> pending.copy(petId = newPetId) }
                    gson.toJson(updated)
                }
                android.util.Log.d(
                    "EVENT_DOC_UPLOAD",
                    "Moved pending event docs from pet=$oldPetId to server pet=$newPetId event=${first.eventId}"
                )
            }
        }
    }

    private suspend fun syncPendingEventCreates(db: AppDatabase, api: ApiService) {
        val all = db.eventDao().getAllSync()
        android.util.Log.d("EVENT_SYNC", "ALL events in Room: ${all.size}")
        all.forEach {
            android.util.Log.d("EVENT_SYNC", "  id=${it.id} op=${it.pendingOperation} synced=${it.synced} pendingDelete=${it.pendingDelete}")
        }

        val pending = db.eventDao().getPendingCreatesForMerge()
            .filter { it.id.startsWith("local_ev_") }
            .filter { !it.petId.startsWith("local_") }
        android.util.Log.d("EVENT_SYNC", "getPendingCreatesForMerge() returned: ${pending.size}")
        pending.forEach { entity ->
            try {
                val response = api.createEvent(
                    CreateEventRequest(
                        petId = entity.petId,
                        ownerId = entity.ownerId,
                        title = entity.title,
                        eventType = entity.eventType.lowercase(),
                        date = entity.date,
                        price = entity.price,
                        provider = entity.provider,
                        clinic = entity.clinic,
                        description = entity.description,
                        followUpDate = entity.followUpDate
                    )
                )

                val created = response.body() ?: return@forEach

                val createdEntity = runCatching { created.toEntity() }.getOrElse {
                    android.util.Log.e("EVENT_SYNC", "toEntity failed for ${created.id}: ${it.message}")
                    // Construye la entidad manualmente desde la que ya teníamos
                    entity.copy(
                        id = created.id,
                        synced = true,
                        pendingOperation = null
                    )
                }

                db.eventDao().deleteById(entity.id)
                db.eventDao().upsert(createdEntity)

                movePendingDocumentsToServerEvent(
                    oldEventId = entity.id,
                    newEventId = created.id,
                    petId = entity.petId
                )

                // Invalida ambos cachés para forzar recarga desde servidor
                HiveCacheManager(applicationContext).invalidateEvents(entity.petId)
                com.example.petcare.data.repository.RepositoryProvider.petRepository
                    .invalidatePetLru(entity.petId)

            } catch (e: java.net.ConnectException) {
                // Error de red — relanzar para que el worker haga Result.retry()
                android.util.Log.e("EVENT_SYNC", "Network error syncing ${entity.id}, will retry: ${e.message}")
                throw e  // ← el coroutineScope externo lo captura y devuelve Result.retry()
            } catch (e: Exception) {
                android.util.Log.e("EVENT_SYNC", "Create failed ${entity.id}: ${e.message}")
            }
        }
    }

    private fun movePendingDocumentsToServerEvent(
        oldEventId: String,
        newEventId: String,
        petId: String
    ) {
        val hive = HiveCacheManager(applicationContext)
        val gson = Gson()

        hive.movePendingEventDocuments(
            oldPetId = petId,
            oldEventId = oldEventId,
            newPetId = petId,
            newEventId = newEventId
        ) { json ->

            val updated = gson.fromJson(json, Array<PendingEventDocument>::class.java)
                .map { pending ->
                    pending.copy(
                        eventId = newEventId,
                        petId = petId // redundante pero seguro
                    )
                }

            gson.toJson(updated)
        }

        android.util.Log.d(
            "EVENT_DOC_UPLOAD",
            "Moved pending docs from event=$oldEventId to server event=$newEventId"
        )
    }

    private suspend fun syncPendingEventUpdates(db: AppDatabase, api: ApiService) {
        val pending = db.eventDao().getAllSync()
            .filter { it.pendingOperation == "UPDATE" }
            .filter { !it.id.startsWith("local_ev_") }

        pending.forEach { entity ->
            try {
                api.updateEvent(
                    entity.id,
                    mapOf(
                        "title" to entity.title,
                        "description" to entity.description,
                        "provider" to entity.provider,
                        "clinic" to entity.clinic,
                        "date" to entity.date,
                        "price" to entity.price
                    )
                )

                db.eventDao().markSynced(entity.id)

            } catch (e: Exception) {
                android.util.Log.e("EVENT_SYNC", "Update failed ${entity.id}: ${e.message}")
            }
        }
    }

    private suspend fun syncPendingEventDeletes(db: AppDatabase, api: ApiService) {
        db.eventDao().getPendingDeletesForMerge()
            .forEach { entity ->
                try {
                    val response = api.deleteEvent(entity.id)
                    if (response.isSuccessful || response.code() == 204) {
                        db.eventDao().deleteById(entity.id)
                    }
                } catch (e: Exception) { }
            }
    }




}
