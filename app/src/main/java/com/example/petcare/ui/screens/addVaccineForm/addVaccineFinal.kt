// AddVaccineFinalForm.kt — reemplaza completo

package com.example.petcare.ui.screens.addVaccineForm

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.ui.components.*
import com.example.petcare.data.analytics.FeatureClicksTracker
import com.example.petcare.ui.theme.PetCareTheme

@Composable
fun AddVaccineFinalForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddVaccineViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            val mimeType = context.contentResolver.getType(it)
                ?: "application/octet-stream"
            val fileName = com.example.petcare.util.FirebaseDocumentUploader
                .getFileName(context, it)
                ?: "document_${System.currentTimeMillis()}"

            viewModel.addDocument(context, it, mimeType, fileName)
        }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TransparentTopBar(title = "Add New Vaccine", onBackClick = onBack)
            Stepper(currentStep = 3, stepLabels = listOf("Basic Info", "Details", "Overview"))

            // ── Resumen read-only ────────────────────────────────────
            TextFieldComponent(
                name = "Vaccine",
                label = state.selectedVaccine?.name ?: "",
                value = state.selectedVaccine?.name ?: "",
                onValueChange = {}
            )
            TextFieldComponent(
                name = "Date Given",
                label = state.dateGiven,
                value = state.dateGiven,
                onValueChange = {}
            )
            TextFieldComponent(
                name = "Administered By",
                label = state.administeredBy,
                value = state.administeredBy,
                onValueChange = viewModel::setAdministeredBy
            )
            DateTextField(
                name = "Next Due Date (optional)",
                onDateSelected = viewModel::setNextDueDate
            )

            // ── Documentos adjuntos ──────────────────────────────────
            AttachedDocumentsCard(
                documents = state.stagedDocuments.map { staged ->
                    com.example.petcare.data.model.AttachedDocument(
                        id = staged.uri.toString(),
                        fileName = staged.fileName,
                        fileUri = staged.downloadUrl
                    )
                },
                isUploading = state.stagedDocuments.any { it.isUploading },
                onDocumentPicked = { _, _, _ ->
                    fileLauncher.launch(
                        arrayOf(
                            "image/*",
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument" +
                                    ".wordprocessingml.document",
                            "text/plain"
                        )
                    )
                }
            )

            // ── Error ────────────────────────────────────────────────
            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ── Botones ──────────────────────────────────────────────────
        Row {
            ButtonOutline(
                bgColor = MaterialTheme.colorScheme.background,
                outlineColor = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.secondary,
                width = 169.dp, height = 50.57.dp,
                text = "Back", onclick = onBack
            )
            Spacer(modifier = Modifier.width(10.dp))
            ButtonDefault(
                bgColor = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.onSecondary,
                width = 169.dp, height = 50.57.dp,
                text = if (state.isLoading) "Saving…" else "Add Vaccine",
                onclick = {
                    viewModel.submit {
                        FeatureClicksTracker.endRoute()
                        onclick()
                    }
                }
            )
        }
    }
}
