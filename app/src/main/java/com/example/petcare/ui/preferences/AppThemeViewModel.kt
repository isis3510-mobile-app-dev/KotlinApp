package com.example.petcare.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.preferences.AppThemeMode
import com.example.petcare.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppThemeViewModel(
    private val repo: UserPreferencesRepository
) : ViewModel() {
    val themeMode: StateFlow<AppThemeMode> = repo.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppThemeMode.SYSTEM)

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }
}
