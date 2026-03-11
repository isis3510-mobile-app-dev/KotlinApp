package com.example.petcare.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.preferences.AppThemeMode
import com.example.petcare.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val notificationsEnabled: Boolean = false,
    val offlineModeEnabled: Boolean = false,
    val currentThemeMode: AppThemeMode = AppThemeMode.SYSTEM
)

sealed interface UiEvent {
    object NavigateToLogin : UiEvent
}

class ProfileViewModel(
    private val repo: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        repo.notificationsEnabled,
        repo.offlineModeEnabled,
        repo.themeMode
    ) { notifications, offlineMode, theme ->
        ProfileUiState(
            notificationsEnabled = notifications,
            offlineModeEnabled = offlineMode,
            currentThemeMode = theme
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    private val _uiEvents = Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun onNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch { repo.setNotificationsEnabled(enabled) }
    }

    fun onOfflineModeToggled(enabled: Boolean) {
        viewModelScope.launch { repo.setOfflineModeEnabled(enabled) }
    }

    fun onThemeModeChanged(mode: AppThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    fun onSignOutClicked() {
        viewModelScope.launch {
            _uiEvents.send(UiEvent.NavigateToLogin)
        }
    }
}
