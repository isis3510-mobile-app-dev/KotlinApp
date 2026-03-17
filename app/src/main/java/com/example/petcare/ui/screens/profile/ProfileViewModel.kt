package com.example.petcare.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.auth.AuthRepository
import com.example.petcare.data.auth.UserRepository
import com.example.petcare.data.model.UpdateUserRequest
import com.example.petcare.data.model.User
import com.example.petcare.data.network.ApiClient
import com.example.petcare.data.network.ApiService
import com.example.petcare.data.preferences.AppThemeMode
import com.example.petcare.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val notificationsEnabled: Boolean = false,
    val offlineModeEnabled: Boolean = false,
    val currentThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
    )

sealed interface UiEvent {
    object NavigateToLogin : UiEvent
    object SaveSuccess : UiEvent
}

class ProfileViewModel(
    private val repo: UserPreferencesRepository,
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val apiService = ApiClient.create(authRepository).create(ApiService::class.java)
    private val userRepository = UserRepository(apiService)

    private val _userState = MutableStateFlow<Triple<User?, Boolean, String?>>(
        Triple(null, false, null) // user, isLoading, error
    )

    val uiState: StateFlow<ProfileUiState> = combine(
        repo.notificationsEnabled,
        repo.offlineModeEnabled,
        repo.themeMode,
        _userState
    ) { notifications, offlineMode, theme, (user, isLoading, error) ->
        ProfileUiState(
            user = user,
            isLoading = isLoading,
            error = error,
            notificationsEnabled = notifications,
            offlineModeEnabled = offlineMode,
            currentThemeMode = theme
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    private val _uiEvents = Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _userState.value = Triple(null, true, null)
            userRepository.getMe()
                .onSuccess { user ->
                    _userState.value = Triple(user, false, null)
                }
                .onFailure { error ->
                    _userState.value = Triple(null, false, error.message)
                }
        }
    }

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
            authRepository.logout()
            _uiEvents.send(UiEvent.NavigateToLogin)
        }
    }


    sealed class EditField {
        object Name : EditField()
        object Phone : EditField()
        object Address : EditField()
    }


    fun updateField(field: EditField, value: String) {
        val current = _userState.value.first ?: return
        viewModelScope.launch {
            _userState.value = Triple(current, true, null) // isLoading = true

            val request = when (field) {
                is EditField.Name    -> UpdateUserRequest(
                    name = value,
                    initials = value.split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .take(2)
                        .joinToString("")
                )
                is EditField.Phone   -> UpdateUserRequest(phone = value)
                is EditField.Address -> UpdateUserRequest(address = value)
            }

            userRepository.updateMe(request)
                .onSuccess { updatedUser ->
                    _userState.value = Triple(updatedUser, false, null)
                    _uiEvents.send(UiEvent.SaveSuccess)
                }
                .onFailure { error ->
                    _userState.value = Triple(current, false, error.message)
                }
        }
    }
}