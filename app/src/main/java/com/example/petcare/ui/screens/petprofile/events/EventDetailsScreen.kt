package com.example.petcare.ui.screens.petprofile.events

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.ui.components.ActionFooter
import com.example.petcare.ui.components.AttachedDocumentsCard
import com.example.petcare.ui.components.ProviderInfoCard
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.theme.ErrorContainer
import com.example.petcare.ui.theme.ErrorContent
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.OffWhite
import com.example.petcare.data.analytics.FeatureClicksTracker
import com.example.petcare.ui.theme.PetCareTheme

@Composable
fun EventDetailsScreen(
    petId: String,
    eventId: String,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: EventDetailsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Load real event data
    LaunchedEffect(eventId) { viewModel.load(eventId) }

    // Navigate back after successful delete
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }


    // Delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete event?") },
            text  = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
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
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (!uiState.isEditing) {
                ActionFooter(
                    onDeleteClicked = {
                        FeatureClicksTracker.startRoute("Delete Event Flow")
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
                        onClick  = { viewModel.cancelEditing() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(28.dp)
                    ) { Text("Cancel") }

                    Button(
                        onClick  = {
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
                            Text("Save", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
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
                    // ── Top App Bar ───────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(horizontal = 8.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text       = "Medical Event",
                                    style      = MaterialTheme.typography.titleLarge,
                                    color      = MaterialTheme.colorScheme.onSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text  = event.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
                            )
                        }
                    }

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
                                onDocumentPicked = { uri, _, _ ->
                                    viewModel.addDocument(context, petId, uri)
                                }
                            )

                            // ── Edit mode ─────────────────────────────────────
                        } else {
                            // Title
                            TextFieldComponent(
                                name          = "Event Name",
                                label         = "e.g. Vet visit",
                                value         = uiState.editTitle,
                                onValueChange = viewModel::setTitle
                            )

                            // Description
                            TextFieldComponent(
                                name          = "Description",
                                label         = "e.g. Annual checkup",
                                value         = uiState.editDescription,
                                onValueChange = viewModel::setDescription
                            )

                            // Provider
                            TextFieldComponent(
                                name          = "Provider / Doctor",
                                label         = "e.g. Dr. Smith",
                                value         = uiState.editProvider,
                                onValueChange = viewModel::setProvider
                            )

                            // Clinic
                            TextFieldComponent(
                                name          = "Clinic",
                                label         = "e.g. Happy Paws Clinic",
                                value         = uiState.editClinic,
                                onValueChange = viewModel::setClinic
                            )

                            // Price
                            TextFieldComponent(
                                name          = "Price (optional)",
                                label         = "e.g. 50",
                                value         = uiState.editPrice,
                                onValueChange = viewModel::setPrice
                            )

                            // Documents still available in edit mode
                            AttachedDocumentsCard(
                                documents        = event.attachedDocuments,
                                isUploading      = uiState.isUploadingDoc,
                                onDocumentPicked = { uri, _, _ ->
                                    viewModel.addDocument(context, petId, uri)
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

// ── Previews ─────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun EventDetailsScreenPreview() {
    PetCareTheme {
        EventDetailsScreen(
            petId          = "preview_pet",
            eventId        = "preview_event",
            onNavigateBack = {}
        )
    }
}