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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.ui.components.ExpandableFAB
import com.example.petcare.ui.components.OverdueWarningBanner
import com.example.petcare.ui.components.SuggestionBanner
import com.example.petcare.ui.screens.petprofile.components.events.eventTabContent
import com.example.petcare.ui.screens.petprofile.components.overview.PetInformationCard
import com.example.petcare.ui.screens.petprofile.components.overview.PetProfileHeader
import com.example.petcare.ui.screens.petprofile.components.overview.PetProfileTabs
import com.example.petcare.ui.screens.petprofile.components.overview.QuickActionGrid
import com.example.petcare.ui.screens.petprofile.components.overview.UpcomingEventsBanner
import com.example.petcare.ui.screens.petprofile.components.vaccines.vaccineTabContent
import com.example.petcare.data.analytics.FeatureClicksTracker
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.OffWhite
import com.example.petcare.util.DisplayTextLimits
import com.example.petcare.util.truncateForDisplay

@Composable
fun PetProfileScreen(
    petId: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit = {},
    onPetDeleted: () -> Unit = {},
    onAddEvent: () -> Unit = {},
    onAddVaccine: () -> Unit = {},
    onWeightTracker: () -> Unit = {},
    onNFCScan: () -> Unit = {},
    onNavigateToVaccineDetail: (petId: String, vaccineId: String) -> Unit = { _, _ -> },
    onNavigateToEventDetail: (petId: String, eventId: String) -> Unit = { _, _ -> },
    onSeeAllNotifications: (petId: String, petName: String) -> Unit = { _, _ -> },
) {
    val viewModel: PetProfileViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsStateWithLifecycle()

    LaunchedEffect(petId) { viewModel.loadPet(petId) }

    // ── Dialog / Sheet state ──────────────────────────────────────────────────
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet    by remember { mutableStateOf(false) }

    // ── Delete dialog ─────────────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${uiState.name.truncateForDisplay(DisplayTextLimits.DETAIL_TITLE)}?") },
            text  = {
                Text("All of this pet's data including vaccines and events will be permanently deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        FeatureClicksTracker.endRoute()
                        viewModel.deletePet(petId, onDeleted = onPetDeleted)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete permanently") }
            },
            dismissButton = {
                TextButton(onClick = {
                    FeatureClicksTracker.cancelRoute()
                    showDeleteDialog = false
                },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )) { Text("Cancel") }
            }
        )
    }

    // ── Edit bottom sheet ─────────────────────────────────────────────────────
    if (showEditSheet && !uiState.isLoading) {
        EditPetBottomSheet(
            petId              = petId,
            initialName        = uiState.name,
            initialBreed       = uiState.breed,
            // Strip " kg" suffix added by the ViewModel for display
            initialWeight      = uiState.weight.removeSuffix(" kg"),
            initialColor          = uiState.color,
            initialBirthDate      = uiState.dateOfBirth,
            initialKnownAllergies = uiState.knownAllergies,
            initialDefaultVet  = uiState.defaultVet,
            initialDefaultClinic = uiState.defaultClinic,
            initialPhotoUrl    = uiState.photoUrl,
            onDismiss          = { showEditSheet = false },
            onSaved            = {
                FeatureClicksTracker.endRoute()
                showEditSheet = false
                viewModel.reloadPet()   // silently refresh after save
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {

        // ── Top App Bar ───────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenDark)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    FeatureClicksTracker.cancelRoute()
                    onBack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Row {
                    //IconButton(onClick = { /* Share */ }) {
                    //    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    //}
                    // Edit button now opens the bottom sheet
                    IconButton(onClick = {
                        FeatureClicksTracker.startRoute("Edit Pet Flow")
                        showEditSheet = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                    IconButton(onClick = {
                        FeatureClicksTracker.startRoute("Delete Pet Flow")
                        showDeleteDialog = true
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                    }
                }
            }
        }

        // ── Pet Header ────────────────────────────────────────────────────────
        item {
            PetProfileHeader(
                name         = uiState.name,
                breed        = uiState.breed,
                species      = uiState.species,
                age          = uiState.age,
                weight       = uiState.weight,
                gender       = uiState.gender,
                isHealthy    = uiState.isHealthy,
                isLost       = uiState.isLost,
                isNfcSynched = uiState.isNfcSynched,
                photoPath    = uiState.photoUrl
            )
        }

        // ── Tabs ──────────────────────────────────────────────────────────────
        item {
            PetProfileTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected    = viewModel::onTabSelected
            )
        }

        // ── Health alerts ─────────────────────────────────────────────────────
        if (uiState.suggestions.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    SuggestionBanner(suggestions = uiState.suggestions.take(2))
                    if (uiState.suggestions.size > 2) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                FeatureClicksTracker.recordClick()
                                onSeeAllNotifications(petId, uiState.name)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text     = "See all ${uiState.suggestions.size} alerts",
                                fontSize = 13.sp,
                                color    =  MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        } else if (uiState.overdueVaccinesCount > 0) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    OverdueWarningBanner(overdueCount = uiState.overdueVaccinesCount)
                }
            }
        } else {
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // ── Tab content ───────────────────────────────────────────────────────
        when (selectedTabIndex) {
            0 -> overviewTabContent(
                uiState      = uiState,
                viewModel    = viewModel,
                onAddEvent   = onAddEvent,
                onAddVaccine = onAddVaccine,
                onWeightTracker = onWeightTracker,
                onNFCScan    = onNFCScan
            )

            1 -> {
                val displayedVaccines = if (uiState.vaccineFilter != null) {
                    uiState.vaccines.filter { it.status == uiState.vaccineFilter }
                } else {
                    uiState.vaccines
                }
                vaccineTabContent(
                    vaccines          = displayedVaccines,
                    onFilterClick     = viewModel::onVaccineFilterClick,
                    onVaccineClick    = { vaccine ->
                        FeatureClicksTracker.recordClick()
                        onNavigateToVaccineDetail(petId, vaccine.id)
                    },
                    onAddVaccineClick = onAddVaccine
                )
            }

            2 -> eventTabContent(
                events          = uiState.events,
                onEventClick    = { eventId ->
                    FeatureClicksTracker.recordClick()
                    onNavigateToEventDetail(petId, eventId)
                },
                onAddEventClick = onAddEvent
            )
        }
    }
}

// ── Overview tab ──────────────────────────────────────────────────────────────

private fun LazyListScope.overviewTabContent(
    uiState: PetProfileUiState,
    viewModel: PetProfileViewModel,
    onAddEvent: () -> Unit,
    onNFCScan: () -> Unit,
    onAddVaccine: () -> Unit,
    onWeightTracker: () -> Unit,
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
                isNfcSynced = uiState.isNfcSynched,
            )

            if (uiState.upcomingEventsCount > 0) {
                UpcomingEventsBanner(
                    upcomingCount = uiState.upcomingEventsCount,
                    onClick       = { viewModel.onTabSelected(2) }
                )
            }

            QuickActionGrid(
                onAddEventClick   = {
                    FeatureClicksTracker.startRoute("Add Event Flow")
                    onAddEvent()
                },
                onAddVaccineClick = {
                    FeatureClicksTracker.startRoute("Add Vaccine Flow")
                    onAddVaccine()
                },
                onLostModeClick   = {
                    FeatureClicksTracker.recordClick()
                    viewModel.onLostModeClicked()
                },
                onWeightTrackerClick = {
                    FeatureClicksTracker.startRoute("Weight Log Flow")
                    onWeightTracker()
                },
                onNfcClick        = onNFCScan,
                isLost            = uiState.isLost
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
