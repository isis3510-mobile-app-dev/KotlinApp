package com.example.petcare.ui.screens.petprofile.vaccines

import androidx.lifecycle.ViewModel
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineFilterStatus
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VaccineDetailsUiState(
    val vaccine: VaccineRecord? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class VaccineDetailsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VaccineDetailsUiState(isLoading = true))
    val uiState: StateFlow<VaccineDetailsUiState> = _uiState.asStateFlow()

    // Mocking data load
    init {
        loadVaccineDetails("1")
    }

    fun loadVaccineDetails(vaccineId: String) {
        // Mock data
        _uiState.value = VaccineDetailsUiState(
            vaccine = VaccineRecord(
                id = "1",
                name = "Bordetella",
                provider = "Dr. Smith",
                dateGiven = "Sep 19, 2024",
                nextDueDate = "Sep 19, 2025",
                lotNumber = null,
                status = VaccineFilterStatus.COMPLETED,
                attachedDocumentName = "vaccine_record_pdf"
            ),
            isLoading = false
        )
    }

    fun deleteVaccine() {
        // Implementation
    }

    fun editVaccine() {
        // Implementation
    }
}
