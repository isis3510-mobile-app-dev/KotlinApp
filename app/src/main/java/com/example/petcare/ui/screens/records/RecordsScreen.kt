package com.example.petcare.ui.screens.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.ui.components.*
import com.example.petcare.ui.theme.*

@Composable
fun HealthRecordsScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    reloadTrigger: Boolean = false,
    onReloadConsumed: () -> Unit = {},
    onNavigateToVaccineDetail: (petId: String, vaccinationId: String) -> Unit = { _, _ -> },
    onNavigateToEventDetail:   (petId: String, eventId: String)   -> Unit = { _, _ -> },
    onAddRecordClick:  () -> Unit = {},
    onAddVaccineClick: () -> Unit = {},
    onAddEventClick:   () -> Unit = {}
) {
    val viewModel: HealthRecordsViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Carga inicial
    LaunchedEffect(Unit) { viewModel.loadData() }

    // Recarga reactiva cuando el padre señala "reload_records"
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger) {
            viewModel.loadData()
            onReloadConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = "Health Records",
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Filters(
            filters          = listOf("All", "Vaccines", "Events"),
            selectedFilter   = state.selectedFilter,
            onFilterSelected = viewModel::onFilterSelected
        )

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenDark)
                }
            }
            state.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.error ?: "Error", color = Color.Red)
                }
            }
            else -> {
                when (state.selectedFilter) {
                    "All"      -> AllRecordsContent(
                        state                     = state,
                        onNavigateToVaccineDetail = onNavigateToVaccineDetail,
                        onNavigateToEventDetail   = onNavigateToEventDetail,
                        onAddRecordClick          = onAddRecordClick
                    )
                    "Vaccines" -> VaccinesContent(
                        state                     = state,
                        onNavigateToVaccineDetail = onNavigateToVaccineDetail,
                        onAddVaccineClick         = onAddVaccineClick
                    )
                    "Events"   -> EventsContent(
                        state                   = state,
                        onNavigateToEventDetail = onNavigateToEventDetail,
                        onAddEventClick         = onAddEventClick
                    )
                }
            }
        }
    }
}

// ── All ───────────────────────────────────────────────────────────────────────

@Composable
private fun AllRecordsContent(
    state: HealthRecordsState,
    onNavigateToVaccineDetail: (String, String) -> Unit,
    onNavigateToEventDetail:   (String, String) -> Unit,
    onAddRecordClick: () -> Unit
) {
    val overdueCount  = state.vaccines.count { it.data.status == "overdue" }
    val upcomingCount = state.vaccines.count { it.data.status == "upcoming" }

    if (state.vaccines.isEmpty() && state.events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateView(
                icon          = Icons.Default.Vaccines,
                message       = "No health records yet",
                buttonText    = "Add New Record",
                onButtonClick = onAddRecordClick
            )
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding      = PaddingValues(bottom = 80.dp)
    ) {
        if (overdueCount > 0) {
            item {
                OverdueWarningBanner(overdueCount = overdueCount)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (state.vaccines.isNotEmpty()) {
            item {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vaccines", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    if (overdueCount > 0)
                        StatusChip("$overdueCount overdue", ErrorContainer, ErrorContent)
                    if (overdueCount > 0 && upcomingCount > 0)
                        Spacer(modifier = Modifier.padding(4.dp))
                    if (upcomingCount > 0)
                        StatusChip("$upcomingCount upcoming", InfoContainer, InfoContent)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(state.vaccines) { item ->
                VaccineListItem(
                    vaccine = item.data,
                    // ← item.vaccinationId es el _id embebido correcto
                    onClick = { onNavigateToVaccineDetail(item.petId, item.vaccinationId) }
                )
                HorizontalDivider(color = GrayBackground)
            }
        }

        if (state.events.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Medical Events", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(state.events) { item ->
                MedicalEventItem(
                    event   = item.data,
                    onClick = { onNavigateToEventDetail(item.petId, item.eventId) }
                )
            }
        }
    }
}

// ── Vaccines ──────────────────────────────────────────────────────────────────

@Composable
private fun VaccinesContent(
    state: HealthRecordsState,
    onNavigateToVaccineDetail: (String, String) -> Unit,
    onAddVaccineClick: () -> Unit
) {
    if (state.vaccines.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateView(
                icon          = Icons.Default.Vaccines,
                message       = "No vaccines recorded yet",
                buttonText    = "Add Vaccine",
                onButtonClick = onAddVaccineClick
            )
        }
        return
    }

    val activeCount = state.vaccines.count { it.data.status != "overdue" }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Vaccines", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            StatusChip("$activeCount active", SuccessContainer, SuccessContent)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding      = PaddingValues(bottom = 80.dp)
        ) {
            items(state.vaccines) { item ->
                VaccineListItem(
                    vaccine = item.data,
                    onClick = { onNavigateToVaccineDetail(item.petId, item.vaccinationId) }
                )
            }
        }
    }
}

// ── Events ────────────────────────────────────────────────────────────────────

@Composable
private fun EventsContent(
    state: HealthRecordsState,
    onNavigateToEventDetail: (String, String) -> Unit,
    onAddEventClick: () -> Unit
) {
    if (state.events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateView(
                icon          = Icons.Default.CalendarMonth,
                message       = "No medical events yet",
                buttonText    = "Add Medical Event",
                onButtonClick = onAddEventClick
            )
        }
        return
    }

    Column {
        Text("Medical Events", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding      = PaddingValues(bottom = 80.dp)
        ) {
            items(state.events) { item ->
                MedicalEventItem(
                    event   = item.data,
                    onClick = { onNavigateToEventDetail(item.petId, item.eventId) }
                )
            }
        }
    }
}

// ── Chip ──────────────────────────────────────────────────────────────────────

@Composable
private fun StatusChip(label: String, background: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 11.sp, color = textColor)
    }
}

