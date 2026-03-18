package com.example.petcare.ui.screens.records

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.petcare.ui.components.EmptyStateView
import com.example.petcare.ui.components.Filters
import com.example.petcare.ui.components.MedicalEventItem
import com.example.petcare.ui.components.OverdueWarningBanner
import com.example.petcare.ui.components.VaccineListItem
import com.example.petcare.ui.theme.ErrorContainer
import com.example.petcare.ui.theme.ErrorContent
import com.example.petcare.ui.theme.GrayBackground
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.InfoContainer
import com.example.petcare.ui.theme.InfoContent
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.SuccessContent

@Composable
fun HealthRecordsScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onNavigateToVaccineDetail: (petId: String, vaccinationId: String) -> Unit = { _, _ -> },
    onNavigateToEventDetail: (petId: String, eventId: String) -> Unit = { _, _ -> }
) {
    val viewModel: HealthRecordsViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadData() }

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
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenDark)
                }
            }

            state.error != null -> {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.error ?: "Error", color = Color.Red)
                }
            }

            else -> {
                when (state.selectedFilter) {
                    "All"      -> AllRecordsContent(state, onNavigateToVaccineDetail, onNavigateToEventDetail)
                    "Vaccines" -> VaccinesContent(state, onNavigateToVaccineDetail)
                    "Events"   -> EventsContent(state, onNavigateToEventDetail)
                }
            }
        }
    }
}

@Composable
private fun AllRecordsContent(
    state: HealthRecordsState,
    onNavigateToVaccineDetail: (String, String) -> Unit,
    onNavigateToEventDetail: (String, String) -> Unit
) {
    val overdueCount  = state.vaccines.count { it.data.status == "overdue" }
    val upcomingCount = state.vaccines.count { it.data.status == "upcoming" }

    if (state.vaccines.isEmpty() && state.events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateView(icon = Icons.Default.Vaccines, message = "No health records yet")
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
                    if (overdueCount > 0)  StatusChip("$overdueCount overdue",  ErrorContainer, ErrorContent)
                    if (overdueCount > 0 && upcomingCount > 0) Spacer(modifier = Modifier.padding(4.dp))
                    if (upcomingCount > 0) StatusChip("$upcomingCount upcoming", InfoContainer,  InfoContent)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(state.vaccines) { item ->
                VaccineListItem(
                    vaccine = item.data,
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

@Composable
private fun VaccinesContent(
    state: HealthRecordsState,
    onNavigateToVaccineDetail: (String, String) -> Unit
) {
    if (state.vaccines.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateView(icon = Icons.Default.Vaccines, message = "No vaccines recorded yet")
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

@Composable
private fun EventsContent(
    state: HealthRecordsState,
    onNavigateToEventDetail: (String, String) -> Unit
) {
    if (state.events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateView(icon = Icons.Default.CalendarMonth, message = "No medical events yet")
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

@Preview(showBackground = true)
@Composable
fun HealthRecordsScreenPreview() {
    PetCareTheme { HealthRecordsScreen() }
}