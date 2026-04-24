package com.example.petcare.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.hive.HiveCacheManager
import com.example.petcare.data.local.mapper.toEntity
import com.example.petcare.data.model.AddVaccinationRequest
import com.example.petcare.data.model.CreatePetRequest
import com.example.petcare.data.model.CreateWeightLogRequest
import com.example.petcare.data.model.UpdatePetRequest
import com.example.petcare.data.model.UpdateVaccinationRequest
import com.example.petcare.data.model.UpdateWeightLogRequest
import com.example.petcare.data.network.ApiService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "pet_sync"
    }

    override suspend fun doWork(): Result {
        android.util.Log.d("SYNC_WORKER", "doWork started")
        kotlinx.coroutines.delay(1500)

        val db = AppDatabase.getInstance(applicationContext)

        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        android.util.Log.d("SYNC_WORKER", "Current Firebase user: ${user?.uid ?: "NULL"}")
        if (user == null) return Result.retry()

        // Fetch token once using coroutine-native approach — avoids runBlocking deadlock
        val token = try {
            user.getIdToken(true).await().token
        } catch (e: Exception) {
            android.util.Log.e("SYNC_WORKER", "Token fetch failed: ${e.message}")
            null
        }

        if (token.isNullOrBlank()) {
            android.util.Log.e("SYNC_WORKER", "No token — retrying later")
            return Result.retry()
        }

        android.util.Log.d("SYNC_WORKER", "Token fetched: ${token.take(20)}...")

        // Build a one-off API client with the token baked in — no interceptor needed
        val api = buildAuthenticatedApi(token)

        return try {
            syncPendingCreates(db, api)
            syncPendingUpdates(db, api)
            syncPendingDeletes(db, api)
            syncPendingVaccinationCreates(db, api)
            syncPendingVaccinationUpdates(db, api)
            syncPendingVaccinationDeletes(db, api)
            syncPendingWeightLogCreates(db, api)
            syncPendingWeightLogUpdates(db, api)
            syncPendingWeightLogDeletes(db, api)
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

        // Get a fresh Firebase token before making any requests
        val token = suspendCancellableCoroutine<String?> { cont ->
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user == null) {
                android.util.Log.e("SYNC_WORKER", "No Firebase user for token fetch")
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            user.getIdToken(true)
                .addOnSuccessListener { result ->
                    android.util.Log.d("SYNC_WORKER", "Token fetched: ${result.token?.take(20)}...")
                    cont.resume(result.token)
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("SYNC_WORKER", "Token fetch failed: ${e.message}")
                    cont.resume(null)
                }
        }

        if (token == null) {
            android.util.Log.e("SYNC_WORKER", "Cannot sync — no auth token")
            return
        }

        pending.forEach { entity ->
            android.util.Log.d("SYNC_WORKER", "Uploading pet: ${entity.name}, id=${entity.id}")
            try {
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
                        photoUrl       = entity.photoUrl,
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
                            owner = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: entity.owner,
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

}
