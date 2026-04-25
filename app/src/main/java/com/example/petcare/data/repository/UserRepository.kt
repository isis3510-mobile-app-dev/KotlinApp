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
        val online = isOnline(context)
        Log.d(TAG, "getMe start online=$online thread=${Thread.currentThread().name}")

        cache.get(CACHE_KEY_ME)?.let {
            Log.d(TAG, "getMe served from USER_LRU_CACHE key=$CACHE_KEY_ME")
            return@runCatching it
        }

        if (online) {
            Log.d(TAG, "getMe USER_LRU_CACHE miss -> API users/me")
            val user = api.getMe().body() ?: error("Empty response")
            dao.insert(user.toEntity())
            Log.d(TAG, "getMe API success -> Room upsert userId=${user.id}")
            cache.put(CACHE_KEY_ME, user)
            return@runCatching user
        }

        // 3. Room fallback (offline)
        Log.d(TAG, "getMe offline -> Room fallback")
        val local = dao.getUser() ?: error("No offline data available")
        val user = local.toDomain()
        Log.d(TAG, "getMe Room fallback success userId=${user.id} pendingSync=${local.pendingSync}")
        cache.put(CACHE_KEY_ME, user)
        return@runCatching user
    }

    suspend fun updateMe(request: UpdateUserRequest): Result<User> = runCatching {
        val current = dao.getUser() ?: error("No local user")
        val online = isOnline(context)
        Log.d(
            TAG,
            "updateMe start online=$online fields name=${request.name != null} phone=${request.phone != null} address=${request.address != null}"
        )

        return@runCatching if (online) {
            try {
                val user = api.updateMe(request).body() ?: error("Empty response")
                dao.insert(user.toEntity().copy(pendingSync = false))
                Log.d(TAG, "updateMe API success -> Room pendingSync=false userId=${user.id}")
                cache.put(CACHE_KEY_ME, user)   // update cache with fresh server data
                user
            } catch (e: Exception) {
                Log.w(TAG, "updateMe API failed while online -> save local pending: ${e.message}")
                val updated = saveLocalPending(current, request)
                cache.put(CACHE_KEY_ME, updated) // keep cache consistent with local state
                enqueueUserSync()
                updated
            }
        } else {
            Log.d(TAG, "updateMe offline -> Room pending update + WorkManager")
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
        Log.d(TAG, "Room pending profile saved userId=${updated.id} pendingSync=${updated.pendingSync}")
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
        Log.d(TAG, "Queued USER_SYNC WorkManager uniqueWork=${UserSyncWorker.UNIQUE_WORK_NAME}")
    }

    private companion object {
        const val TAG = "USER_PROFILE"
    }
}
