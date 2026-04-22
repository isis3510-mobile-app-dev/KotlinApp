package com.example.petcare.ui.screens.nfc

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.nfc.NfcReadInspection
import com.example.petcare.data.model.Pet
import com.example.petcare.data.repository.NfcPetPayload
import com.example.petcare.data.repository.NfcRepository
import com.example.petcare.data.nfc.NfcManager
import com.example.petcare.data.nfc.NfcWriteInspection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.example.petcare.data.repository.INfcRepository
import com.example.petcare.data.repository.RepositoryProvider

// ── UI State ──────────────────────────────────────────────────────────────────

data class PetsUiState(
    val pets: List<Pet> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null // <--- ADD THIS LINE
)
sealed interface NfcUiState {
    object Idle : NfcUiState
    object Loading : NfcUiState
    object WaitingForTag : NfcUiState
    object ProcessingTag : NfcUiState
    data class WriteSuccess(val petName: String) : NfcUiState
    data class ReadSuccess(val payload: NfcPetPayload) : NfcUiState
    data class Error(val message: String) : NfcUiState
}

// ─────────────────────────────────────────────────────────────────────────────

class NfcViewModel(
    private val repository: INfcRepository = RepositoryProvider.nfcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NfcUiState>(NfcUiState.Idle)
    val uiState: StateFlow<NfcUiState> = _uiState.asStateFlow()

    // Write-flow context
    private var pendingPetId: String? = null
    private var pendingToken: String? = null
    private var pendingPayloadJson: String? = null
    private var readModeActive: Boolean = false

    // ── Write flow ────────────────────────────────────────────────────────────

    /**
     * Step 1 — Called when "Start Writing" is tapped.
     * With mocks: no real network call, goes straight a WaitingForTag.
     */
    fun prepareWrite(
        petId: String,
        firebaseToken: String
    ) {
        readModeActive = false
        _uiState.value = NfcUiState.Loading
        viewModelScope.launch {
            repository.fetchWritePayload(petId, firebaseToken).fold(
                onSuccess = { payload ->
                    val json = JSONObject().apply {
                        put("petId",      payload.petId)
                        put("petName",    payload.petName)
                        put("species",    payload.species)
                        put("breed",      payload.breed)
                        put("ownerName",  payload.ownerName)
                        put("ownerPhone", payload.ownerPhone)
                    }.toString()

                    pendingPetId       = petId
                    pendingToken       = firebaseToken
                    pendingPayloadJson = json
                    _uiState.value     = NfcUiState.WaitingForTag
                },
                onFailure = { e ->
                    _uiState.value = NfcUiState.Error(
                        e.message ?: "Failed to load pet data. Please try again."
                    )
                }
            )
        }
    }

    /**
     * Step 2 — Called from MainActivity.onNewIntent() when a physical tag
     * is detected while in write mode.
     */
    fun onTagDetectedForWrite(tag: Tag, nfcManager: NfcManager) {
        val petId   = pendingPetId       ?: return
        val token   = pendingToken       ?: return
        val payload = pendingPayloadJson ?: return

        when (nfcManager.inspectTagForWrite(tag, petId, payload)) {
            NfcWriteInspection.ReadOnlyTag -> {
                _uiState.value = NfcUiState.Error("This NFC tag is read-only.")
                return
            }
            NfcWriteInspection.IncompatibleTag -> {
                _uiState.value = NfcUiState.Error("This NFC tag is not compatible with PetCare.")
                return
            }
            is NfcWriteInspection.CapacityTooSmall -> {
                _uiState.value = NfcUiState.Error("This NFC tag is too small for PetCare data.")
                return
            }
            NfcWriteInspection.ReadyToWrite -> Unit
        }

        _uiState.value = NfcUiState.ProcessingTag
        viewModelScope.launch {
            nfcManager.writeTag(tag, petId, payload).fold(
                onSuccess = {
                    // Fire-and-forget — mock returns success immediately
                    repository.markNfcSynced(petId, token)

                    val name = runCatching {
                        JSONObject(payload).optString("petName", "your pet")
                    }.getOrDefault("your pet")

                    clearPendingWrite()
                    _uiState.value = NfcUiState.WriteSuccess(name)
                },
                onFailure = { e ->
                    // On write failure, keep pending state so the user can
                    // try again with the same tag without re-tapping "Start Writing"
                    _uiState.value = NfcUiState.Error(nfcManager.writeErrorMessage(e))
                }
            )
        }
    }

    // ── Read flow ─────────────────────────────────────────────────────────────

    /**
     * Transitions to WaitingForTag for read mode.
     * Called when "Start Scanning" is tapped.
     */
    fun startReadMode(nfcManager: NfcManager) {
        if (!nfcManager.isNfcSupported) {
            _uiState.value = NfcUiState.Error("NFC is not supported on this device")
            return
        }
        if (!nfcManager.isNfcEnabled) {
            _uiState.value = NfcUiState.Error("Please enable NFC in your device settings")
            return
        }
        clearPendingWrite()
        readModeActive = true
        _uiState.value = NfcUiState.WaitingForTag
    }


    /**
     * Called from MainActivity.onNewIntent() when a tag is detected in read mode.
     * Reads the petId from the tag, then fetches public info (mocked).
     */
    fun onTagDetectedForRead(tag: Tag, nfcManager: NfcManager) {
        if (!readModeActive) return
        _uiState.value = NfcUiState.ProcessingTag
        viewModelScope.launch {
            when (val inspection = nfcManager.inspectTagForRead(tag)) {
                NfcReadInspection.NonPetCareTag -> {
                    _uiState.value = NfcUiState.Error("Tag detected, but it isn't a PetCare tag.")
                }
                is NfcReadInspection.PetCareTag -> {
                    repository.fetchPublicPetInfo(inspection.petId).fold(
                        onSuccess = { payload ->
                            readModeActive = false
                            _uiState.value = NfcUiState.ReadSuccess(payload)
                        },
                        onFailure = { e ->
                            _uiState.value = NfcUiState.Error(
                                e.message ?: "Could not load pet information."
                            )
                        }
                    )
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** True when a write payload is staged and we're waiting for a tag tap. */
    fun isPendingWrite(): Boolean = pendingPetId != null

    /**
     * Resets to Idle and clears any pending write state.
     *
     * For multiple test cycles: after a WriteSuccess or ReadSuccess, the
     * UI navigates away and the user can come back and start again.
     * This is called from the Back button and the Cancel button inside
     * ScanningNFCScreen, so state never gets stuck.
     */
    fun reset() {
        clearPendingWrite()
        readModeActive = false
        _uiState.value = NfcUiState.Idle
    }

    fun resetSession() = reset()

    /**
     * Called after a WriteSuccess or Error to allow retrying the write
     * on the same tag WITHOUT going back to WriteNFCScreen.
     *
     * Usage: show a "Try again" button in the error state inside
     * ScanningNFCScreen when isPendingWrite() is still true.
     */
    fun retryWrite() {
        if (pendingPetId != null) {
            _uiState.value = NfcUiState.WaitingForTag
        }
    }

    fun retryRead() {
        if (readModeActive) {
            _uiState.value = NfcUiState.WaitingForTag
        }
    }

    /**
     * Called after a ReadSuccess to scan another tag without navigating away.
     * Goes back to WaitingForTag so the next tap triggers a new read.
     */
    fun readAnother() {
        readModeActive = true
        _uiState.value = NfcUiState.WaitingForTag
    }

    fun isReadyForReadTag(): Boolean =
        readModeActive && _uiState.value is NfcUiState.WaitingForTag

    fun isReadModeActive(): Boolean = readModeActive

    private fun clearPendingWrite() {
        pendingPetId       = null
        pendingToken       = null
        pendingPayloadJson = null
    }



}
