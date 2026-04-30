package com.example.petcare.ui.screens.addVaccineForm

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.AddDocumentRequest
import com.example.petcare.data.model.AddVaccinationRequest
import com.example.petcare.data.model.Vaccine
import com.example.petcare.data.network.isOnline
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.ui.screens.addEventForm.StagedDocument
import com.example.petcare.util.FirebaseDocumentUploader
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.PicassoImageCompressor
import com.example.petcare.util.normalizeForCommit
import com.example.petcare.util.sanitizeForEditing
import com.example.petcare.util.validateCommittedInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID

data class AddVaccineFormState(
    val petId: String = "",
    val petBirthDateIso: String? = null,
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
            Log.d(TAG, "loadCatalog start species=$petSpecies")
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
                    Log.d(TAG, "loadCatalog success total=${all.size} filtered=${filtered.size}")
                },
                onFailure = {
                    Log.e(TAG, "loadCatalog failed: ${it.message}", it)
                    _state.value = _state.value.copy(isCatalogLoading = false)
                }
            )
        }
    }

    fun setPetId(v: String) {
        _state.value = _state.value.copy(petId = v)
        if (v.isNotBlank()) {
            viewModelScope.launch {
                RepositoryProvider.petRepository.getPet(v).onSuccess { pet ->
                    _state.value = _state.value.copy(petBirthDateIso = pet.birthDate)
                }
            }
        }
    }
    fun setSelectedVaccine(v: Vaccine?) { _state.value = _state.value.copy(selectedVaccine = v) }
    fun setDateGiven(v: String)         { _state.value = _state.value.copy(dateGiven = v) }
    fun setAdministeredBy(v: String)    { _state.value = _state.value.copy(administeredBy = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value) }
    fun setLotNumber(v: String)         { _state.value = _state.value.copy(lotNumber = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.LOT_NUMBER).value) }
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
        Log.d(
            TAG,
            "Queued vaccination staging document petId=$petId stagingId=$stagingId fileName=$fileName mimeType=$mimeType"
        )

        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Staging coroutine started thread=${Thread.currentThread().name}")
            try {
                if (!isOnline(context)) {
                    val local = copyStagedDocumentLocally(context, uri, fileName)
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            stagedDocuments = _state.value.stagedDocuments.map {
                                if (it.uri == uri && it.isUploading) {
                                    it.copy(
                                        uri = local,
                                        downloadUrl = local.toString(),
                                        isUploading = false
                                    )
                                } else it
                            }
                        )
                    }
                    Log.d(TAG, "Staging saved locally offline fileName=$fileName uri=$local")
                    return@launch
                }

                val prepared = async(Dispatchers.IO) {
                    PicassoImageCompressor.prepareImageIfNeeded(context, uri, mimeType, fileName)
                }.await()
                Log.d(
                    TAG,
                    "Prepared staging document original=$fileName prepared=${prepared.fileName} mimeType=${prepared.mimeType}"
                )

                FeatureExecutionTracker.track("Upload Vaccine Attachment") {
                    FirebaseDocumentUploader.uploadVaccinationDocumentStaging(context, prepared.uri, petId, stagingId)
                }.fold(
                        onSuccess = { uploaded ->
                            Log.d(
                                TAG,
                                "Staging upload success petId=$petId stagingId=$stagingId fileName=${uploaded.fileName}"
                            )
                            withContext(Dispatchers.Main) {
                                _state.value = _state.value.copy(
                                    stagedDocuments = _state.value.stagedDocuments.map {
                                        if (it.uri == uri && it.isUploading) {
                                            it.copy(
                                                fileName = prepared.fileName,
                                                mimeType = prepared.mimeType,
                                                downloadUrl = uploaded.downloadUrl,
                                                isUploading = false
                                            )
                                        } else it
                                    }
                                )
                                Log.d(TAG, "Staging document UI state updated on ${Thread.currentThread().name}")
                            }
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Staging upload failed petId=$petId stagingId=$stagingId: ${e.message}", e)
                            runCatching {
                                val local = copyStagedDocumentLocally(context, prepared.uri, prepared.fileName)
                                withContext(Dispatchers.Main) {
                                    _state.value = _state.value.copy(
                                        stagedDocuments = _state.value.stagedDocuments.map {
                                            if (it.uri == uri && it.isUploading) {
                                                it.copy(
                                                    uri = local,
                                                    fileName = prepared.fileName,
                                                    mimeType = prepared.mimeType,
                                                    downloadUrl = local.toString(),
                                                    isUploading = false,
                                                    error = null
                                                )
                                            } else it
                                        }
                                    )
                                }
                                Log.d(TAG, "Staging upload failed; saved locally for later sync fileName=${prepared.fileName} uri=$local")
                            }.onFailure { localError ->
                                Log.e(TAG, "Staging local fallback failed fileName=$fileName: ${localError.message}", localError)
                                withContext(Dispatchers.Main) {
                                    _state.value = _state.value.copy(
                                        stagedDocuments = _state.value.stagedDocuments.map {
                                            if (it.uri == uri && it.isUploading) {
                                                it.copy(isUploading = false, error = e.message)
                                            } else it
                                        }
                                    )
                                }
                            }
                        }
                    )
            } catch (e: Exception) {
                Log.e(TAG, "Staging preparation failed petId=$petId stagingId=$stagingId: ${e.message}", e)
                runCatching {
                    val local = copyStagedDocumentLocally(context, uri, fileName)
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            stagedDocuments = _state.value.stagedDocuments.map {
                                if (it.uri == uri && it.isUploading) {
                                    it.copy(
                                        uri = local,
                                        downloadUrl = local.toString(),
                                        isUploading = false,
                                        error = null
                                    )
                                } else it
                            }
                        )
                    }
                    Log.d(TAG, "Staging preparation failed; saved original locally for later sync fileName=$fileName uri=$local")
                }.onFailure { localError ->
                    Log.e(TAG, "Staging local fallback failed fileName=$fileName: ${localError.message}", localError)
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            stagedDocuments = _state.value.stagedDocuments.map {
                                if (it.uri == uri && it.isUploading) {
                                    it.copy(isUploading = false, error = e.message)
                                } else it
                            }
                        )
                    }
                }
            }
        }
    }

    fun addDocument(context: Context, uri: Uri) {
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = FirebaseDocumentUploader.getFileName(context, uri)
            ?: "document_${System.currentTimeMillis()}"
        addDocument(context, uri, mimeType, fileName)
    }

    fun removeDocument(doc: StagedDocument) {
        _state.value = _state.value.copy(
            stagedDocuments = _state.value.stagedDocuments.filter { it != doc }
        )
    }

    // ── Submit ────────────────────────────────────────────────────────────

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        val selectedVaccine = s.selectedVaccine
        val administeredByError = validateCommittedInput(s.administeredBy, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.PROVIDER_OR_CLINIC)
        val lotNumberError = validateCommittedInput(s.lotNumber, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.LOT_NUMBER)
        val birthDateError = if (s.petBirthDateIso != null && isBeforeBirth(s.dateGiven, s.petBirthDateIso)) {
            "Vaccination date cannot be before pet's birth date (${s.petBirthDateIso.take(10)})."
        } else null
        val firstError = listOfNotNull(
            if (s.petId.isBlank() || selectedVaccine == null || s.dateGiven.isBlank()) "Pet, vaccine and date are required." else null,
            birthDateError,
            administeredByError,
            lotNumberError
        ).firstOrNull()
        if (firstError != null) {
            Log.d(TAG, "submit blocked validationError=$firstError")
            _state.value = s.copy(error = firstError)
            return
        }
        if (s.stagedDocuments.any { it.isUploading }) {
            Log.d(TAG, "submit blocked waitingForUploads count=${s.stagedDocuments.count { it.isUploading }}")
            _state.value = s.copy(error = "Please wait for documents to finish uploading")
            return
        }
        val vaccine = selectedVaccine ?: return

        viewModelScope.launch {
            Log.d(
                TAG,
                "submit start petId=${s.petId} selectedVaccine=${vaccine.id} stagedDocs=${s.stagedDocuments.size}"
            )
            _state.value = s.copy(isLoading = true, error = null)

            val request = AddVaccinationRequest(
                vaccineId      = vaccine.id,
                dateGiven      = toIso(s.dateGiven),
                nextDueDate    = s.nextDueDate.takeIf { it.isNotBlank() }?.let { toIso(it) },
                lotNumber      = normalizeForCommit(s.lotNumber, InputFieldPolicy.GENERAL_TEXT),
                status         = "completed",
                administeredBy = normalizeForCommit(s.administeredBy, InputFieldPolicy.GENERAL_TEXT)
            )

            FeatureExecutionTracker.track("Add Vaccination") {
                RepositoryProvider.petRepository.addVaccination(s.petId, request)
            }.fold(
                onSuccess = { pet ->
                    // Buscar la vacunación recién creada (la última)
                    val newVaccinationId = pet.vaccinations.lastOrNull()?.id
                    Log.d(
                        TAG,
                        "submit add vaccination success petId=${s.petId} newVaccinationId=$newVaccinationId"
                    )

                    if (newVaccinationId != null) {
                        val successfulDocs = s.stagedDocuments
                            .filter { it.downloadUrl != null && it.error == null }
                        Log.d(TAG, "submit attaching stagedDocs=${successfulDocs.size} to vaccinationId=$newVaccinationId")

                        successfulDocs.forEach { doc ->
                            val localUri = doc.downloadUrl.orEmpty()
                            if (localUri.startsWith("file:")) {
                                RepositoryProvider.petRepository.queueVaccinationDocument(
                                    sourceUri = doc.uri,
                                    petId = s.petId,
                                    vaccinationId = newVaccinationId,
                                    fileName = doc.fileName,
                                    mimeType = doc.mimeType
                                ).fold(
                                    onSuccess = {
                                        Log.d(TAG, "submit queued local staged document fileName=${doc.fileName} vaccinationId=$newVaccinationId")
                                    },
                                    onFailure = {
                                        Log.e(TAG, "submit queue local staged document failed fileName=${doc.fileName}: ${it.message}", it)
                                    }
                                )
                            } else {
                                RepositoryProvider.petRepository.addVaccinationDocument(
                                    s.petId,
                                    newVaccinationId,
                                    AddDocumentRequest(
                                        fileName = doc.fileName,
                                        fileUri  = doc.downloadUrl
                                    )
                                ).fold(
                                    onSuccess = {
                                        Log.d(TAG, "submit document metadata saved fileName=${doc.fileName}")
                                    },
                                    onFailure = {
                                        Log.e(TAG, "submit document metadata failed fileName=${doc.fileName}: ${it.message}", it)
                                    }
                                )
                            }
                        }
                    }

                    _state.value = _state.value.copy(isLoading = false)
                    Log.d(TAG, "submit completed petId=${s.petId}")
                    onSuccess()
                },
                onFailure = { e ->
                    Log.e(TAG, "submit failed petId=${s.petId}: ${e.message}", e)
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

    private fun isBeforeBirth(date: String, birthIso: String): Boolean {
        val chosen = parseLocalDate(date) ?: return false
        val birth = parseLocalDate(birthIso) ?: return false
        return chosen.isBefore(birth)
    }

    private fun parseLocalDate(raw: String): LocalDate? = runCatching {
        when {
            raw.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> {
                val p = raw.split("/")
                LocalDate.of(p[2].toInt(), p[1].toInt(), p[0].toInt())
            }
            raw.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) -> LocalDate.parse(raw.take(10))
            else -> null
        }
    }.getOrNull()

    private fun copyStagedDocumentLocally(
        context: Context,
        sourceUri: Uri,
        fileName: String
    ): Uri {
        val dir = File(context.filesDir, "pending_vaccination_staging/${_state.value.stagingId}")
        dir.mkdirs()
        val safeFileName = fileName.replace(Regex("""[^\w.\-]"""), "_")
        val file = File(dir, "${UUID.randomUUID()}_$safeFileName")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read selected document")
        return Uri.fromFile(file)
    }

    private companion object {
        const val TAG = "DOC_UPLOAD"
    }
}
