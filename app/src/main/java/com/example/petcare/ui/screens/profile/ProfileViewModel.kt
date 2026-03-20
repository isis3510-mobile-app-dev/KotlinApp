package com.example.petcare.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.repository.AuthRepository
import com.example.petcare.data.repository.UserRepository
import com.example.petcare.data.model.UpdateUserRequest
import com.example.petcare.data.model.User
import com.example.petcare.data.model.VaccineUrgencyLevel
import com.example.petcare.data.network.ApiClient
import com.example.petcare.data.network.ApiService
import com.example.petcare.data.preferences.AppThemeMode
import com.example.petcare.data.preferences.UserPreferencesRepository
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.InputValidators
import com.example.petcare.util.enforceMaxLength
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
    val vaccineUrgencyLevel: VaccineUrgencyLevel = VaccineUrgencyLevel.DANGER_ONLY,
    val offlineModeEnabled: Boolean = false,
    val currentThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val emailUpdateSent: Boolean = false
)

sealed interface UiEvent {
    object NavigateToLogin : UiEvent
    object SaveSuccess : UiEvent
    data class ShowMessage(val message: String) : UiEvent
}

class ProfileViewModel(
    private val repo: UserPreferencesRepository,
    private val authRepository: AuthRepository = AuthRepository(),
    private val initialUser: User? = null
) : ViewModel() {

    private val apiService = ApiClient.create(authRepository).create(ApiService::class.java)
    private val userRepository = UserRepository(apiService)

    private val _userState = MutableStateFlow(
        Triple(initialUser, initialUser == null, null as String?)
    )

    val uiState: StateFlow<ProfileUiState> = combine(
        repo.notificationsEnabled,
        repo.vaccineUrgencyLevel,
        repo.offlineModeEnabled,
        repo.themeMode,
        _userState
    ) { notifications, vaccineUrgencyLevel, offlineMode, theme, (user, isLoading, error) ->
        ProfileUiState(
            user                 = user,
            isLoading            = isLoading,
            error                = error,
            notificationsEnabled = notifications,
            vaccineUrgencyLevel  = vaccineUrgencyLevel,
            offlineModeEnabled   = offlineMode,
            currentThemeMode     = theme
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    private val _uiEvents = Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    init {
        // Si no se inyectó un usuario inicial, cargamos desde la API
        // (siempre cargamos para tener el pet_ids actualizado)
        loadUserProfile()
    }

    /** Recarga el perfil desde la API — llamar cuando el pet count puede haber cambiado */
    fun loadUserProfile() {
        viewModelScope.launch {
            _userState.value = Triple(_userState.value.first, true, null)
            FeatureExecutionTracker.track("Load User Profile") {
                userRepository.getMe()
            }.onSuccess { user ->
                    _userState.value = Triple(user, false, null)
                }
                .onFailure { error ->
                    // Si falla y ya teníamos un usuario, lo mantenemos (no mostramos error)
                    val existing = _userState.value.first
                    _userState.value = Triple(existing, false, if (existing == null) error.message else null)
                }
        }
    }

    fun onNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch { repo.setNotificationsEnabled(enabled) }
    }

    fun onVaccineUrgencyLevelChanged(level: VaccineUrgencyLevel) {
        viewModelScope.launch { repo.setVaccineUrgencyLevel(level) }
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

    sealed class EditField {
        object Name    : EditField()
        object Phone   : EditField()
        object Address : EditField()
    }

    fun updateField(field: EditField, value: String) {
        val current = _userState.value.first ?: return
        viewModelScope.launch {
            _userState.value = Triple(current, true, null)

            val sanitizedValue = when (field) {
                is EditField.Name -> enforceMaxLength(value.trim(), InputTextLimits.USER_NAME)
                is EditField.Phone -> enforceMaxLength(value.trim(), InputTextLimits.PHONE)
                is EditField.Address -> enforceMaxLength(value.trim(), InputTextLimits.ADDRESS)
            }

            if (field is EditField.Phone &&
                sanitizedValue.isNotBlank() &&
                !InputValidators.isValidFlexiblePhone(sanitizedValue)
            ) {
                _userState.value = Triple(current, false, null)
                _uiEvents.send(UiEvent.ShowMessage("Invalid phone number format"))
                return@launch
            }

            val request = when (field) {
                is EditField.Name -> UpdateUserRequest(
                    name     = sanitizedValue,
                    initials = sanitizedValue.split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .take(2)
                        .joinToString("")
                )
                is EditField.Phone   -> UpdateUserRequest(phone = sanitizedValue)
                is EditField.Address -> UpdateUserRequest(address = sanitizedValue)
            }

            FeatureExecutionTracker.track("Update User Profile") {
                userRepository.updateMe(request)
            }.onSuccess { updatedUser ->
                    _userState.value = Triple(updatedUser, false, null)
                    _uiEvents.send(UiEvent.SaveSuccess)
                }
                .onFailure { error ->
                    _userState.value = Triple(current, false, error.message)
                }
        }
    }

    fun updateEmail(currentPassword: String, newEmail: String) {
        val current = _userState.value.first ?: return
        viewModelScope.launch {
            _userState.value = Triple(current, true, null)
            val sanitizedEmail = newEmail.trim()
            authRepository.updateEmail(currentPassword, sanitizedEmail)
                .onSuccess {
                    userRepository.updateMe(UpdateUserRequest(email = sanitizedEmail))
                        .onSuccess { updatedUser ->
                            _userState.value = Triple(updatedUser, false, null)
                        }
                        .onFailure {
                            _userState.value = Triple(current.copy(email = sanitizedEmail), false, null)
                        }
                    _uiEvents.send(UiEvent.ShowMessage("Email updated successfully"))
                }
                .onFailure { error ->
                    _userState.value = Triple(current, false, null)
                    val message = when {
                        error.message?.contains("password") == true ||
                                error.message?.contains("credential") == true ->
                            "Incorrect password"
                        error.message?.contains("already") == true ->
                            "That email is already in use"
                        error.message?.contains("formatted") == true ->
                            "Invalid email format"
                        error.message?.contains("Google") == true ->
                            "Google accounts cannot change their email here"
                        else -> error.message ?: "Error updating email"
                    }
                    _uiEvents.send(UiEvent.ShowMessage(message))
                }
        }
    }

    fun deleteAccount() {
        val current = _userState.value.first ?: return
        viewModelScope.launch {
            _userState.value = Triple(current, true, null)
            FeatureExecutionTracker.track("Delete Account") {
                userRepository.deleteMe()
            }.onSuccess {
                    authRepository.logout()
                    _userState.value = Triple(null, false, null)
                    _uiEvents.send(UiEvent.NavigateToLogin)
                }
                .onFailure { error ->
                    _userState.value = Triple(current, false, null)
                    _uiEvents.send(UiEvent.ShowMessage(
                        error.message ?: "Error deleting account"
                    ))
                }
        }
    }
}
