package com.example.petcare.ui.screens.petprofile.vaccines

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.ui.components.AttachedDocumentsCard
import com.example.petcare.ui.components.DateTextField
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineFilterStatus
import com.example.petcare.data.model.AttachedDocument
import com.example.petcare.data.analytics.FeatureClicksTracker
import com.example.petcare.ui.theme.*

@Composable
fun VaccineDetailsScreen(
    petId: String,
    vaccineId: String,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: VaccineDetailsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Load real data
    LaunchedEffect(petId, vaccineId) { viewModel.load(petId, vaccineId) }

    // Navigate back after successful delete
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    // Confirm-delete dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete vaccination?") },
            text    = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val vaccine = uiState.vaccine
                        if (vaccine != null) {
                            FeatureClicksTracker.endRoute()
                            viewModel.deleteVaccine()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
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
                    Text("Vaccine Details", color = MaterialTheme.colorScheme.surface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        bottomBar = {
            if (!uiState.isEditing) {
                StickyBottomActions(
                    isDeleting = uiState.isDeleting,
                    onDelete   = {
                        FeatureClicksTracker.startRoute("Delete Vaccination Flow")
                        showDeleteDialog = true
                    },
                    onEdit     = {
                        FeatureClicksTracker.startRoute("Edit Vaccination Flow")
                        viewModel.startEditing()
                    }
                )
            } else {
                // Save / Cancel bar
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick  = { viewModel.cancelEditing() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(28.dp)
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            val v = uiState.vaccine ?: return@Button
                            FeatureClicksTracker.endRoute()
                            viewModel.saveEdits()
                        },
                        enabled  = !uiState.isSaving,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(28.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.surface)
                    ) {
                        if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                        else Text("Save", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            }
            uiState.vaccine != null -> {
                val vaccine = uiState.vaccine!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error banner
                    uiState.error?.let { err ->
                        Card(colors = CardDefaults.cardColors(containerColor = ErrorContainer)) {
                            Text(err, color = ErrorContent, modifier = Modifier.padding(16.dp))
                        }
                    }

                    // Header
                    VaccineHeaderCard(name = vaccine.name, status = vaccine.status)

                    // Timeline — read-only or editable
                    if (uiState.isEditing) {
                        EditableTimelineCard(
                            dateGiven    = vaccine.dateGiven,
                            nextDueDate  = uiState.editNextDueDate,
                            onNextDueDate = viewModel::setNextDueDate
                        )
                    } else {
                        TimelineCard(dateGiven = vaccine.dateGiven, nextDueDate = vaccine.nextDueDate)
                    }

                    // Provider — editable
                    if (uiState.isEditing) {
                        EditableProviderCard(
                            administeredBy    = uiState.editAdministeredBy,
                            lotNumber         = uiState.editLotNumber,
                            onAdministeredBy  = viewModel::setAdministeredBy,
                            onLotNumber       = viewModel::setLotNumber
                        )
                    } else {
                        ProviderCard(veterinarian = vaccine.provider, clinic = "Happy Paws Clinic")
                    }

                    // Documents
                    AttachedDocumentsCard(
                        documents        = uiState.vaccine?.attachedDocuments ?: emptyList(),
                        isUploading      = uiState.isUploadingDoc,
                        onDocumentPicked = { uri, _, _ ->
                            viewModel.addDocument(context, uri)
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun EditableTimelineCard(
    dateGiven: String,
    nextDueDate: String,
    onNextDueDate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("TIMELINE", style = MaterialTheme.typography.labelMedium, color = GrayText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Date given: $dateGiven", style = MaterialTheme.typography.bodyMedium, color = GrayText)
            DateTextField(name = "Next due date (optional)", onDateSelected = onNextDueDate)
        }
    }
}

@Composable
private fun EditableProviderCard(
    administeredBy: String,
    lotNumber: String,
    onAdministeredBy: (String) -> Unit,
    onLotNumber: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("PROVIDER INFORMATION", style = MaterialTheme.typography.labelMedium, color = GrayText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            TextFieldComponent(name = "Administered by", label = "e.g. Dr. Smith", value = administeredBy, onValueChange = onAdministeredBy)
            TextFieldComponent(name = "Lot number (optional)", label = "e.g. LOT123", value = lotNumber, onValueChange = onLotNumber)
        }
    }
}

@Composable
private fun VaccineHeaderCard(name: String, status: VaccineFilterStatus) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(24.dp)) {
        Column {
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            val (bg, fg, label) = when (status) {
                VaccineFilterStatus.COMPLETED -> Triple(SuccessContainer, SuccessContent, "✓ Completed")
                VaccineFilterStatus.UPCOMING  -> Triple(InfoContainer,    InfoContent,    "Upcoming")
                VaccineFilterStatus.OVERDUE   -> Triple(ErrorContainer,   ErrorContent,   "! Overdue")
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TimelineCard(dateGiven: String, nextDueDate: String?) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(24.dp)) {
        Column {
            Text("TIMELINE", style = MaterialTheme.typography.labelMedium, color = GrayText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Date Given", fontSize = 12.sp, color = GrayText)
                    Text(dateGiven, fontSize = 16.sp)
                }
            }
            if (nextDueDate != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = GrayBorder)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Next Due Date", fontSize = 12.sp, color = GrayText)
                        Text(nextDueDate, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(veterinarian: String, clinic: String) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(24.dp)) {
        Column {
            Text("PROVIDER INFORMATION", style = MaterialTheme.typography.labelMedium, color = GrayText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Veterinarian", fontSize = 12.sp, color = GrayText)
                    Text(veterinarian, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = GrayBorder)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Clinic", fontSize = 12.sp, color = GrayText)
                    Text(clinic, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun StickyBottomActions(isDeleting: Boolean, onDelete: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(30.dp))
                .border(1.dp, if (isDeleting) GrayBorder else MaterialTheme.colorScheme.error, RoundedCornerShape(30.dp))
                .clickable(enabled = !isDeleting, onClick = onDelete)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isDeleting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ErrorContent, strokeWidth = 2.dp)
            else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.secondary)
                .clickable(onClick = onEdit)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp))
                Text("Edit", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
            }
        }
    }
}