package com.example.petcare

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.petcare.ui.preferences.AppThemeViewModel
import com.example.petcare.data.model.User
import com.example.petcare.data.preferences.UserPreferencesRepository
import com.example.petcare.ui.screens.petprofile.EditPetViewModel
import com.example.petcare.ui.screens.profile.ProfileViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val repository: UserPreferencesRepository,
    private val initialUser: User? = null
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

/**
 * Factory for AndroidViewModel subclasses that need an Application reference.
 * Used by EditPetViewModel (and AddPetViewModel, which is already handled by
 * the default AndroidViewModelFactory built into the Compose viewModel() call).
 */
@Suppress("UNCHECKED_CAST")
class ApplicationViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(EditPetViewModel::class.java) -> {
                EditPetViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}