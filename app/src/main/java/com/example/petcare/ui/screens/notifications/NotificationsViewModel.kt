package com.example.petcare.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.GroupedSuggestion
import com.example.petcare.data.model.PetFilterChip
import com.example.petcare.data.model.PetSuggestion
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val displayed: List<GroupedSuggestion> = emptyList(),
    val availablePets: List<PetFilterChip> = emptyList(),
    val selectedPetId: String? = null,
    val isLoading: Boolean = false
)

class NotificationsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var allSuggestions: List<PetSuggestion> = emptyList()

    fun load(filterPetId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val pets = RepositoryProvider.petRepository.getPets()
                .getOrElse { emptyList() }

            allSuggestions = pets.map { pet ->
                async {
                    RepositoryProvider.petRepository
                        .getPetSmart(pet.id)
                        .getOrElse { emptyList() }
                        .map { PetSuggestion(pet.id, pet.name, it) }
                }
            }.awaitAll().flatten()

            val chips = allSuggestions
                .groupBy { it.petId }
                .map { (petId, items) ->
                    PetFilterChip(
                        petId      = petId,
                        petName    = items.first().petName,
                        alertCount = items.size
                    )
                }
                .sortedBy { it.petName }

            _uiState.value = NotificationsUiState(
                displayed     = applyFilter(filterPetId),
                availablePets = chips,
                selectedPetId = filterPetId,
                isLoading     = false
            )
        }
    }

    fun onPetFilterSelected(petId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedPetId = petId,
            displayed     = applyFilter(petId)
        )
    }

    private fun applyFilter(petId: String?) = groupByVaccine(
        if (petId != null) allSuggestions.filter { it.petId == petId }
        else allSuggestions
    )

    private fun groupByVaccine(suggestions: List<PetSuggestion>) =
        suggestions
            .groupBy { it.suggestion.title }
            .map { (title, items) ->
                GroupedSuggestion(
                    vaccineTitle = title,
                    type         = if (items.any { it.suggestion.type == "danger" }) "danger"
                    else if (items.any { it.suggestion.type == "warning" }) "warning"
                    else "info",
                    pets         = items.map { it.petName }.distinct(),
                    message      = items.first().suggestion.message
                )
            }
            .sortedBy { when (it.type) { "danger" -> 0; "warning" -> 1; else -> 2 } }
}