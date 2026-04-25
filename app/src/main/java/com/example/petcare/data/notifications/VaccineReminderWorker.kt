package com.example.petcare.data.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.petcare.PetCareApplication
import com.example.petcare.data.model.CreateNotificationRequest
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class VaccineReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val evaluator = ReminderEvaluator()

    override suspend fun doWork(): Result = try {
        Log.d(TAG, "doWork started thread=${Thread.currentThread().name}")
        RepositoryProvider.ensureInitialized(applicationContext)

        val app = applicationContext as? PetCareApplication ?: run {
            Log.w(TAG, "Skipping reminders: applicationContext is not PetCareApplication")
            return Result.success()
        }
        val preferences = app.userPreferencesRepository

        if (!preferences.notificationsEnabled.first()) {
            Log.d(TAG, "Skipping reminders: notifications disabled")
            return Result.success()
        }

        val authUser = RepositoryProvider.authRepository.currentUser ?: run {
            Log.d(TAG, "Skipping reminders: no Firebase user")
            return Result.success()
        }
        if (authUser.uid.isBlank()) {
            Log.d(TAG, "Skipping reminders: blank Firebase uid")
            return Result.success()
        }

        val userId = RepositoryProvider.userRepository
            .getMe()
            .getOrNull()
            ?.id
            ?: run {
                Log.w(TAG, "Retry reminders: backend user profile not available")
                return Result.retry()
            }

        val pets = RepositoryProvider.petRepository.getPets().getOrElse { emptyList() }
        Log.d(TAG, "Loaded pets for reminders count=${pets.size} userId=$userId")
        if (pets.isEmpty()) {
            Log.d(TAG, "No pets available for reminder evaluation")
            return Result.success()
        }

        val urgencyLevel = preferences.vaccineUrgencyLevel.first()
        val sentKeys = preferences.sentNotificationKeys.first()
        Log.d(TAG, "Reminder preferences urgency=$urgencyLevel sentKeys=${sentKeys.size}")

        val eventsByPetId = coroutineScope {
            pets.map { pet ->
                async(Dispatchers.IO) {
                    val events = RepositoryProvider.eventRepository
                        .getEvents(petId = pet.id)
                        .getOrElse { emptyList() }
                    Log.d(
                        TAG,
                        "Events fetched petId=${pet.id} count=${events.size} thread=${Thread.currentThread().name}"
                    )
                    pet.id to events
                }
            }.awaitAll().toMap()
        }

        val suggestionsByPetId = coroutineScope {
            pets.map { pet ->
                async(Dispatchers.IO) {
                    val suggestions = RepositoryProvider.petRepository
                        .getPetSmart(pet.id)
                        .getOrElse { emptyList() }
                    Log.d(
                        TAG,
                        "Suggestions fetched petId=${pet.id} count=${suggestions.size} thread=${Thread.currentThread().name}"
                    )
                    pet.id to suggestions
                }
            }.awaitAll().toMap()
        }

        val candidates = evaluator.evaluate(
            pets = pets,
            eventsByPetId = eventsByPetId,
            suggestionsByPetId = suggestionsByPetId,
            urgencyLevel = urgencyLevel,
            sentKeys = sentKeys
        )
        Log.d(TAG, "Reminder candidates count=${candidates.size}")

        candidates.forEach { candidate ->
            Log.d(TAG, "Creating notification type=${candidate.backendType} target=${candidate.targetRoute}")
            val created = RepositoryProvider.notificationRepository
                .createNotification(
                    CreateNotificationRequest(
                        userId = userId,
                        type = candidate.backendType,
                        header = candidate.header,
                        text = candidate.text
                    )
                )
                .getOrNull()
                ?: run {
                    Log.w(TAG, "Backend notification creation failed type=${candidate.backendType}")
                    return@forEach
                }

            NotificationDispatcher.showReminder(
                context = applicationContext,
                candidate = candidate,
                backendNotificationId = created.id
            )
            preferences.addSentNotificationKey(candidate.dedupeKey)
            Log.d(TAG, "Notification dispatched backendId=${created.id} key=${candidate.dedupeKey}")
        }

        Log.d(TAG, "doWork success")
        Result.success()
    } catch (e: Exception) {
        Log.e(TAG, "doWork failed, retrying: ${e.message}", e)
        Result.retry()
    }

    private companion object {
        const val TAG = "REMINDER_WORKER"
    }
}
