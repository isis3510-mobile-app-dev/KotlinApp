package com.example.petcare.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.lru.UserLruCache
import com.example.petcare.data.model.UpdateUserRequest
import com.example.petcare.data.repository.RepositoryProvider

class UserSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "user_sync_work"
    }

    override suspend fun doWork(): Result {
        val db      = AppDatabase.getInstance(applicationContext)
        val api     = RepositoryProvider.apiService
        val userDao = db.userDao()
        val cache   = RepositoryProvider.userCache

        val pendingUsers = userDao.getPendingSync()

        if (pendingUsers.isEmpty()) {
            Log.d("USER_SYNC", "No pending user updates")
            return Result.success()
        }

        for (user in pendingUsers) {
            try {
                // Only name and phone are allowed for offline edits
                val request = UpdateUserRequest(
                    name  = user.name,
                    phone = user.phone
                )
                val response = api.updateMe(request)
                if (!response.isSuccessful) {
                    Log.e("USER_SYNC", "Server rejected update: ${response.code()}")
                    return Result.retry()
                }
                userDao.insert(user.copy(pendingSync = false))
                cache.invalidate(UserLruCache.CACHE_KEY_ME)
                Log.d("USER_SYNC", "User synced successfully")
            } catch (e: Exception) {
                Log.e("USER_SYNC", "Error syncing user", e)
                return Result.retry()
            }
        }

        return Result.success()
    }
}