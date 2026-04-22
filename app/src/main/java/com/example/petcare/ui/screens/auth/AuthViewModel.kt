package com.example.petcare.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.UpdateUserRequest
import com.example.petcare.data.repository.AuthRepository
import com.example.petcare.data.repository.UserRepository
import com.example.petcare.data.model.User
import com.example.petcare.data.network.ApiClient
import com.example.petcare.data.network.ApiService
import com.google.firebase.auth.FirebaseUser
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.normalizeForCommit
import com.example.petcare.util.validateCommittedInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val apiService = ApiClient.create(authRepository).create(ApiService::class.java)
    private val userRepository = UserRepository(apiService)

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val user: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
        data class ResetEmailSent(val email: String) : AuthState()
    }

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    val isLoggedIn get() = authRepository.currentUser != null

    // Auth
    fun login(email: String, password: String) {
        val emailError = validateCommittedInput(
            value = email,
            fieldPolicy = InputFieldPolicy.EMAIL,
            required = true,
            maxLength = InputTextLimits.EMAIL,
            fieldName = "Email"
        )
        if (emailError != null) {
            _state.value = AuthState.Error(emailError)
            return
        }

        val passwordError = validateCommittedInput(
            value = password,
            fieldPolicy = InputFieldPolicy.PASSWORD,
            required = true,
            maxLength = InputTextLimits.PASSWORD,
            fieldName = "Password"
        )
        if (passwordError != null) {
            _state.value = AuthState.Error(passwordError)
            return
        }

        viewModelScope.launch {
            _state.value = AuthState.Loading
            authRepository.login(
                normalizeForCommit(email, InputFieldPolicy.EMAIL),
                normalizeForCommit(password, InputFieldPolicy.PASSWORD)
            ).fold(
                onSuccess = { firebaseUser ->
                    completeAuthSuccess(firebaseUser, shouldRetryProfileBootstrap = false)
                },
                onFailure = { _state.value = AuthState.Error(friendlyError(it)) }
            )
        }
    }

    fun register(email: String, password: String, fullName: String) {
        val nameError = validateCommittedInput(
            value = fullName,
            fieldPolicy = InputFieldPolicy.GENERAL_TEXT,
            required = true,
            maxLength = InputTextLimits.USER_NAME,
            fieldName = "Full name"
        )
        if (nameError != null) {
            _state.value = AuthState.Error(nameError)
            return
        }

        val emailError = validateCommittedInput(
            value = email,
            fieldPolicy = InputFieldPolicy.EMAIL,
            required = true,
            maxLength = InputTextLimits.EMAIL,
            fieldName = "Email"
        )
        if (emailError != null) {
            _state.value = AuthState.Error(emailError)
            return
        }

        val passwordError = validateCommittedInput(
            value = password,
            fieldPolicy = InputFieldPolicy.PASSWORD,
            required = true,
            maxLength = InputTextLimits.PASSWORD,
            fieldName = "Password"
        )
        if (passwordError != null) {
            _state.value = AuthState.Error(passwordError)
            return
        }

        viewModelScope.launch {
            _state.value = AuthState.Loading
            authRepository.register(
                email = normalizeForCommit(email, InputFieldPolicy.EMAIL),
                password = normalizeForCommit(password, InputFieldPolicy.PASSWORD),
                fullName = normalizeForCommit(fullName, InputFieldPolicy.GENERAL_TEXT)
            ).fold(
                onSuccess = { firebaseUser ->
                    completeAuthSuccess(firebaseUser, shouldRetryProfileBootstrap = true)
                },
                onFailure = { _state.value = AuthState.Error(friendlyError(it)) }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            authRepository.loginWithGoogle(idToken).fold(
                onSuccess = { firebaseUser ->
                    completeAuthSuccess(firebaseUser, shouldRetryProfileBootstrap = true)
                },
                onFailure = { _state.value = AuthState.Error(friendlyError(it)) }
            )
        }
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            fetchUserProfileInternal().fold(
                onSuccess = { _userProfile.value = it },
                onFailure = { android.util.Log.e("AUTH", "Fetch profile error: ${it.message}") }
            )
        }
    }

    fun logout() {
        authRepository.logout()
        _userProfile.value = null
        _state.value = AuthState.Idle
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }

    fun syncEmailWithBackend() {
        viewModelScope.launch {
            val currentFirebaseEmail = authRepository.currentUser?.email ?: return@launch
            val mongoEmail = userRepository.getMe().getOrNull()?.email ?: return@launch

            if (currentFirebaseEmail != mongoEmail) {
                android.util.Log.d("EMAIL_SYNC",
                    "Syncing email: Firebase=$currentFirebaseEmail, Mongo=$mongoEmail")
                userRepository.updateMe(UpdateUserRequest(email = currentFirebaseEmail))
                    .onSuccess { updatedUser ->
                        _userProfile.value = updatedUser
                        android.util.Log.d("EMAIL_SYNC", "MongoDB email updated successfully")
                    }
                    .onFailure {
                        android.util.Log.e("EMAIL_SYNC", "Failed to sync: ${it.message}")
                    }
            }
        }
    }
    fun resetPassword(email: String) {
        val emailError = validateCommittedInput(
            value = email,
            fieldPolicy = InputFieldPolicy.EMAIL,
            required = true,
            maxLength = InputTextLimits.EMAIL,
            fieldName = "Email"
        )
        if (emailError != null) {
            _state.value = AuthState.Error(emailError)
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            authRepository.resetPassword(normalizeForCommit(email, InputFieldPolicy.EMAIL))
                .onSuccess {
                    _state.value = AuthState.ResetEmailSent(normalizeForCommit(email, InputFieldPolicy.EMAIL))
                }
                .onFailure {
                    _state.value = AuthState.Error(friendlyError(it))
                }
        }
    }


    // ── Helpers ───────────────────────────────────────────────────

    private suspend fun completeAuthSuccess(
        firebaseUser: FirebaseUser,
        shouldRetryProfileBootstrap: Boolean
    ) {
        val profileResult = fetchUserProfileWithRetry(
            maxAttempts = if (shouldRetryProfileBootstrap) 5 else 2
        )

        profileResult.onSuccess { _userProfile.value = it }
            .onFailure {
                android.util.Log.w("AUTH", "Profile bootstrap incomplete: ${it.message}")
            }

        _state.value = AuthState.Success(firebaseUser)
    }

    private suspend fun fetchUserProfileInternal(): Result<User> = userRepository.getMe()

    private suspend fun fetchUserProfileWithRetry(maxAttempts: Int): Result<User> {
        var lastResult: Result<User> = Result.failure(IllegalStateException("Profile not loaded"))

        repeat(maxAttempts) { attempt ->
            lastResult = fetchUserProfileInternal()
            if (lastResult.isSuccess) return lastResult

            val error = lastResult.exceptionOrNull()
            if (!shouldRetryProfileFetch(error) || attempt == maxAttempts - 1) {
                return lastResult
            }

            delay((attempt + 1) * 300L)
        }

        return lastResult
    }

    private fun shouldRetryProfileFetch(error: Throwable?): Boolean {
        val message = error?.message?.lowercase().orEmpty()
        return "empty response" in message || "404" in message || "not found" in message
    }

    private fun friendlyError(e: Throwable): String = when {
        e.message?.contains("email address is already", ignoreCase = true) == true ->
            "This email is already registered"
        e.message?.contains("badly formatted", ignoreCase = true) == true ->
            "Enter a valid email address"
        e.message?.contains("network", ignoreCase = true) == true ->
            "No internet connection. Please try again."
        e.message?.contains("too many", ignoreCase = true) == true ->
            "Too many attempts. Please try again later."
        e.message?.contains("no user record", ignoreCase = true) == true ||
            e.message?.contains("password is invalid", ignoreCase = true) == true ||
            e.message?.contains("invalid credential", ignoreCase = true) == true ||
            e.message?.contains("supplied auth credential is incorrect", ignoreCase = true) == true ->
            "Incorrect email or password"
        else -> "We couldn't sign you in. Please try again."
    }
}
