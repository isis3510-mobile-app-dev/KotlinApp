package com.example.petcare.ui.screens.petprofile.vaccines

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.AddDocumentRequest
import com.example.petcare.data.model.AttachedDocument
import com.example.petcare.data.network.isOnline
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineFilterStatus
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineRecord
import com.example.petcare.util.FirebaseDocumentUploader
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.PicassoImageCompressor
import com.example.petcare.util.normalizeForCommit
import com.example.petcare.util.sanitizeForEditing
import com.example.petcare.util.trimToNullIfBlank
import com.example.petcare.util.validateCommittedInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VaccineDetailsUiState(
    val vaccine: VaccineRecord? = null,
    val petId: String = "",
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingDoc: Boolean = false,      // ← NUEVO
    val error: String? = null,
    val isDeleted: Boolean = false,
    val isEditing: Boolean = false,
    val editAdministeredBy: String = "",
    val editNextDueDate: String = "",
    val editLotNumber: String = ""
)

class VaccineDetailsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VaccineDetailsUiState(isLoading = true))
    val uiState: StateFlow<VaccineDetailsUiState> = _uiState.asStateFlow()

    // ── Load ──────────────────────────────────────────────────────────────

    fun load(petId: String, vaccineId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, petId = petId, error = null)
            RepositoryProvider.petRepository.syncPendingVaccinationDocuments(petId, vaccineId)
                .onSuccess { synced ->
                    if (synced > 0) {
                        Log.d(TAG, "Synced pending documents before load count=$synced vaccinationId=$vaccineId")
                    }
                }

            val catalogMap = RepositoryProvider.petRepository
                .getVaccineCatalog()
                .getOrElse { emptyList() }
                .associateBy { it.id }

            RepositoryProvider.petRepository.getPet(petId).fold(
                onSuccess = { pet ->
                    val vacc = pet.vaccinations.find { it.id == vaccineId }
                    if (vacc == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error     = "Vaccination not found"
                        )
                        return@fold
                    }

                    val status = when (vacc.status.lowercase()) {
                        "overdue"  -> VaccineFilterStatus.OVERDUE
                        "upcoming" -> VaccineFilterStatus.UPCOMING
                        else       -> VaccineFilterStatus.COMPLETED
                    }

                    val record = VaccineRecord(
                        id                = vacc.id,
                        name              = resolveVaccineName(vacc.vaccineId, catalogMap),
                        provider          = vacc.administeredBy,
                        dateGiven         = vacc.dateGiven.take(10),
                        nextDueDate       = vacc.nextDueDate?.take(10),
                        lotNumber         = vacc.lotNumber.ifBlank { null },
                        status            = status,
                        attachedDocuments = vacc.attachedDocuments.map { doc ->
                            AttachedDocument(
                                id       = doc.id,
                                fileName = doc.fileName,
                                fileUri  = doc.fileUri
                            )
                        } + pendingAttachedDocuments(pet.id, vacc.id)
                    )

                    _uiState.value = _uiState.value.copy(
                        vaccine            = record,
                        isLoading          = false,
                        editAdministeredBy = sanitizeForEditing(vacc.administeredBy, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value,
                        editNextDueDate    = vacc.nextDueDate?.take(10) ?: "",
                        editLotNumber      = sanitizeForEditing(vacc.lotNumber, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.LOT_NUMBER).value
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error     = e.message
                    )
                }
            )
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────

    fun deleteVaccine() {
        val petId         = _uiState.value.petId
        val vaccinationId = _uiState.value.vaccine?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            FeatureExecutionTracker.track("Delete Vaccination") {
                RepositoryProvider.petRepository.deleteVaccination(
                    petId         = petId,
                    vaccinationId = vaccinationId
                )
            }.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        isDeleted  = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        error      = e.message
                    )
                }
            )
        }
    }

    // ── Edit ──────────────────────────────────────────────────────────────

    fun startEditing()  { _uiState.value = _uiState.value.copy(isEditing = true,  error = null) }
    fun cancelEditing() { _uiState.value = _uiState.value.copy(isEditing = false, error = null) }

    fun setAdministeredBy(v: String) {
        _uiState.value = _uiState.value.copy(
            editAdministeredBy = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value
        )
    }
    fun setNextDueDate(v: String) {
        _uiState.value = _uiState.value.copy(editNextDueDate = v)
    }
    fun setLotNumber(v: String) {
        _uiState.value = _uiState.value.copy(
            editLotNumber = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.LOT_NUMBER).value
        )
    }

    fun saveEdits() {
        val petId         = _uiState.value.petId
        val vaccinationId = _uiState.value.vaccine?.id ?: return
        val s             = _uiState.value
        val validationMessage = listOfNotNull(
            validateCommittedInput(s.editAdministeredBy, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.PROVIDER_OR_CLINIC),
            validateCommittedInput(s.editLotNumber, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.LOT_NUMBER)
        ).firstOrNull()
        if (validationMessage != null) {
            _uiState.value = s.copy(error = validationMessage)
            return
        }
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true, error = null)
            FeatureExecutionTracker.track("Edit Vaccination") {
                RepositoryProvider.petRepository.updateVaccination(
                    petId          = petId,
                    vaccinationId  = vaccinationId,
                    administeredBy = normalizeForCommit(s.editAdministeredBy, InputFieldPolicy.GENERAL_TEXT),
                    nextDueDate    = s.editNextDueDate.takeIf { it.isNotBlank() },
                    lotNumber      = normalizeForCommit(s.editLotNumber, InputFieldPolicy.GENERAL_TEXT)
                )
            }.fold(
                onSuccess = { _ ->
                    _uiState.value = _uiState.value.copy(
                        isSaving  = false,
                        isEditing = false,
                        vaccine   = _uiState.value.vaccine?.copy(
                            provider    = normalizeForCommit(s.editAdministeredBy, InputFieldPolicy.GENERAL_TEXT),
                            nextDueDate = s.editNextDueDate.takeIf { it.isNotBlank() },
                            lotNumber   = normalizeForCommit(s.editLotNumber, InputFieldPolicy.GENERAL_TEXT).trimToNullIfBlank()
                        )
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error    = e.message
                    )
                }
            )
        }
    }

    // ── Documents ─────────────────────────────────────────────────────────

    fun addDocument(
        context: Context,
        uri: Uri,
        mimeType: String? = null,
        fileName: String? = null
    ) {
        val petId         = _uiState.value.petId
        val vaccinationId = _uiState.value.vaccine?.id ?: return
        Log.d(TAG, "Detail document upload requested petId=$petId vaccinationId=$vaccinationId")
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Detail upload coroutine started thread=${Thread.currentThread().name}")
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isUploadingDoc = true, error = null)
                Log.d(TAG, "Detail upload UI loading state set on ${Thread.currentThread().name}")
            }
            val resolvedMimeType = mimeType ?: context.contentResolver.getType(uri) ?: "application/octet-stream"
            val resolvedFileName = fileName ?: FirebaseDocumentUploader.getFileName(context, uri)
                ?: "document_${System.currentTimeMillis()}"

            if (!isOnline(context)) {
                queuePendingDocument(context, uri, petId, vaccinationId, resolvedFileName, resolvedMimeType)
                return@launch
            }

            val prepared = try {
                async(Dispatchers.IO) {
                    PicassoImageCompressor.prepareImageIfNeeded(context, uri, resolvedMimeType, resolvedFileName)
                }.await()
            } catch (e: Exception) {
                Log.e(TAG, "Detail document preparation failed fileName=$resolvedFileName: ${e.message}", e)
                queuePendingDocument(context, uri, petId, vaccinationId, resolvedFileName, resolvedMimeType)
                return@launch
            }
            Log.d(TAG, "Detail document prepared original=$resolvedFileName prepared=${prepared.fileName}")

            FirebaseDocumentUploader
                .uploadVaccinationDocument(context, prepared.uri, petId, vaccinationId)
                .fold(
                    onSuccess = { uploaded ->
                        Log.d(TAG, "Detail Firebase upload success fileName=${uploaded.fileName}")
                        RepositoryProvider.petRepository.addVaccinationDocument(
                            petId,
                            vaccinationId,
                            AddDocumentRequest(
                                fileName = prepared.fileName,
                                fileUri  = uploaded.downloadUrl
                            )
                        ).fold(
                            onSuccess = { updatedPet ->
                                Log.d(TAG, "Detail backend metadata saved vaccinationId=$vaccinationId")
                                val updatedVacc = updatedPet.vaccinations
                                    .find { it.id == vaccinationId }
                                withContext(Dispatchers.Main) {
                                    _uiState.value = _uiState.value.copy(
                                        isUploadingDoc = false,
                                        vaccine = _uiState.value.vaccine?.copy(
                                            attachedDocuments = updatedVacc
                                                ?.attachedDocuments
                                                ?.map { doc ->
                                                    AttachedDocument(
                                                        id       = doc.id,
                                                        fileName = doc.fileName,
                                                        fileUri  = doc.fileUri
                                                    )
                                                } ?: emptyList()
                                        )
                                    )
                                    Log.d(TAG, "Detail document UI state updated on ${Thread.currentThread().name}")
                                }
                            },
                            onFailure = { e ->
                                Log.e(TAG, "Detail backend metadata failed vaccinationId=$vaccinationId: ${e.message}", e)
                                withContext(Dispatchers.Main) {
                                    _uiState.value = _uiState.value.copy(
                                        isUploadingDoc = false,
                                        error = "Uploaded but failed to save: ${e.message}"
                                    )
                                }
                            }
                        )
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Detail Firebase upload failed vaccinationId=$vaccinationId: ${e.message}", e)
                        queuePendingDocument(context, uri, petId, vaccinationId, resolvedFileName, resolvedMimeType)
                    }
                )
        }
    }

    fun deleteDocument(petId: String, vaccinationId: String, documentId: String) {
        if (documentId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "This document cannot be deleted yet because it has no server id."
            )
            return
        }
        viewModelScope.launch {
            RepositoryProvider.petRepository.deleteVaccinationDocument(petId, vaccinationId, documentId)
                .onSuccess { updatedPet ->
                    val updatedVacc = updatedPet.vaccinations.find { it.id == vaccinationId }
                    _uiState.value = _uiState.value.copy(
                        vaccine = _uiState.value.vaccine?.copy(
                            attachedDocuments = updatedVacc?.attachedDocuments?.map { doc ->
                                AttachedDocument(id = doc.id, fileName = doc.fileName, fileUri = doc.fileUri)
                            } ?: emptyList()
                        )
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    private fun resolveVaccineName(
        vaccineId: String?,
        catalogMap: Map<String, com.example.petcare.data.model.Vaccine>
    ): String {
        val fromCatalog = vaccineId
            ?.trim()
            ?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
            ?.let { catalogMap[it]?.name }
            ?.trim()
            .takeUnless { it.isNullOrBlank() }

        return fromCatalog ?: "Unknown vaccine"
    }

    private fun pendingAttachedDocuments(
        petId: String,
        vaccinationId: String
    ): List<AttachedDocument> =
        RepositoryProvider.petRepository
            .getPendingVaccinationDocuments(petId, vaccinationId)
            .map {
                AttachedDocument(
                    id = "pending_${it.id}",
                    fileName = "${it.fileName} (pending sync)",
                    fileUri = it.localUri
                )
            }

    private suspend fun queuePendingDocument(
        context: Context,
        uri: Uri,
        petId: String,
        vaccinationId: String,
        fileName: String,
        mimeType: String
    ) {
        RepositoryProvider.petRepository
            .queueVaccinationDocument(uri, petId, vaccinationId, fileName, mimeType)
            .fold(
                onSuccess = { pending ->
                    Log.d(TAG, "Detail document queued locally id=${pending.id} fileName=${pending.fileName}")
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isUploadingDoc = false,
                            error = "No internet connection. Document saved locally and will sync when online.",
                            vaccine = _uiState.value.vaccine?.copy(
                                attachedDocuments = _uiState.value.vaccine?.attachedDocuments.orEmpty() +
                                    AttachedDocument(
                                        id = "pending_${pending.id}",
                                        fileName = "${pending.fileName} (pending sync)",
                                        fileUri = pending.localUri
                                    )
                            )
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Detail document local queue failed fileName=$fileName: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isUploadingDoc = false,
                            error = "Could not save document locally: ${e.message}"
                        )
                    }
                }
            )
    }

    private companion object {
        const val TAG = "DOC_UPLOAD"
    }
}
