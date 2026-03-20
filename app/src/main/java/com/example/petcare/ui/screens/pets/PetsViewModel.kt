package com.example.petcare.ui.screens.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.CreatePetRequest
import com.example.petcare.data.model.Pet
import com.example.petcare.data.repository.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PetsUiState(
    val pets: List<Pet> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFilter: String = "All Pets"
)

class PetsViewModel(
    private val petRepository: PetRepository
) : ViewModel() {

    private var allPets: List<Pet> = emptyList()

    private val _uiState = MutableStateFlow(PetsUiState(isLoading = true))
    val uiState: StateFlow<PetsUiState> = _uiState.asStateFlow()

    init {
        loadPets()
    }

    /** Llamado desde MainActivity cuando regresa de AddPet o PetProfile */
    fun refresh() = loadPets()

    fun loadPets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            FeatureExecutionTracker.track("Load My Pets") {
                petRepository.getPets()
            }.fold(
                onSuccess = { pets ->
                    allPets = pets
                    applyFilters()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error     = e.message ?: "Error loading pets"
                        )
                    }
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun updateSelectedFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        applyFilters()
    }

    private fun applyFilters() {
        val query  = _uiState.value.searchQuery
        val filter = _uiState.value.selectedFilter
        val filtered = allPets.filter { pet ->
            val matchesSearch = pet.name.contains(query, ignoreCase = true) ||
                    pet.breed.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "Healthy"     -> pet.status.equals("healthy", ignoreCase = true)
                "Vaccine Due" -> pet.status.equals("vaccine due", ignoreCase = true)
                "Lost"        -> pet.status.equals("lost", ignoreCase = true)
                else          -> true
            }
            matchesSearch && matchesFilter
        }
        _uiState.update { it.copy(pets = filtered, isLoading = false) }
    }

    fun createPet(request: CreatePetRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            petRepository.createPet(request).fold(
                onSuccess = {
                    loadPets()
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "Error creating pet") }
                }
            )
        }
    }

    fun deletePet(petId: String) {
        viewModelScope.launch {
            petRepository.deletePet(petId).fold(
                onSuccess = { loadPets() },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "Error deleting pet") }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}