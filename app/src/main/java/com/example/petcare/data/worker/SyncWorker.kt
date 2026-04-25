package com.example.petcare.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.hive.HiveCacheManager
import com.example.petcare.data.local.mapper.toEntity
import com.example.petcare.data.model.AddVaccinationRequest
import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.CreatePetRequest
import com.example.petcare.data.model.CreateWeightLogRequest
import com.example.petcare.data.model.UpdatePetRequest
import com.example.petcare.data.model.UpdateVaccinationRequest
import com.example.petcare.data.model.UpdateWeightLogRequest
import com.example.petcare.data.network.ApiService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.storage
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
        private const val EVENT_MAX_RETRY = 6
        private const val EVENT_BASE_RETRY_MS = 2_000L
        private const val EVENT_MAX_RETRY_MS = 5 * 60_000L
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
            val eventSyncHadFailures = syncPendingEventOperations(db, api)

            coroutineScope {
                launch(Dispatchers.IO + CoroutineName("vaccination-sync")) {
                    syncPendingVaccinationCreates(db, api)
                    syncPendingVaccinationUpdates(db, api)
                    syncPendingVaccinationDeletes(db, api)
                }
                launch(Dispatchers.IO + CoroutineName("weight-sync")) {
                    syncPendingWeightLogCreates(db, api)
                    syncPendingWeightLogUpdates(db, api)
                    syncPendingWeightLogDeletes(db, api)
                }
            }
            HiveCacheManager(applicationContext).invalidatePets(user.uid)
            if (eventSyncHadFailures) Result.retry() else Result.success()
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
        db.vaccineDao().getPendingSync()
            .filter { it.id.startsWith("local_vax_") }
            // Skip any whose petId is still a temp — pet must sync first
            .filter { !it.petId.startsWith("local_") }
            .forEach { entity ->
                try {
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
                    val pet = response.body() ?: return@forEach
                    // Remove the local vaccination we just synced
                    db.vaccineDao().deleteVaccineById(entity.id)
                    // Insert all vaccinations from server (will update/replace other remote ones)
                    val serverEntities = pet.vaccinations.map { it.toEntity(entity.petId) }
                    db.vaccineDao().insertAll(serverEntities)
                } catch (e: Exception) { /* skip */ }
            }
    }

    private suspend fun syncPendingVaccinationUpdates(db: AppDatabase, api: ApiService) {
        db.vaccineDao().getPendingSync()
            .filter { !it.id.startsWith("local_vax_") }
            .filter { !it.petId.startsWith("local_") }
            .forEach { entity ->
                try {
                    api.updateVaccination(
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
                    db.vaccineDao().updateVaccine(entity.copy(pendingSync = false))
                } catch (e: Exception) { /* skip */ }
            }
    }

    private suspend fun syncPendingVaccinationDeletes(db: AppDatabase, api: ApiService) {
        db.vaccineDao().getPendingDelete()
            .filter { !it.petId.startsWith("local_") }
            .forEach { entity ->
                try {
                    val response = api.deleteVaccination(entity.petId, entity.id)
                    if (response.isSuccessful || response.code() == 204) {
                        db.vaccineDao().deleteVaccineById(entity.id)
                    }
                } catch (e: Exception) { /* skip */ }
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

    private suspend fun syncPendingEventOperations(db: AppDatabase, api: ApiService): Boolean {
        val nowMs = System.currentTimeMillis()
        var hadFailures = false

        db.eventDao().getPendingCreatesForSync(nowMs).forEach { entity ->
            try {
                if (entity.petId.startsWith("local_")) {
                    scheduleEventCreateRetry(db, entity.id, entity.retryCount)
                    hadFailures = true
                    return@forEach
                }

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

                val created = response.body()
                if (response.isSuccessful && created != null) {
                    db.eventDao().deleteById(entity.id)
                    db.eventDao().upsert(created.toEntity())
                    HiveCacheManager(applicationContext).invalidateEvents(entity.petId)
                } else {
                    scheduleEventCreateRetry(db, entity.id, entity.retryCount)
                    hadFailures = true
                }
            } catch (_: Exception) {
                scheduleEventCreateRetry(db, entity.id, entity.retryCount)
                hadFailures = true
            }
        }

        db.eventDao().getPendingDeletesForSync(nowMs).forEach { entity ->
            try {
                if (entity.id.startsWith("local_ev_")) {
                    db.eventDao().deleteById(entity.id)
                    return@forEach
                }

                val response = api.deleteEvent(entity.id)
                if (response.isSuccessful || response.code() == 204 || response.code() == 404) {
                    db.eventDao().deleteById(entity.id)
                    HiveCacheManager(applicationContext).invalidateEvents(entity.petId)
                } else {
                    scheduleEventDeleteRetry(db, entity.id, entity.retryCount)
                    hadFailures = true
                }
            } catch (_: Exception) {
                scheduleEventDeleteRetry(db, entity.id, entity.retryCount)
                hadFailures = true
            }
        }

        return hadFailures
    }

    private suspend fun scheduleEventCreateRetry(db: AppDatabase, eventId: String, currentRetry: Int) {
        val nextRetry = (currentRetry + 1).coerceAtMost(EVENT_MAX_RETRY)
        val nextRetryAt = System.currentTimeMillis() + retryDelay(nextRetry)
        db.eventDao().markCreateSyncFailed(eventId, nextRetry, nextRetryAt)
    }

    private suspend fun scheduleEventDeleteRetry(db: AppDatabase, eventId: String, currentRetry: Int) {
        val nextRetry = (currentRetry + 1).coerceAtMost(EVENT_MAX_RETRY)
        val nextRetryAt = System.currentTimeMillis() + retryDelay(nextRetry)
        db.eventDao().markDeleteSyncFailed(eventId, nextRetry, nextRetryAt)
    }

    private fun retryDelay(retryCount: Int): Long {
        val exponential = EVENT_BASE_RETRY_MS * (1L shl retryCount.coerceIn(0, 12))
        return exponential.coerceAtMost(EVENT_MAX_RETRY_MS)
    }

}
