package com.example.petcare.ui.screens.nfc

import android.app.Application
import android.content.Context
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.model.Pet
import com.example.petcare.data.nfc.NfcManager
import com.example.petcare.data.nfc.NfcWriteInspection
import com.example.petcare.data.repository.INfcRepository
import com.example.petcare.data.repository.NfcPetPayload
import com.example.petcare.data.repository.RepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PetsUiState(
    val pets: List<Pet> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
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

class NfcViewModel(
    application: Application,
    private val repository: INfcRepository = RepositoryProvider.nfcRepository
) : AndroidViewModel(application) {

    companion object {
        private const val NFC_CACHE_PREFS = "nfc_cache_prefs"
        private const val NFC_CACHE_PREFIX = "write_payload_"
    }

    private val _uiState = MutableStateFlow<NfcUiState>(NfcUiState.Idle)
    val uiState: StateFlow<NfcUiState> = _uiState.asStateFlow()

    private var pendingPetId: String? = null
    private var pendingToken: String? = null
    private var pendingPayloadJson: String? = null
    private var readModeActive: Boolean = false

    fun prepareWrite(
        petId: String,
        firebaseToken: String
    ) {
        readModeActive = false
        _uiState.value = NfcUiState.Loading

        viewModelScope.launch {
            val payload = loadBestWritePayload(petId, firebaseToken)
            payload.fold(
                onSuccess = { resolved ->
                    val json = payloadToTagJson(resolved)
                    pendingPetId = petId
                    pendingToken = firebaseToken
                    pendingPayloadJson = json
                    cacheWritePayload(resolved)
                    _uiState.value = NfcUiState.WaitingForTag
                },
                onFailure = { e ->
                    _uiState.value = NfcUiState.Error(
                        e.message ?: "Failed to load pet data. Please try again."
                    )
                }
            )
        }
    }

    fun onTagDetectedForWrite(tag: Tag, nfcManager: NfcManager) {
        val petId = pendingPetId ?: return
        val token = pendingToken ?: ""
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
                    // Best effort sync; do not block offline flow on backend connectivity.
                    repository.markNfcSynced(petId, token)
                    val name = runCatching {
                        JSONObject(payload).optString("petName", "your pet")
                    }.getOrDefault("your pet")

                    clearPendingWrite()
                    _uiState.value = NfcUiState.WriteSuccess(name)
                },
                onFailure = { e ->
                    _uiState.value = NfcUiState.Error(nfcManager.writeErrorMessage(e))
                }
            )
        }
    }

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

    fun onTagDetectedForRead(tag: Tag, nfcManager: NfcManager) {
        if (!readModeActive) return
        _uiState.value = NfcUiState.ProcessingTag

        viewModelScope.launch(Dispatchers.IO) {
            val rawPayload = nfcManager.readRawPayloadFromTag(tag)
            val petId = nfcManager.readPetIdFromTag(tag)

            if (petId.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    _uiState.value = NfcUiState.Error(
                        "This NFC tag doesn't contain a valid PetCare payload.\n" +
                            "Make sure it was written with the current app format."
                    )
                }
                return@launch
            }

            val fallbackFromTag = rawPayload?.let(::payloadFromTagJson)
            val backend = repository.fetchPublicPetInfo(petId)

            withContext(Dispatchers.Main) {
                backend.fold(
                    onSuccess = { payload ->
                        cacheWritePayload(payload)
                        _uiState.value = NfcUiState.ReadSuccess(payload)
                    },
                    onFailure = { e ->
                        if (fallbackFromTag != null) {
                            cacheWritePayload(fallbackFromTag)
                            _uiState.value = NfcUiState.ReadSuccess(fallbackFromTag)
                        } else {
                            _uiState.value = NfcUiState.Error(
                                e.message ?: "Could not load pet information."
                            )
                        }
                    }
                )
            }
        }
    }

    fun isPendingWrite(): Boolean = pendingPetId != null

    fun reset() {
        clearPendingWrite()
        readModeActive = false
        _uiState.value = NfcUiState.Idle
    }

    fun resetSession() = reset()

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

    fun readAnother() {
        readModeActive = true
        _uiState.value = NfcUiState.WaitingForTag
    }

    fun isReadyForReadTag(): Boolean =
        readModeActive && _uiState.value is NfcUiState.WaitingForTag

    fun isReadModeActive(): Boolean = readModeActive

    private suspend fun loadBestWritePayload(
        petId: String,
        firebaseToken: String
    ): Result<NfcPetPayload> {
        val remote = repository.fetchWritePayload(petId, firebaseToken)
        if (remote.isSuccess) return remote

        loadCachedWritePayload(petId)?.let { return Result.success(it) }
        loadLocalWritePayloadFromRoom(petId)?.let { return Result.success(it) }

        return Result.failure(
            remote.exceptionOrNull()
                ?: IllegalStateException("No payload available for offline NFC write.")
        )
    }

    private suspend fun loadLocalWritePayloadFromRoom(petId: String): NfcPetPayload? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val localPet = AppDatabase.getInstance(getApplication())
            .petDao()
            .getPetById(uid, petId)
            ?: return null

        val ownerName = FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()
        val ownerPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber.orEmpty()

        return NfcPetPayload(
            petId = localPet.id,
            petName = localPet.name,
            species = localPet.species,
            breed = localPet.breed,
            ownerName = ownerName,
            ownerPhone = ownerPhone,
            ownerInitials = ownerName.trim()
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() },
            photoUrl = localPet.photoUrl.orEmpty(),
            status = localPet.status,
            appDeepLink = "petcare://pet/${localPet.id}",
            knownAllergies = localPet.knownAllergies,
            defaultVet = localPet.defaultVet,
            defaultClinic = localPet.defaultClinic
        )
    }

    private fun payloadToTagJson(payload: NfcPetPayload): String {
        return JSONObject().apply {
            put("petId", payload.petId)
            put("petName", payload.petName)
            put("species", payload.species)
            put("breed", payload.breed)
            put("ownerName", payload.ownerName)
            put("ownerPhone", payload.ownerPhone)
            put("ownerInitials", payload.ownerInitials)
            put("photoUrl", payload.photoUrl)
            put("status", payload.status)
            put("knownAllergies", payload.knownAllergies)
            put("defaultVet", payload.defaultVet)
            put("defaultClinic", payload.defaultClinic)
        }.toString()
    }

    private fun payloadFromTagJson(raw: String): NfcPetPayload? {
        return runCatching {
            val json = JSONObject(raw)
            val petId = json.optString("petId").ifBlank { json.optString("pet_id") }
            if (petId.isBlank()) return null

            NfcPetPayload(
                petId = petId,
                petName = json.optString("petName"),
                species = json.optString("species"),
                breed = json.optString("breed"),
                ownerName = json.optString("ownerName"),
                ownerPhone = json.optString("ownerPhone"),
                ownerInitials = json.optString("ownerInitials"),
                photoUrl = json.optString("photoUrl"),
                status = json.optString("status", "Unknown"),
                appDeepLink = "petcare://pet/$petId",
                knownAllergies = json.optString("knownAllergies"),
                defaultVet = json.optString("defaultVet"),
                defaultClinic = json.optString("defaultClinic")
            )
        }.getOrNull()
    }

    private fun cacheWritePayload(payload: NfcPetPayload) {
        getPrefs().edit()
            .putString("$NFC_CACHE_PREFIX${payload.petId}", payloadToTagJson(payload))
            .apply()
    }

    private fun loadCachedWritePayload(petId: String): NfcPetPayload? {
        val raw = getPrefs().getString("$NFC_CACHE_PREFIX$petId", null) ?: return null
        return payloadFromTagJson(raw)
    }

    private fun getPrefs() = getApplication<Application>()
        .getSharedPreferences(NFC_CACHE_PREFS, Context.MODE_PRIVATE)

    private fun clearPendingWrite() {
        pendingPetId = null
        pendingToken = null
        pendingPayloadJson = null
    }

    class NfcViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NfcViewModel::class.java)) {
                return NfcViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
