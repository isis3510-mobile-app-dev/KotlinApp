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
import com.example.petcare.ui.theme.PetCareTheme

@Composable
fun EventDetailsScreen(
    petId: String,
    eventId: String,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: EventDetailsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load real event data
    LaunchedEffect(eventId) { viewModel.load(eventId) }

    // Navigate back after successful delete
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    // File picker for document upload
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = uri.lastPathSegment ?: "document"
            viewModel.addDocument(fileName, uri.toString())
        }
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
                    onDeleteClicked = { showDeleteDialog = true },
                    onEditClicked   = { viewModel.startEditing() },
                    isDeleteEnabled = !uiState.isDeleting
                )
            } else {
                // Save / Cancel bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick  = { viewModel.cancelEditing() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(28.dp)
                    ) { Text("Cancel") }

                    Button(
                        onClick  = { viewModel.saveEdits() },
                        enabled  = !uiState.isSaving,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(28.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = GreenDark)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = OffWhite
    ) { paddingValues ->

        when {
            uiState.isLoading -> {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenDark)
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
                            .background(GreenDark)
                            .padding(horizontal = 8.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text       = "Medical Event",
                                    style      = MaterialTheme.typography.titleLarge,
                                    color      = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text  = event.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
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
                                documents    = event.attachedDocuments,
                                onAddClicked = { filePicker.launch("*/*") }
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
                                documents    = event.attachedDocuments,
                                onAddClicked = { filePicker.launch("*/*") }
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