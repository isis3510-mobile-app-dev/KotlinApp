package com.example.petcare.ui.screens.petprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.ui.components.ExpandableFAB
import com.example.petcare.ui.components.NavBar
import com.example.petcare.ui.components.OverdueWarningBanner
import com.example.petcare.ui.screens.petprofile.components.events.eventTabContent
import com.example.petcare.ui.screens.petprofile.components.overview.PetInformationCard
import com.example.petcare.ui.screens.petprofile.components.overview.PetProfileHeader
import com.example.petcare.ui.screens.petprofile.components.overview.PetProfileTabs
import com.example.petcare.ui.screens.petprofile.components.overview.QuickActionGrid
import com.example.petcare.ui.screens.petprofile.components.overview.UpcomingEventsBanner
import com.example.petcare.ui.screens.petprofile.components.vaccines.vaccineTabContent
import com.example.petcare.ui.theme.GreenAccentDark
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.OffWhite

@Composable
fun PetProfileScreen(
    viewModel: PetProfileViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateTab: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavBar(
                currentRoute = "pets", // Active tab
                onItemClick = onNavigateTab
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) { // Vaccines tab
                androidx.compose.material3.FloatingActionButton(
                    onClick = viewModel::onAddVaccineClicked,
                    containerColor = GreenAccentDark,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Vaccine")
                }
            } else if (selectedTabIndex == 0) { // Overview tab
                ExpandableFAB()
            }
            // Add action for events tab already implemented inside eventTabContent
        },
        containerColor = OffWhite // OffWhite / very light blue-green background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top App Bar area (blends into the header)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenDark)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Row {
                        IconButton(onClick = { /* Share */ }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = { /* Edit */ }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                        IconButton(onClick = { /* More */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                    }
                }
            }

            // Header Component
            item {
                PetProfileHeader(
                    name = uiState.name,
                    breed = uiState.breed,
                    species = uiState.species,
                    age = uiState.age,
                    weight = uiState.weight,
                    gender = uiState.gender,
                    isHealthy = uiState.isHealthy,
                    isNfcSynched = uiState.isNfcSynched
                )
            }

            // Tabs
            item {
                PetProfileTabs(
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = viewModel::onTabSelected
                )
            }

            // Warning Banner
            if (uiState.overdueVaccinesCount > 0) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        OverdueWarningBanner(overdueCount = uiState.overdueVaccinesCount)
                    }
                }
            } else {
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    overviewTabContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
                1 -> {
                    val displayedVaccines = if (uiState.vaccineFilter != null) {
                        uiState.vaccines.filter { it.status == uiState.vaccineFilter }
                    } else {
                        uiState.vaccines
                    }
                    
                    vaccineTabContent(
                        vaccines = displayedVaccines,
                        onFilterClick = viewModel::onVaccineFilterClick,
                        onVaccineClick = viewModel::onVaccineClicked
                    )
                }
                2 -> {
                    eventTabContent(
                        events = uiState.events,
                        onEventClick = { /* Navigate to event details */ },
                        onAddEventClick = viewModel::onAddEventClicked
                    )
                }
            }
        }
    }
}

private fun LazyListScope.overviewTabContent(
    uiState: PetProfileUiState,
    viewModel: PetProfileViewModel
) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pet Information
            PetInformationCard(
                species = uiState.species,
                breed = uiState.breed,
                dateOfBirth = uiState.dateOfBirth,
                age = uiState.age,
                weight = uiState.weight,
                color = uiState.color,
                gender = uiState.gender,
                microchip = uiState.microchip
            )

            // Upcoming Events Button Widget
            if (uiState.upcomingEventsCount > 0) {
                UpcomingEventsBanner(
                    upcomingCount = uiState.upcomingEventsCount,
                    onClick = { viewModel.onTabSelected(2) } // Navigate to events tab
                )
            }

            // 2x2 Grid Actions
            QuickActionGrid(
                onAddEventClick = viewModel::onAddEventClicked,
                onAddVaccineClick = viewModel::onAddVaccineClicked,
                onLostModeClick = viewModel::onLostModeClicked,
                onNfcClick = viewModel::onNfcActiveClicked
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PetProfileScreenOverviewPreview() {
    PetCareTheme {
        PetProfileScreen(viewModel = PetProfileViewModel())
    }
}

@Preview(showBackground = true)
@Composable
fun PetProfileScreenVaccinesPreview() {
    PetCareTheme {
        val viewModel = PetProfileViewModel()
        viewModel.onTabSelected(1)
        PetProfileScreen(viewModel = viewModel)
    }
}

@Preview(showBackground = true)
@Composable
fun PetProfileScreenEventsPreview() {
    PetCareTheme {
        val viewModel = PetProfileViewModel()
        viewModel.onTabSelected(2)
        PetProfileScreen(viewModel = viewModel)
    }
}
