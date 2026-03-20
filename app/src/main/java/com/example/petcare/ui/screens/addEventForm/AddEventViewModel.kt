package com.example.petcare.ui.screens.addEventForm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.EventType
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.util.FirebaseDocumentUploader
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.enforceMaxLength
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class StagedDocument(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    // Se llena después de subir a staging
    val downloadUrl: String? = null,
    val isUploading: Boolean = false,
    val error: String? = null
)

data class AddEventFormState(
    val petId: String   = "",
    val ownerId: String = "",
    // Step 1
    val title: String        = "",
    val date: String         = "",
    val time: String         = "",
    val eventType: EventType = EventType.CHECKUP,
    // Step 2
    val description: String = "",
    val price: String       = "",
    val provider: String    = "",
    val clinic: String      = "",
    // Step 3
    val followUpDate: String         = "",
    val reminderEnabled: Boolean     = false,
    val stagedDocuments: List<StagedDocument> = emptyList(),  // ← NUEVO
    val stagingId: String            = UUID.randomUUID().toString(), // ← NUEVO
    // UI
    val isLoading: Boolean = false,
    val error: String?     = null
)

class AddEventViewModel : ViewModel() {

    private val _state = MutableStateFlow(AddEventFormState())
    val state: StateFlow<AddEventFormState> = _state.asStateFlow()

    fun setPetId(v: String)            { _state.value = _state.value.copy(petId = v) }
    fun setOwnerId(v: String)          { _state.value = _state.value.copy(ownerId = v) }
    fun setTitle(v: String)            { _state.value = _state.value.copy(title = enforceMaxLength(v, InputTextLimits.EVENT_TITLE)) }
    fun setDate(v: String)             { _state.value = _state.value.copy(date = v) }
    fun setTime(v: String)             { _state.value = _state.value.copy(time = v) }
    fun setEventType(v: EventType)     { _state.value = _state.value.copy(eventType = v) }
    fun setDescription(v: String)      { _state.value = _state.value.copy(description = enforceMaxLength(v, InputTextLimits.NOTES)) }
    fun setPrice(v: String)            { _state.value = _state.value.copy(price = v) }
    fun setProvider(v: String)         { _state.value = _state.value.copy(provider = enforceMaxLength(v, InputTextLimits.PROVIDER_OR_CLINIC)) }
    fun setClinic(v: String)           { _state.value = _state.value.copy(clinic = enforceMaxLength(v, InputTextLimits.PROVIDER_OR_CLINIC)) }
    fun setFollowUpDate(v: String)     { _state.value = _state.value.copy(followUpDate = v) }
    fun setReminderEnabled(v: Boolean) { _state.value = _state.value.copy(reminderEnabled = v) }

    // ── Document staging ──────────────────────────────────────────────────

    fun addDocument(context: Context, uri: Uri, mimeType: String, fileName: String) {
        val petId     = _state.value.petId
        val stagingId = _state.value.stagingId

        // Agrega el doc como "uploading" inmediatamente para feedback visual
        val pending = StagedDocument(
            uri        = uri,
            fileName   = fileName,
            mimeType   = mimeType,
            isUploading = true
        )
        _state.value = _state.value.copy(
            stagedDocuments = _state.value.stagedDocuments + pending
        )

        viewModelScope.launch {
            FirebaseDocumentUploader
                .uploadEventDocumentStaging(context, uri, petId, stagingId)
                .fold(
                    onSuccess = { uploaded ->
                        // Reemplaza el pending con el doc completo
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

    fun submit(onSuccess: (eventId: String) -> Unit) {
        val s = _state.value
        if (s.petId.isBlank() || s.title.isBlank() || s.date.isBlank()) {
            _state.value = s.copy(error = "Pet, title and date are required")
            return
        }
        // Esperar a que terminen los uploads pendientes
        if (s.stagedDocuments.any { it.isUploading }) {
            _state.value = s.copy(error = "Please wait for documents to finish uploading")
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)

            val request = CreateEventRequest(
                petId        = s.petId,
                ownerId      = s.ownerId,
                title        = s.title.trim(),
                eventType    = s.eventType.name.lowercase(),
                date         = toIso(s.date),
                price        = s.price.toDoubleOrNull(),
                provider     = s.provider.trim(),
                clinic       = s.clinic.trim(),
                description  = s.description.trim(),
                followUpDate = s.followUpDate
                    .takeIf { it.isNotBlank() }
                    ?.let { toIso(it) }
            )

            FeatureExecutionTracker.track("Create Event") {
                RepositoryProvider.eventRepository.createEvent(request)
            }.fold(
                onSuccess = { event ->
                    // Registrar documentos staged en el backend
                    val successfulDocs = s.stagedDocuments
                        .filter { it.downloadUrl != null && it.error == null }

                    successfulDocs.forEach { doc ->
                        RepositoryProvider.eventRepository.addDocument(
                            eventId  = event.id,
                            fileName = doc.fileName,
                            fileUri  = doc.downloadUrl
                        )
                    }

                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess(event.id)
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
    fun reset()      { _state.value = AddEventFormState() }

    private fun toIso(date: String): String = try {
        val p = date.split("/")
        if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}T00:00:00Z" else date
    } catch (_: Exception) { date }
}
