package com.example.petcare.ui.screens.petprofile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PetProfileUiState(
    val name: String = "Max",
    val breed: String = "Golden Retriever",
    val species: String = "Dog",
    val age: String = "6 yrs",
    val weight: String = "28.5 kg",
    val gender: String = "Male",
    val isHealthy: Boolean = true,
    val color: String = "Golden",
    val microchip: String = "XR123456789",
    val dateOfBirth: String = "Mar 14, 2020",
    val overdueVaccinesCount: Int = 1, // Set > 0 to test the conditional banner
    val upcomingEventsCount: Int = 2
)

class PetProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PetProfileUiState())
    val uiState: StateFlow<PetProfileUiState> = _uiState.asStateFlow()
    
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    fun onTabSelected(index: Int) {
        _selectedTabIndex.value = index
    }

    fun onAddEventClicked() {
        // Implementation
    }

    fun onAddVaccineClicked() {
        // Implementation
    }

    fun onLostModeClicked() {
        // Implementation
    }

    fun onNfcActiveClicked() {
        // Implementation
    }
}
