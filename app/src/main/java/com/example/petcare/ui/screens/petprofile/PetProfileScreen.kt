package com.example.petcare.ui.screens.petprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.ui.components.ExpandableFAB
import com.example.petcare.ui.components.OverdueWarningBanner
import com.example.petcare.ui.screens.petprofile.components.events.eventTabContent
import com.example.petcare.ui.screens.petprofile.components.overview.PetInformationCard
import com.example.petcare.ui.screens.petprofile.components.overview.PetProfileHeader
import com.example.petcare.ui.screens.petprofile.components.overview.PetProfileTabs
import com.example.petcare.ui.screens.petprofile.components.overview.QuickActionGrid
import com.example.petcare.ui.screens.petprofile.components.overview.UpcomingEventsBanner
import com.example.petcare.ui.screens.petprofile.components.vaccines.vaccineTabContent
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.OffWhite

@Composable
fun PetProfileScreen(
    petId: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit = {},
    onAddEvent: () -> Unit = {},
    onAddVaccine: () -> Unit = {},
    onNFCScan: () -> Unit = {},
    onNavigateToVaccineDetail: (petId: String, vaccineId: String) -> Unit = { _, _ -> },
    onNavigateToEventDetail: (petId: String, eventId: String) -> Unit = { _, _ -> }
) {
    val viewModel: PetProfileViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsStateWithLifecycle()

    // ── Load pet data once ────────────────────────────────────────────────
    LaunchedEffect(petId) { viewModel.loadPet(petId) }

    // ── Delete dialog state ───────────────────────────────────────────────
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${uiState.name}?") },
            text  = {
                Text("All of this pet's data including vaccines and events will be permanently deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePet(petId, onNavigatedBack = onBack)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {

        // ── Top App Bar ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenDark)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Row {
                    IconButton(onClick = { /* Share — future feature */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(onClick = { /* Edit pet info — future feature */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                    // MoreVert opens delete dialog
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                    }
                }
            }
        }

        // ── Pet Header ────────────────────────────────────────────────────
        item {
            PetProfileHeader(
                name         = uiState.name,
                breed        = uiState.breed,
                species      = uiState.species,
                age          = uiState.age,
                weight       = uiState.weight,
                gender       = uiState.gender,
                isHealthy    = uiState.isHealthy,
                isNfcSynched = uiState.isNfcSynched
            )
        }

        // ── Tabs ──────────────────────────────────────────────────────────
        item {
            PetProfileTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected    = viewModel::onTabSelected
            )
        }

        // ── Overdue banner / spacing ──────────────────────────────────────
        if (uiState.overdueVaccinesCount > 0) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    OverdueWarningBanner(overdueCount = uiState.overdueVaccinesCount)
                }
            }
        } else {
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // ── Tab content ───────────────────────────────────────────────────
        when (selectedTabIndex) {
            0 -> overviewTabContent(
                uiState      = uiState,
                viewModel    = viewModel,
                onAddEvent   = onAddEvent,
                onAddVaccine = onAddVaccine,
                onNFCScan    = onNFCScan
            )

            1 -> {
                val displayedVaccines = if (uiState.vaccineFilter != null) {
                    uiState.vaccines.filter { it.status == uiState.vaccineFilter }
                } else {
                    uiState.vaccines
                }
                vaccineTabContent(
                    vaccines       = displayedVaccines,
                    onFilterClick  = viewModel::onVaccineFilterClick,
                    onVaccineClick = { vaccine ->
                        onNavigateToVaccineDetail(petId, vaccine.id)
                    }
                )
            }

            2 -> eventTabContent(
                events         = uiState.events,
                onEventClick   = { eventId -> onNavigateToEventDetail(petId, eventId) },
                onAddEventClick = onAddEvent
            )
        }
    }
}

// ── Overview tab content ──────────────────────────────────────────────────

private fun LazyListScope.overviewTabContent(
    uiState:      PetProfileUiState,
    viewModel:    PetProfileViewModel,
    onAddEvent:   () -> Unit,
    onNFCScan:    () -> Unit,
    onAddVaccine: () -> Unit
) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PetInformationCard(
                species     = uiState.species,
                breed       = uiState.breed,
                dateOfBirth = uiState.dateOfBirth,
                age         = uiState.age,
                weight      = uiState.weight,
                color       = uiState.color,
                gender      = uiState.gender,
                microchip   = uiState.microchip
            )

            if (uiState.upcomingEventsCount > 0) {
                UpcomingEventsBanner(
                    upcomingCount = uiState.upcomingEventsCount,
                    onClick       = { viewModel.onTabSelected(2) }
                )
            }

            QuickActionGrid(
                onAddEventClick   = onAddEvent,
                onAddVaccineClick = onAddVaccine,
                onLostModeClick   = viewModel::onLostModeClicked,
                onNfcClick        = onNFCScan
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true, name = "PetProfile - Overview")
@Composable
fun PetProfileOverviewPreview() {
    PetCareTheme {
        Scaffold(
            floatingActionButton = {
                ExpandableFAB(onAddPet = {}, onAddEvent = {}, onAddVaccine = {}, onScanNFC = {})
            },
            containerColor = OffWhite
        ) { innerPadding ->
            PetProfileScreen(petId = "preview_id", contentPadding = innerPadding, onBack = {})
        }
    }
}
