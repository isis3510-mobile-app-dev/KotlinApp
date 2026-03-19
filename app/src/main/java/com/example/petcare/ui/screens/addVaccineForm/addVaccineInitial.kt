package com.example.petcare.ui.screens.addVaccineForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.ui.components.*
import com.example.petcare.ui.screens.pets.PetsViewModel
import com.example.petcare.ui.theme.GrayBorder
import com.example.petcare.ui.theme.PetCareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVaccineInitialForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddVaccineViewModel,
    petsViewModel: PetsViewModel
) {
    val state     by viewModel.state.collectAsStateWithLifecycle()
    val petsState by petsViewModel.uiState.collectAsStateWithLifecycle()

    var petDropdownExpanded     by remember { mutableStateOf(false) }
    var vaccineDropdownExpanded by remember { mutableStateOf(false) }

    val selectedPetName = petsState.pets.find { it.id == state.petId }?.name ?: "Select a Pet"

    // Load catalog when the selected pet changes (to filter by species)
    val selectedPet = petsState.pets.find { it.id == state.petId }
    LaunchedEffect(state.petId) {
        viewModel.loadCatalog(petSpecies = selectedPet?.species ?: "")
    }

    Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TransparentTopBar(title = "Add New Vaccine", onBackClick = onBack)
                Stepper(currentStep = 1, stepLabels = listOf("Basic Info", "Details", "Overview"))

                // ── Pet dropdown ─────────────────────────────────────────────
                Column {
                    Text(
                        text     = "Pet *",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded         = petDropdownExpanded,
                        onExpandedChange = { petDropdownExpanded = !petDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value         = selectedPetName,
                            onValueChange = {},
                            readOnly      = true,
                            trailingIcon  = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = petDropdownExpanded)
                            },
                            shape    = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors   = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = GrayBorder,
                                focusedTextColor     = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor   = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        ExposedDropdownMenu(
                            expanded         = petDropdownExpanded,
                            onDismissRequest = { petDropdownExpanded = false }
                        ) {
                            petsState.pets.forEach { pet ->
                                DropdownMenuItem(
                                    text    = { Text(pet.name) },
                                    onClick = {
                                        viewModel.setPetId(pet.id)
                                        viewModel.setSelectedVaccine(null) // reset vaccine when pet changes
                                        petDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Vaccine catalog dropdown ──────────────────────────────────
                Column {
                    Text(
                        text     = "Vaccine *",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (state.isCatalogLoading) {
                        Box(
                            modifier          = Modifier.fillMaxWidth().height(53.dp),
                            contentAlignment  = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        ExposedDropdownMenuBox(
                            expanded         = vaccineDropdownExpanded,
                            onExpandedChange = {
                                // Only open if a pet is selected (so catalog is filtered)
                                if (state.petId.isNotBlank()) vaccineDropdownExpanded = !vaccineDropdownExpanded
                            }
                        ) {
                            OutlinedTextField(
                                value         = state.selectedVaccine?.name ?: "Select a Vaccine",
                                onValueChange = {},
                                readOnly      = true,
                                enabled       = state.petId.isNotBlank(),
                                placeholder   = { Text("Select a pet first") },
                                trailingIcon  = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = vaccineDropdownExpanded)
                                },
                                shape    = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors   = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = MaterialTheme.colorScheme.secondary,
                                    unfocusedBorderColor = GrayBorder,
                                    focusedTextColor     = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor   = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            ExposedDropdownMenu(
                                expanded         = vaccineDropdownExpanded,
                                onDismissRequest = { vaccineDropdownExpanded = false }
                            ) {
                                if (state.catalogVaccines.isEmpty()) {
                                    DropdownMenuItem(
                                        text    = { Text("No vaccines available", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { vaccineDropdownExpanded = false }
                                    )
                                } else {
                                    state.catalogVaccines.forEach { vaccine ->
                                        DropdownMenuItem(
                                            text    = {
                                                Column {
                                                    Text(vaccine.name, style = MaterialTheme.typography.bodyMedium)
                                                    if (vaccine.productName.isNotBlank()) {
                                                        Text(
                                                            vaccine.productName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.setSelectedVaccine(vaccine)
                                                vaccineDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Date ─────────────────────────────────────────────────────
                DateTextField(name = "Date *", onDateSelected = viewModel::setDateGiven)
            }

            ButtonDefault(
                bgColor   = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.onSecondary,
                width     = 342.dp,
                height    = 56.dp,
                text      = "Continue",
                onclick   = {
                    if (state.petId.isNotBlank()
                        && state.selectedVaccine != null
                        && state.dateGiven.isNotBlank()
                    ) onclick()
                }
            )
    }