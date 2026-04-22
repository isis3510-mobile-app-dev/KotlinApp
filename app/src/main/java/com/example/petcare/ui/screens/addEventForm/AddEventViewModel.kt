package com.example.petcare.ui.screens.addEventForm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.EventType
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.ui.navigation.Routes
import com.example.petcare.util.EventDateUtils
import com.example.petcare.util.FirebaseDocumentUploader
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.normalizeForCommit
import com.example.petcare.util.sanitizeForEditing
import com.example.petcare.util.trimToNullIfBlank
import com.example.petcare.util.validateCommittedInput
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
    val originRoute: String = Routes.Records,
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

    fun setPetId(v: String)            { _state.value = _state.value.copy(petId = normalizeForCommit(v, InputFieldPolicy.GENERAL_TEXT)) }
    fun setOwnerId(v: String)          { _state.value = _state.value.copy(ownerId = normalizeForCommit(v, InputFieldPolicy.GENERAL_TEXT)) }
    fun setOriginRoute(v: String)      { _state.value = _state.value.copy(originRoute = v) }
    fun setTitle(v: String)            { _state.value = _state.value.copy(title = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.EVENT_TITLE).value) }
    fun setDate(v: String)             { _state.value = _state.value.copy(date = v) }
    fun setTime(v: String)             { _state.value = _state.value.copy(time = v) }
    fun setEventType(v: EventType)     { _state.value = _state.value.copy(eventType = v) }
    fun setDescription(v: String)      { _state.value = _state.value.copy(description = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.NOTES).value) }
    fun setPrice(v: String)            { _state.value = _state.value.copy(price = sanitizeForEditing(v, InputFieldPolicy.DECIMAL, InputTextLimits.PRICE).value) }
    fun setProvider(v: String)         { _state.value = _state.value.copy(provider = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value) }
    fun setClinic(v: String)           { _state.value = _state.value.copy(clinic = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value) }
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
        val titleError = validateCommittedInput(s.title, InputFieldPolicy.GENERAL_TEXT, required = true, maxLength = InputTextLimits.EVENT_TITLE, fieldName = "Title")
        val descriptionError = validateCommittedInput(s.description, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.NOTES)
        val providerError = validateCommittedInput(s.provider, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.PROVIDER_OR_CLINIC)
        val clinicError = validateCommittedInput(s.clinic, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.PROVIDER_OR_CLINIC)
        val priceError = validateCommittedInput(s.price, InputFieldPolicy.DECIMAL, maxLength = InputTextLimits.PRICE, fieldName = "Price")

        val firstError = listOfNotNull(
            if (s.petId.isBlank() || s.ownerId.isBlank()) "Pet and user profile are required." else null,
            if (s.date.isBlank()) "Date is required." else null,
            titleError,
            descriptionError,
            providerError,
            clinicError,
            priceError
        ).firstOrNull()

        if (firstError != null) {
            _state.value = s.copy(error = firstError)
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
                title        = normalizeForCommit(s.title, InputFieldPolicy.GENERAL_TEXT),
                eventType    = s.eventType.name.lowercase(),
                date         = toIso(s.date, s.time),
                price        = normalizeForCommit(s.price, InputFieldPolicy.DECIMAL).trimToNullIfBlank()?.toDoubleOrNull(),
                provider     = normalizeForCommit(s.provider, InputFieldPolicy.GENERAL_TEXT),
                clinic       = normalizeForCommit(s.clinic, InputFieldPolicy.GENERAL_TEXT),
                description  = normalizeForCommit(s.description, InputFieldPolicy.GENERAL_TEXT),
                followUpDate = s.followUpDate
                    .takeIf { it.isNotBlank() }
                    ?.let { toIso(it, "12:00 AM") }
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

    private fun toIso(date: String, time: String): String =
        EventDateUtils.toIsoFromAppDateTime(
            appDate = date,
            appTime = time
        ) ?: date
}
