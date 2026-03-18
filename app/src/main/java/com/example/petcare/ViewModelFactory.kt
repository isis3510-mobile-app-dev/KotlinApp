package com.example.petcare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.petcare.ui.preferences.AppThemeViewModel
import com.example.petcare.data.model.User
import com.example.petcare.data.preferences.UserPreferencesRepository
import com.example.petcare.ui.screens.profile.ProfileViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val repository: UserPreferencesRepository,
    private val initialUser: User? = null   // ← nuevo, opcional
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AppThemeViewModel::class.java) -> {
                AppThemeViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(repository, initialUser = initialUser) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}