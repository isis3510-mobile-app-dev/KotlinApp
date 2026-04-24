package com.example.petcare.ui.screens.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.data.model.WeightLog
import com.example.petcare.ui.components.DateTextField
import com.example.petcare.util.EventDateUtils
import java.util.Locale

@Composable
fun WeightTrackerScreen(
    petId: String,
    onBack: () -> Unit
) {
    val viewModel: WeightTrackerViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(petId) {
        viewModel.load(petId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
                Text(
                    "Weight Tracker",
                    color = MaterialTheme.colorScheme.surface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        WeightSummaryCard(
                            petName = state.petName,
                            latest = state.logs.firstOrNull(),
                            unit = state.preferredUnit,
                            onUnitSelected = viewModel::setPreferredUnit
                        )
                    }

                    item {
                        WeightLogForm(
                            state = state,
                            onWeightChange = viewModel::setWeightInput,
                            onDateSelected = viewModel::setDateInput,
                            onSave = viewModel::save,
                            onCancel = viewModel::cancelEditing
                        )
                    }

                    if (state.error != null) {
                        item {
                            Text(
                                state.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (state.logs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No weight logs yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(state.logs, key = { it.id }) { log ->
                            WeightLogRow(
                                log = log,
                                unit = state.preferredUnit,
                                onEdit = { viewModel.startEditing(log) },
                                onDelete = { viewModel.delete(log) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WeightSummaryCard(
    petName: String,
    latest: WeightLog?,
    unit: String,
    onUnitSelected: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Balance, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text(petName.ifBlank { "Pet" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = latest?.let { "${formatWeight(it.weight, unit)} logged ${displayDate(it.loggedAt)}" }
                    ?: "Start logging this pet's weight",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = unit == "kg", onClick = { onUnitSelected("kg") }, label = { Text("kg") })
                FilterChip(selected = unit == "lb", onClick = { onUnitSelected("lb") }, label = { Text("lb") })
            }
        }
    }
}

@Composable
private fun WeightLogForm(
    state: WeightTrackerUiState,
    onWeightChange: (String) -> Unit,
    onDateSelected: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (state.editingLogId == null) "New Weight Log" else "Edit Weight Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = state.weightInput,
                onValueChange = onWeightChange,
                label = { Text("Weight (${state.preferredUnit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            DateTextField(
                name = "Logged Date *",
                value = state.dateInput,
                onDateSelected = onDateSelected,
                allowFutureDates = false,
                minDateInclusive = state.petBirthDateIso
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.editingLogId != null) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text("Cancel") }
                }
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (state.editingLogId == null) "Save Log" else "Save Changes", color = MaterialTheme.colorScheme.surface)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightLogRow(
    log: WeightLog,
    unit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatWeight(log.weight, unit), fontWeight = FontWeight.Bold)
                Text(displayDate(log.loggedAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (log.id.startsWith("local_weight_")) {
                    Text("Pending sync", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatWeight(weightKg: Double, unit: String): String =
    if (unit == "lb") {
        "${String.format(Locale.US, "%.1f", weightKg * 2.20462)} lb"
    } else {
        "${String.format(Locale.US, "%.1f", weightKg)} kg"
    }

private fun displayDate(raw: String): String =
    EventDateUtils.splitToAppDateTime(raw).first.ifBlank { raw.take(10) }
