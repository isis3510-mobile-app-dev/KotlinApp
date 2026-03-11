package com.example.petcare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.petcare.data.preferences.AppThemeViewModel
import com.example.petcare.data.preferences.UserPreferencesRepository
import com.example.petcare.ui.screens.profile.ProfileViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val repository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AppThemeViewModel::class.java) -> {
                AppThemeViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
