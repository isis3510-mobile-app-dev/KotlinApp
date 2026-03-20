package com.example.petcare.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

class NotificationDismissedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != NotificationDispatcher.ACTION_DISMISS_NOTIFICATION) return
        val notificationId = intent.getStringExtra(NotificationDispatcher.EXTRA_NOTIFICATION_ID) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RepositoryProvider.ensureInitialized()
                RepositoryProvider.notificationRepository
                    .markNotificationDismissed(notificationId, Instant.now().toString())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
