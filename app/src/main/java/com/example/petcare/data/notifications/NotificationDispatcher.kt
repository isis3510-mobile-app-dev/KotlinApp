package com.example.petcare.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.petcare.MainActivity
import com.example.petcare.R

object NotificationDispatcher {

    const val CHANNEL_ID = "petcare_reminders"
    private const val CHANNEL_NAME = "Pet reminders"
    private const val CHANNEL_DESCRIPTION = "Upcoming events and vaccine alerts"

    const val ACTION_OPEN_NOTIFICATION = "com.example.petcare.action.OPEN_NOTIFICATION"
    const val ACTION_DISMISS_NOTIFICATION = "com.example.petcare.action.DISMISS_NOTIFICATION"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_TARGET_ROUTE = "extra_target_route"

    fun showReminder(
        context: Context,
        candidate: ReminderCandidate,
        backendNotificationId: String
    ) {
        createNotificationChannel(context)

        val notificationInt = candidate.dedupeKey.hashCode()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_NOTIFICATION
            putExtra(EXTRA_NOTIFICATION_ID, backendNotificationId)
            putExtra(EXTRA_TARGET_ROUTE, candidate.targetRoute)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationInt,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationDismissedReceiver::class.java).apply {
            action = ACTION_DISMISS_NOTIFICATION
            putExtra(EXTRA_NOTIFICATION_ID, backendNotificationId)
        }

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationInt,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(candidate.header)
            .setContentText(candidate.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(candidate.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationInt, notification)
        } catch (_: SecurityException) {
            // Permission not granted (Android 13+) or notifications disabled at OS level.
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }
}
