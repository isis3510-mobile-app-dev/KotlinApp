package com.example.petcare.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.lru.UserLruCache
import com.example.petcare.data.model.UpdateUserRequest
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class UserSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "user_sync_work"
        private const val TIMEOUT_MS = 50_000L
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("USER_SYNC", "Worker started thread=${Thread.currentThread().name}")
        val db = AppDatabase.getInstance(applicationContext)
        val api = RepositoryProvider.apiService
        val userDao = db.userDao()
        val cache = RepositoryProvider.userCache

        return@withContext try {
            val pendingUsers = userDao.getPendingSync()
            Log.d("USER_SYNC", "Pending user updates: ${pendingUsers.size}")

            if (pendingUsers.isEmpty()) {
                Log.d("USER_SYNC", "No pending user updates")
                return@withContext Result.success()
            }

            for (user in pendingUsers) {
                Log.d("USER_SYNC", "Uploading user update userId=${user.id}")
                withTimeout(TIMEOUT_MS) {
                    val request = UpdateUserRequest(
                        name = user.name,
                        phone = user.phone
                    )

                    val response = api.updateMe(request)

                    if (!response.isSuccessful) {
                        Log.e("USER_SYNC", "Server rejected update: ${response.code()}")
                        return@withTimeout Result.retry()
                    }

                    userDao.insert(user.copy(pendingSync = false))
                    cache.invalidate(UserLruCache.CACHE_KEY_ME)

                    Log.d("USER_SYNC", "User synced successfully")
                }
            }

            Result.success()

        } catch (e: Exception) {
            Log.e("USER_SYNC", "Error syncing user", e)
            Result.retry()
        }
    }
}
