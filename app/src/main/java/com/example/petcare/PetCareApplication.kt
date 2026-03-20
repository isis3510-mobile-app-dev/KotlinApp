// app/src/main/java/com/example/petcare/PetCareApplication.kt
package com.example.petcare

import android.app.Application
import com.example.petcare.data.repository.AuthRepository
import com.example.petcare.data.network.ApiClientProvider
import com.example.petcare.data.preferences.UserPreferencesRepository
import com.example.petcare.data.preferences.dataStore
import com.example.petcare.data.repository.NfcRepository
import com.example.petcare.data.repository.PetRepository
import com.example.petcare.data.repository.RepositoryProvider

class PetCareApplication : Application() {

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        userPreferencesRepository = UserPreferencesRepository(dataStore)
        RepositoryProvider.init(AuthRepository())
        com.example.petcare.data.analytics.FeatureExecutionTracker.init(this)
    }
}
