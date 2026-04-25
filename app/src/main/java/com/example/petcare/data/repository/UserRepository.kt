package com.example.petcare.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.petcare.data.local.dao.UserDao
import com.example.petcare.data.local.lru.UserLruCache
import com.example.petcare.data.local.lru.UserLruCache.Companion.CACHE_KEY_ME
import com.example.petcare.data.local.mapper.toDomain
import com.example.petcare.data.local.mapper.toEntity
import com.example.petcare.data.model.UpdateUserRequest
import com.example.petcare.data.model.User
import com.example.petcare.data.network.ApiService
import com.example.petcare.data.network.isOnline
import com.example.petcare.data.worker.UserSyncWorker

class UserRepository(
    private val api: ApiService,
    private val dao: UserDao,
    private val context: Context,
    private val cache: UserLruCache = UserLruCache()
) {

    suspend fun getMe(): Result<User> = runCatching {
        cache.get(CACHE_KEY_ME)?.let {
            Log.d("CACHE", "L1 HIT — getUser $CACHE_KEY_ME")
            return@runCatching it
        }

        if (isOnline(context)) {
            val user = api.getMe().body() ?: error("Empty response")
            dao.insert(user.toEntity())
            cache.put(CACHE_KEY_ME, user)
            return@runCatching user
        }

        // 3. Room fallback (offline)
        val local = dao.getUser() ?: error("No offline data available")
        val user = local.toDomain()
        cache.put(CACHE_KEY_ME, user)
        return@runCatching user
    }

    suspend fun updateMe(request: UpdateUserRequest): Result<User> = runCatching {
        val current = dao.getUser() ?: error("No local user")

        return@runCatching if (isOnline(context)) {
            try {
                val user = api.updateMe(request).body() ?: error("Empty response")
                dao.insert(user.toEntity().copy(pendingSync = false))
                cache.put(CACHE_KEY_ME, user)   // update cache with fresh server data
                user
            } catch (e: Exception) {
                val updated = saveLocalPending(current, request)
                cache.put(CACHE_KEY_ME, updated) // keep cache consistent with local state
                enqueueUserSync()
                updated
            }
        } else {
            val updated = saveLocalPending(current, request)
            cache.put(CACHE_KEY_ME, updated)    // reflect offline edit immediately
            enqueueUserSync()
            updated
        }
    }

    suspend fun deleteMe(): Result<Unit> = runCatching {
        if (!isOnline(context)) error("No internet connection")
        api.deleteMe()
        dao.clear()
        cache.clear()
    }

    // Applies only the non-null fields from the request, marks as pending sync,
    // persists, and returns the updated domain object.
    private suspend fun saveLocalPending(
        current: com.example.petcare.data.local.entity.UserEntity,
        request: UpdateUserRequest
    ): User {
        val updated = current.copy(
            name        = request.name  ?: current.name,
            phone       = request.phone ?: current.phone,
            pendingSync = true
        )
        dao.insert(updated)
        return updated.toDomain()
    }

    private fun enqueueUserSync() {
        val work = OneTimeWorkRequestBuilder<UserSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UserSyncWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            work
        )
    }
}