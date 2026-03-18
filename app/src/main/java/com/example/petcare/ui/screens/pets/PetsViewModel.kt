package com.example.petcare.ui.screens.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.CreatePetRequest
import com.example.petcare.data.model.Pet
import com.example.petcare.data.repository.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PetsUiState(
    val pets: List<Pet> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class PetsViewModel(
    private val petRepository: PetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetsUiState(isLoading = true))
    val uiState: StateFlow<PetsUiState> = _uiState.asStateFlow()

    init {
        loadPets()
    }

    fun loadPets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            petRepository.getPets().fold(
                onSuccess = { pets ->
                    _uiState.value = PetsUiState(pets = pets, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = PetsUiState(
                        isLoading = false,
                        error = e.message ?: "Error loading pets"
                    )
                }
            )
        }
    }

    fun createPet(request: CreatePetRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            petRepository.createPet(request).fold(
                onSuccess = {
                    loadPets()
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Error creating pet"
                    )
                }
            )
        }
    }

    fun deletePet(petId: String) {
        viewModelScope.launch {
            petRepository.deletePet(petId).fold(
                onSuccess = { loadPets() },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Error deleting pet"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}