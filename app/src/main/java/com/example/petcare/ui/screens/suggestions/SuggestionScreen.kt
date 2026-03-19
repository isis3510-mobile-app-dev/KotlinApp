package com.example.petcare.ui.screens.suggestions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.data.model.PetFilterChip
import com.example.petcare.ui.components.GroupedSuggestionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionScreen(
    filterPetId: String? = null,
    filterPetName: String? = null,
    onBack: () -> Unit = {},
    viewModel: SuggestionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(filterPetId) { viewModel.load(filterPetId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (filterPetName != null) "$filterPetName's alerts" else "All alerts")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            else -> {
                LazyColumn(
                    modifier              = Modifier.fillMaxSize().padding(padding),
                    contentPadding        = PaddingValues(start = 16.dp, bottom = 24.dp, end = 16.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp)
                ) {

                    if (uiState.availablePets.size > 1) {
                        item {
                            PetFilterRow(
                                chips      = uiState.availablePets,
                                selectedId = uiState.selectedPetId,
                                onSelect   = viewModel::onPetFilterSelected
                            )
                        }
                    }

                    if (uiState.displayed.isEmpty()) {
                        item {
                            Box(
                                modifier         = Modifier.fillMaxWidth().padding(top = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text  = "No alerts at the moment",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(uiState.displayed, key = { it.vaccineTitle }) { grouped ->
                            GroupedSuggestionCard(grouped = grouped)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PetFilterRow(
    chips: List<PetFilterChip>,
    selectedId: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedId == null,
                onClick  = { onSelect(null) },
                label    = { Text("All", fontSize = 13.sp) }
            )
        }
        items(chips) { chip ->
            FilterChip(
                selected = selectedId == chip.petId,
                onClick  = { onSelect(chip.petId) },
                label    = { Text("${chip.petName} (${chip.alertCount})", fontSize = 13.sp) }
            )
        }
    }
}