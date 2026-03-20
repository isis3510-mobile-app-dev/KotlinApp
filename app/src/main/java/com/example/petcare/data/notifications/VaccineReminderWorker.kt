package com.example.petcare.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.petcare.PetCareApplication
import com.example.petcare.data.model.CreateNotificationRequest
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.data.repository.UserRepository
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
        RepositoryProvider.ensureInitialized()

        val app = applicationContext as? PetCareApplication ?: return Result.success()
        val preferences = app.userPreferencesRepository

        if (!preferences.notificationsEnabled.first()) {
            return Result.success()
        }

        val authUser = RepositoryProvider.authRepository.currentUser ?: return Result.success()
        if (authUser.uid.isBlank()) return Result.success()

        val userId = UserRepository(RepositoryProvider.apiService)
            .getMe()
            .getOrNull()
            ?.id
            ?: return Result.retry()

        val pets = RepositoryProvider.petRepository.getPets().getOrElse { emptyList() }
        if (pets.isEmpty()) return Result.success()

        val urgencyLevel = preferences.vaccineUrgencyLevel.first()
        val sentKeys = preferences.sentNotificationKeys.first()

        val eventsByPetId = coroutineScope {
            pets.map { pet ->
                async {
                    pet.id to RepositoryProvider.eventRepository
                        .getEvents(petId = pet.id)
                        .getOrElse { emptyList() }
                }
            }.awaitAll().toMap()
        }

        val suggestionsByPetId = coroutineScope {
            pets.map { pet ->
                async {
                    pet.id to RepositoryProvider.petRepository
                        .getPetSmart(pet.id)
                        .getOrElse { emptyList() }
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

        candidates.forEach { candidate ->
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
                ?: return@forEach

            NotificationDispatcher.showReminder(
                context = applicationContext,
                candidate = candidate,
                backendNotificationId = created.id
            )
            preferences.addSentNotificationKey(candidate.dedupeKey)
        }

        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
