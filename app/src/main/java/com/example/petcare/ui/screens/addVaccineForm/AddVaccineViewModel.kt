package com.example.petcare.ui.screens.addVaccineForm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.AddDocumentRequest
import com.example.petcare.data.model.AddVaccinationRequest
import com.example.petcare.data.model.Vaccine
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.ui.screens.addEventForm.StagedDocument
import com.example.petcare.util.FirebaseDocumentUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AddVaccineFormState(
    val petId: String = "",
    // Step 1
    val selectedVaccine: Vaccine? = null,
    val catalogVaccines: List<Vaccine> = emptyList(),
    val isCatalogLoading: Boolean = false,
    val dateGiven: String = "",
    // Step 2
    val administeredBy: String = "",
    val lotNumber: String = "",
    // Step 3
    val nextDueDate: String = "",
    val stagedDocuments: List<StagedDocument> = emptyList(),  // ← NUEVO
    val stagingId: String = UUID.randomUUID().toString(),      // ← NUEVO
    // UI
    val isLoading: Boolean = false,
    val error: String? = null
)

class AddVaccineViewModel : ViewModel() {

    private val _state = MutableStateFlow(AddVaccineFormState())
    val state: StateFlow<AddVaccineFormState> = _state.asStateFlow()

    fun loadCatalog(petSpecies: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCatalogLoading = true)
            RepositoryProvider.petRepository.getVaccineCatalog().fold(
                onSuccess = { all ->
                    val filtered = if (petSpecies.isBlank()) all
                    else all.filter { vaccine ->
                        vaccine.species.any { it.equals(petSpecies, ignoreCase = true) }
                    }
                    _state.value = _state.value.copy(
                        catalogVaccines  = filtered,
                        isCatalogLoading = false
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(isCatalogLoading = false)
                }
            )
        }
    }

    fun setPetId(v: String)             { _state.value = _state.value.copy(petId = v) }
    fun setSelectedVaccine(v: Vaccine?) { _state.value = _state.value.copy(selectedVaccine = v) }
    fun setDateGiven(v: String)         { _state.value = _state.value.copy(dateGiven = v) }
    fun setAdministeredBy(v: String)    { _state.value = _state.value.copy(administeredBy = v) }
    fun setLotNumber(v: String)         { _state.value = _state.value.copy(lotNumber = v) }
    fun setNextDueDate(v: String)       { _state.value = _state.value.copy(nextDueDate = v) }

    // ── Document staging ──────────────────────────────────────────────────

    fun addDocument(context: Context, uri: Uri, mimeType: String, fileName: String) {
        val petId     = _state.value.petId
        val stagingId = _state.value.stagingId

        val pending = StagedDocument(
            uri         = uri,
            fileName    = fileName,
            mimeType    = mimeType,
            isUploading = true
        )
        _state.value = _state.value.copy(
            stagedDocuments = _state.value.stagedDocuments + pending
        )

        viewModelScope.launch {
            FirebaseDocumentUploader
                .uploadVaccinationDocumentStaging(context, uri, petId, stagingId)
                .fold(
                    onSuccess = { uploaded ->
                        _state.value = _state.value.copy(
                            stagedDocuments = _state.value.stagedDocuments.map {
                                if (it.uri == uri && it.isUploading) {
                                    it.copy(
                                        downloadUrl = uploaded.downloadUrl,
                                        isUploading = false
                                    )
                                } else it
                            }
                        )
                    },
                    onFailure = { e ->
                        _state.value = _state.value.copy(
                            stagedDocuments = _state.value.stagedDocuments.map {
                                if (it.uri == uri && it.isUploading) {
                                    it.copy(isUploading = false, error = e.message)
                                } else it
                            }
                        )
                    }
                )
        }
    }

    fun removeDocument(doc: StagedDocument) {
        _state.value = _state.value.copy(
            stagedDocuments = _state.value.stagedDocuments.filter { it != doc }
        )
    }

    // ── Submit ────────────────────────────────────────────────────────────

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.petId.isBlank() || s.selectedVaccine == null || s.dateGiven.isBlank()) {
            _state.value = s.copy(error = "Pet, vaccine and date are required")
            return
        }
        if (s.stagedDocuments.any { it.isUploading }) {
            _state.value = s.copy(error = "Please wait for documents to finish uploading")
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)

            val request = AddVaccinationRequest(
                vaccineId      = s.selectedVaccine.id,
                dateGiven      = toIso(s.dateGiven),
                nextDueDate    = s.nextDueDate.takeIf { it.isNotBlank() }?.let { toIso(it) },
                lotNumber      = s.lotNumber.trim(),
                status         = "completed",
                administeredBy = s.administeredBy.trim()
            )

            RepositoryProvider.petRepository.addVaccination(s.petId, request).fold(
                onSuccess = { pet ->
                    // Buscar la vacunación recién creada (la última)
                    val newVaccinationId = pet.vaccinations.lastOrNull()?.id

                    if (newVaccinationId != null) {
                        val successfulDocs = s.stagedDocuments
                            .filter { it.downloadUrl != null && it.error == null }

                        successfulDocs.forEach { doc ->
                            RepositoryProvider.petRepository.addVaccinationDocument(
                                s.petId,
                                newVaccinationId,
                                AddDocumentRequest(
                                    fileName = doc.fileName,
                                    fileUri  = doc.downloadUrl
                                )
                            )
                        }
                    }

                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error     = e.message ?: "Error"
                    )
                }
            )
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun reset()      { _state.value = AddVaccineFormState() }

    private fun toIso(date: String): String = try {
        val p = date.split("/")
        if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}T00:00:00Z" else date
    } catch (_: Exception) { date }
}