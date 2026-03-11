package com.example.petcare.ui.screens.petprofile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.example.petcare.data.model.EventType
import com.example.petcare.data.model.MedicalEvent
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineFilterStatus
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineRecord

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
    val upcomingEventsCount: Int = 2,
    val isNfcSynched: Boolean = true, // Added for UI polish
    val events: List<MedicalEvent> = emptyList(),
    val vaccines: List<VaccineRecord> = emptyList(),
    val vaccineFilter: VaccineFilterStatus? = null // null means show all
)

class PetProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PetProfileUiState(
        vaccines = listOf(
            VaccineRecord("3", "Leptospirosis", "Dr. Johnson · City Vet Center", "Oct 1, 2023", null, null, VaccineFilterStatus.OVERDUE),
            VaccineRecord("2", "Rabies", "Dr. Smith · Happy Paws Clinic", "Mar 14, 2024", "Mar 14, 2025", "LP2024-0315", VaccineFilterStatus.UPCOMING),
            VaccineRecord("1", "Bordetella", "Dr. Smith · Happy Paws Clinic", "Sep 19, 2024", "Sep 19, 2025", null, VaccineFilterStatus.COMPLETED)
        ),
        events = listOf(
            MedicalEvent(
                id = "1",
                petId = "1",
                title = "Checkup",
                eventType = EventType.CHECKUP,
                price = 120.0,
                provider = "Dr. Smith",
                clinic = "Happy Paws Clinic",
                date = "Nov 19, 2024",
                description = "Annual wellness exam. All vitals normal. Weight stable at 28.5kg.",
                followUpDate = "Nov 19, 2025"
            ),
            MedicalEvent(
                id = "2",
                petId = "1",
                title = "Dental",
                eventType = EventType.DENTAL,
                price = 280.0,
                provider = "Dr. Johnson",
                clinic = "City Vet Center",
                date = "Jun 4, 2024",
                description = "Routine dental cleaning. No extractions needed."
            )
        )
    ))
    val uiState: StateFlow<PetProfileUiState> = _uiState.asStateFlow()
    
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    fun onTabSelected(index: Int) {
        _selectedTabIndex.value = index
    }
    
    fun onVaccineFilterClick(status: VaccineFilterStatus) {
        // Toggle the filter: if clicking the currently active one, clear it. Otherwise set it.
        val currentFilter = _uiState.value.vaccineFilter
        _uiState.value = _uiState.value.copy(
            vaccineFilter = if (currentFilter == status) null else status
        )
    }

    fun onVaccineClicked(vaccineRecord: VaccineRecord) {
        // In a real app, this would trigger navigation to the VaccineDetailsScreen
        // using a NavController
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
