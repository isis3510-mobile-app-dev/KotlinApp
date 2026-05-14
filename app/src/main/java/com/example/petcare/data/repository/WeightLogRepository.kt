package com.example.petcare.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.petcare.data.local.dao.WeightLogDao
import com.example.petcare.data.local.entity.WeightLogEntity
import com.example.petcare.data.local.mapper.toEntity
import com.example.petcare.data.local.mapper.toWeightLog
import com.example.petcare.data.model.CreateWeightLogRequest
import com.example.petcare.data.model.UpdateWeightLogRequest
import com.example.petcare.data.model.WeightLog
import com.example.petcare.data.network.ApiService
import com.example.petcare.data.worker.SyncWorker
import com.google.firebase.auth.FirebaseAuth

class WeightLogRepository(
    private val weightLogDao: WeightLogDao,
    private val api: ApiService,
    private val context: Context
) {
    private fun currentUserId(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
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
            SyncWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    suspend fun getWeightLogs(petId: String): Result<List<WeightLog>> {
        return if (isOnline()) {
            runCatching {
                val response = api.getWeightLogs(petId)
                if (!response.isSuccessful) error("Failed to load weight logs — HTTP ${response.code()}")
                val logs = response.body().orEmpty()
                weightLogDao.insertAll(logs.map { it.toEntity() })
                logs
            }.recoverCatching {
                weightLogDao.getForPetSync(petId).map { it.toWeightLog() }
            }
        } else {
            runCatching {
                weightLogDao.getForPetSync(petId).map { it.toWeightLog() }
            }
        }
    }

    suspend fun createWeightLog(petId: String, request: CreateWeightLogRequest): Result<WeightLog> {
        val uid = currentUserId()
        return if (isOnline()) {
            runCatching {
                val response = api.createWeightLog(petId, request)
                if (!response.isSuccessful) error("Failed to save weight log — HTTP ${response.code()}")
                val log = response.body() ?: error("Failed to save weight log")
                weightLogDao.insert(log.toEntity())
                log
            }.recoverCatching {
                val mutationId = request.clientMutationId ?: "weight_${System.currentTimeMillis()}"
                val tempId = "local_weight_$mutationId"
                val entity = WeightLogEntity(
                    id = tempId,
                    petId = petId,
                    ownerId = uid,
                    weight = request.weight,
                    loggedAt = request.loggedAt,
                    clientMutationId = mutationId,
                    pendingSync = true,
                    pendingDelete = false
                )
                weightLogDao.insert(entity)
                enqueueSyncWork()
                entity.toWeightLog()
            }
        } else {
            runCatching {
                val mutationId = request.clientMutationId ?: "weight_${System.currentTimeMillis()}"
                val tempId = "local_weight_$mutationId"
                val entity = WeightLogEntity(
                    id = tempId,
                    petId = petId,
                    ownerId = uid,
                    weight = request.weight,
                    loggedAt = request.loggedAt,
                    clientMutationId = mutationId,
                    pendingSync = true,
                    pendingDelete = false
                )
                weightLogDao.insert(entity)
                enqueueSyncWork()
                entity.toWeightLog()
            }
        }
    }

    suspend fun updateWeightLog(
        petId: String,
        weightLogId: String,
        request: UpdateWeightLogRequest
    ): Result<WeightLog> {
        return if (isOnline() && !weightLogId.startsWith("local_weight_")) {
            runCatching {
                val response = api.updateWeightLog(petId, weightLogId, request)
                if (!response.isSuccessful) error("Failed to update weight log — HTTP ${response.code()}")
                val log = response.body() ?: error("Failed to update weight log")
                weightLogDao.insert(log.toEntity())
                log
            }
        } else {
            runCatching {
                val existing = weightLogDao.getById(weightLogId) ?: error("Weight log not found offline")
                val updated = existing.copy(
                    weight = request.weight ?: existing.weight,
                    loggedAt = request.loggedAt ?: existing.loggedAt,
                    pendingSync = true
                )
                weightLogDao.update(updated)
                enqueueSyncWork()
                updated.toWeightLog()
            }
        }
    }

    suspend fun deleteWeightLog(petId: String, weightLogId: String): Result<Unit> {
        return if (isOnline() && !weightLogId.startsWith("local_weight_")) {
            try {
                val response = api.deleteWeightLog(petId, weightLogId)
                if (response.isSuccessful || response.code() == 204) {
                    weightLogDao.deleteById(weightLogId)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete weight log — HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                if (e.message.orEmpty().contains("204") && e.message.orEmpty().contains("Content-Length")) {
                    weightLogDao.deleteById(weightLogId)
                    Result.success(Unit)
                } else {
                    Result.failure(e)
                }
            }
        } else {
            runCatching {
                if (weightLogId.startsWith("local_weight_")) {
                    weightLogDao.deleteById(weightLogId)
                } else {
                    val existing = weightLogDao.getById(weightLogId) ?: error("Weight log not found offline")
                    weightLogDao.update(existing.copy(pendingDelete = true, pendingSync = false))
                    enqueueSyncWork()
                }
            }
        }
    }
}
