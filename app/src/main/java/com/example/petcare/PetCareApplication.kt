// app/src/main/java/com/example/petcare/PetCareApplication.kt
package com.example.petcare

import android.app.Application
import com.example.petcare.data.local.hive.HiveCacheManager
import com.example.petcare.data.repository.AuthRepository
import com.example.petcare.data.network.NetworkObserver
import com.example.petcare.data.notifications.NotificationScheduler
import com.example.petcare.data.preferences.UserPreferencesRepository
import com.example.petcare.data.preferences.dataStore
import com.example.petcare.data.repository.NfcRepository
import com.example.petcare.data.repository.PetRepository
import com.example.petcare.data.repository.RepositoryProvider

class PetCareApplication : Application() {
    private lateinit var networkObserver: NetworkObserver

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        userPreferencesRepository = UserPreferencesRepository(dataStore)
        RepositoryProvider.init(AuthRepository(), this)
        networkObserver = NetworkObserver(this)
        networkObserver.register()
        com.example.petcare.data.analytics.FeatureExecutionTracker.init(this)
        NotificationScheduler.schedule(this)

    }
}
