package com.example.petcare

import android.app.Application
import com.example.petcare.data.preferences.UserPreferencesRepository
import com.example.petcare.data.preferences.dataStore

class PetCareApplication : Application() {
    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        userPreferencesRepository = UserPreferencesRepository(dataStore)
    }
}
