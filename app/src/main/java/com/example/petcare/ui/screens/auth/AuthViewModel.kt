package com.example.petcare.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.auth.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val user: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    val isLoggedIn get() = repository.currentUser != null


    fun login(email: String, password: String) = launchAuth {
        repository.login(email, password)
    }

    fun register(email: String, password: String) = launchAuth {
        repository.register(email, password)
    }

    fun loginWithGoogle(idToken: String) = launchAuth {
        repository.loginWithGoogle(idToken)
    }

    fun logout() {
        repository.logout()
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
        e.message?.contains("no user record") == true          -> "No existe una cuenta con ese correo"
        e.message?.contains("password is invalid") == true     -> "Contraseña incorrecta"
        e.message?.contains("email address is already") == true -> "Este correo ya está registrado"
        e.message?.contains("badly formatted") == true         -> "Correo inválido"
        e.message?.contains("network") == true                 -> "Sin conexión a internet"
        else -> e.message ?: "Error desconocido"
    }
}
