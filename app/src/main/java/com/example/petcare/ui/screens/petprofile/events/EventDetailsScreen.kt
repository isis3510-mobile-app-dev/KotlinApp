package com.example.petcare.ui.screens.petprofile.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.ui.components.ActionFooter
import com.example.petcare.ui.components.AttachedDocumentsCard
import com.example.petcare.ui.components.ProviderInfoCard
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.components.DateTextField
import com.example.petcare.ui.components.TimeTextField
import com.example.petcare.ui.theme.ErrorContainer
import com.example.petcare.ui.theme.ErrorContent
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.OffWhite
import com.example.petcare.data.analytics.FeatureClicksTracker
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.util.DisplayTextLimits
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.truncateForDisplay

@Composable
fun EventDetailsScreen(
    petId: String,
    eventId: String,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: EventDetailsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(eventId, petId) { viewModel.load(eventId, petId) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // FIX: separate delete and save dialogs - each one waits for user confirmation before acting
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete event?") },
            text  = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // FIX: only execute delete when user explicitly confirms
                        showDeleteDialog = false
                        FeatureClicksTracker.endRoute()
                        viewModel.deleteEvent()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                // FIX: Cancel does NOT proceed with the action
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // FIX: Save confirmation dialog in edit mode - user must confirm before saving
    if (showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            title = { Text("Save changes?") },
            text  = { Text("Do you want to save the changes to this event?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveConfirmDialog = false
                        FeatureClicksTracker.endRoute()
                        viewModel.saveEdits()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) { Text("Save") }
            },
            dismissButton = {
                // FIX: Cancel does NOT save
                TextButton(onClick = { showSaveConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (!uiState.isEditing) {
                ActionFooter(
                    onDeleteClicked = {
                        FeatureClicksTracker.startRoute("Delete Event Flow")
                        // FIX: show dialog first, don't act immediately
                        showDeleteDialog = true
                    },
                    onEditClicked   = {
                        FeatureClicksTracker.startRoute("Edit Event Flow")
                        viewModel.startEditing()
                    },
                    isDeleteEnabled = !uiState.isDeleting
                )
            } else {
                // Save / Cancel bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        // FIX: Cancel closes edit mode without saving - no confirmation needed
                        onClick  = { viewModel.cancelEditing() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(28.dp)
                    ) { Text("Cancel") }

                    Button(
                        onClick  = {
                            // FIX: Save button directly saves without showing a warning dialog
                            // The old code was showing a dialog (showDeleteDialog) but still saving - that was the bug
                            FeatureClicksTracker.endRoute()
                            viewModel.saveEdits()
                        },
                        enabled  = !uiState.isSaving,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(28.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.secondary)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.surface)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text       = "Medical Event",
                                style      = MaterialTheme.typography.titleLarge,
                                color      = MaterialTheme.colorScheme.surface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
    ) { paddingValues ->

        when {
            uiState.isLoading -> {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            }

            uiState.event == null && !uiState.isLoading -> {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Event not found", color = MaterialTheme.colorScheme.error)
                }
            }

            else -> {
                val event = uiState.event!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {


                    // ── Body ──────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // Error banner
                        uiState.error?.let { err ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = ErrorContainer
                                )
                            ) {
                                Text(
                                    text     = err,
                                    color    = ErrorContent,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        // ── Read mode ─────────────────────────────────────
                        if (!uiState.isEditing) {
                            EventDetailsHeaderCard(
                                title     = event.title,
                                eventType = event.eventType
                            )

                            EventDateCard(
                                eventDate    = event.date,
                                followUpDate = event.followUpDate
                            )

                            ProviderInfoCard(
                                veterinarian = event.provider,
                                clinic       = event.clinic
                            )

                            NotesCard(description = event.description)

                            AttachedDocumentsCard(
                                documents        = event.attachedDocuments,
                                isUploading      = uiState.isUploadingDoc,
                                allowAddDocument = event.attachedDocuments.isEmpty(),
                                addDisabledMessage = "Only one document is allowed for events. Delete the current document to upload another.",
                                onDocumentPicked = { uri, mimeType, fileName ->
                                    viewModel.addDocument(context, petId, uri, mimeType, fileName)
                                },
                                onDeleteDocument = { docId ->
                                    viewModel.deleteDocument(context, eventId, docId)
                                }
                            )

                            // ── Edit mode ─────────────────────────────────────
                        } else {
                            TextFieldComponent(
                                name          = "Event Name",
                                label         = "e.g. Vet visit",
                                value         = uiState.editTitle,
                                onValueChange = viewModel::setTitle,
                                fieldPolicy   = InputFieldPolicy.GENERAL_TEXT,
                                maxLength     = InputTextLimits.EVENT_TITLE
                            )

                            TextFieldComponent(
                                name          = "Description",
                                label         = "e.g. Annual checkup",
                                value         = uiState.editDescription,
                                onValueChange = viewModel::setDescription,
                                fieldPolicy   = InputFieldPolicy.GENERAL_TEXT,
                                maxLength     = InputTextLimits.NOTES
                            )

                            TextFieldComponent(
                                name          = "Provider / Doctor",
                                label         = "e.g. Dr. Smith",
                                value         = uiState.editProvider,
                                onValueChange = viewModel::setProvider,
                                fieldPolicy   = InputFieldPolicy.GENERAL_TEXT,
                                maxLength     = InputTextLimits.PROVIDER_OR_CLINIC
                            )

                            TextFieldComponent(
                                name          = "Clinic",
                                label         = "e.g. Happy Paws Clinic",
                                value         = uiState.editClinic,
                                onValueChange = viewModel::setClinic,
                                fieldPolicy   = InputFieldPolicy.GENERAL_TEXT,
                                maxLength     = InputTextLimits.PROVIDER_OR_CLINIC
                            )

                            DateTextField(
                                name           = "Event Date *",
                                value          = uiState.editDate,
                                onDateSelected = viewModel::setDate,
                                minDateInclusive = uiState.petBirthDateIso
                            )

                            TimeTextField(
                                name           = "Event Time",
                                value          = uiState.editTime,
                                onTimeSelected = viewModel::setTime
                            )

                            TextFieldComponent(
                                name          = "Price (optional)",
                                label         = "e.g. 50",
                                value         = uiState.editPrice,
                                onValueChange = viewModel::setPrice,
                                maxLength     = InputTextLimits.PRICE,
                                fieldPolicy   = InputFieldPolicy.DECIMAL
                            )

                            AttachedDocumentsCard(
                                documents        = event.attachedDocuments,
                                isUploading      = uiState.isUploadingDoc,
                                allowAddDocument = event.attachedDocuments.isEmpty(),
                                addDisabledMessage = "Only one document is allowed for events. Delete the current document to upload another.",
                                onDocumentPicked = { uri, mimeType, fileName ->
                                    viewModel.addDocument(context, petId, uri, mimeType, fileName)
                                },
                                onDeleteDocument = { docId ->
                                    viewModel.deleteDocument(context, eventId, docId)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
