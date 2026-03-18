package com.example.petcare.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.Event
import com.example.petcare.data.model.Pet
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val userName: String = "",
    val userId: String = "",
    val pets: List<Pet> = emptyList(),
    val recentEvents: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Set once the AuthViewModel resolves the logged-in user's profile. */
    fun setUserInfo(name: String, userId: String) {
        _state.value = _state.value.copy(userName = name, userId = userId)
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            RepositoryProvider.petRepository.getPets().fold(
                onSuccess = { pets ->
                    // Fetch events for every pet in parallel
                    val eventResults = pets.map { pet ->
                        async {
                            RepositoryProvider.eventRepository
                                .getEvents(petId = pet.id)
                                .getOrElse { emptyList() }
                        }
                    }.awaitAll().flatten()

                    _state.value = _state.value.copy(
                        pets         = pets,
                        recentEvents = eventResults.take(5),
                        isLoading    = false
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to load data"
                    )
                }
            )
        }
    }
}