package com.example.petcare.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.auth.AuthRepository
import com.example.petcare.data.auth.UserRepository
import com.example.petcare.data.model.User
import com.example.petcare.data.network.ApiClient
import com.example.petcare.data.network.ApiService
import com.google.firebase.auth.FirebaseUser
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
    }

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()
    val isLoggedIn get() = authRepository.currentUser != null


    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            authRepository.login(email, password).fold(
                onSuccess = { firebaseUser ->
                    fetchUserProfile()
                    _state.value = AuthState.Success(firebaseUser)
                },
                onFailure = { _state.value = AuthState.Error(friendlyError(it)) }
            )
        }
    }

    fun register(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            authRepository.register(email, password, fullName).fold(
                onSuccess = { firebaseUser ->
                    fetchUserProfile()
                    _state.value = AuthState.Success(firebaseUser)
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
                    fetchUserProfile()
                    _state.value = AuthState.Success(firebaseUser)
                },
                onFailure = { _state.value = AuthState.Error(friendlyError(it)) }
            )
        }
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            userRepository.getMe().fold(
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


    private fun launchAuth(block: suspend () -> Result<FirebaseUser>) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            block().fold(
                onSuccess = { _state.value = AuthState.Success(it) },
                onFailure = { _state.value = AuthState.Error(friendlyError(it)) }
            )
        }
    }

    private fun friendlyError(e: Throwable): String = when {
        e.message?.contains("no user record") == true           -> "No account record"
        e.message?.contains("password is invalid") == true      -> "Password is invalid"
        e.message?.contains("email address is already") == true -> "Email address is already registered"
        e.message?.contains("badly formatted") == true          -> "Email is badly formatted"
        e.message?.contains("network") == true                  -> "No connection to internet"
        else -> e.message ?: "Unknown error"
    }
}
