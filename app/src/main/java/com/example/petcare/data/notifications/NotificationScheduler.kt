package com.example.petcare.data.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val UNIQUE_WORK_NAME = "petcare_vaccine_reminders"
    private const val UNIQUE_WORK_NAME_NOW = "petcare_vaccine_reminders_now"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<VaccineReminderWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Log.d(TAG, "Scheduled periodic reminders uniqueWork=$UNIQUE_WORK_NAME intervalHours=1 network=CONNECTED")
    }

    fun runNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<VaccineReminderWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME_NOW,
            ExistingWorkPolicy.REPLACE,
            request
        )
        Log.d(TAG, "Scheduled one-time reminders uniqueWork=$UNIQUE_WORK_NAME_NOW network=CONNECTED")
    }

    private const val TAG = "REMINDER_WORKER"
}
