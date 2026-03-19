package com.example.petcare.ui.screens.addVaccineForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.ui.components.*
import com.example.petcare.ui.screens.pets.PetsViewModel
import com.example.petcare.ui.theme.GrayBorder
import com.example.petcare.ui.theme.PetCareTheme

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — Basic Info
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVaccineInitialForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddVaccineViewModel,
    petsViewModel: PetsViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val petsState by petsViewModel.uiState.collectAsStateWithLifecycle()

    var petDropdownExpanded by remember { mutableStateOf(false) }
    val selectedPetName = petsState.pets.find { it.id == state.petId }?.name ?: "Select a Pet"

    PetCareTheme {
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

                // ── Pet Selection Dropdown ────────────────────────
                Column {
                    Text(
                        text  = "Pet *",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded          = petDropdownExpanded,
                        onExpandedChange  = { petDropdownExpanded = !petDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value         = selectedPetName,
                            onValueChange = {},
                            readOnly      = true,
                            trailingIcon  = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = petDropdownExpanded)
                            },
                            shape    = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors   = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = GrayBorder,
                                focusedTextColor     = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor   = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        ExposedDropdownMenu(
                            expanded          = petDropdownExpanded,
                            onDismissRequest  = { petDropdownExpanded = false }
                        ) {
                            petsState.pets.forEach { pet ->
                                DropdownMenuItem(
                                    text    = { Text(pet.name) },
                                    onClick = {
                                        viewModel.setPetId(pet.id)
                                        petDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                TextFieldComponent(
                    name = "Vaccine Name *", label = "e.g. Rabies",
                    value = state.vaccineName, onValueChange = viewModel::setVaccineName
                )
                DateTextField(name = "Date *", onDateSelected = viewModel::setDateGiven)
                TextFieldComponent(
                    name = "Product Name", label = "e.g. Rabisin",
                    value = state.productName, onValueChange = viewModel::setProductName
                )
                TextFieldComponent(
                    name = "Manufacturer", label = "e.g. Boehringer",
                    value = state.manufacturer, onValueChange = viewModel::setManufacturer
                )
            }
            ButtonDefault(
                bgColor = com.example.petcare.ui.theme.GreenDark,
                textColor = Color.White,
                width = 342.dp, height = 56.dp, text = "Continue",
                onclick = {
                    if (state.petId.isNotBlank() && state.vaccineName.isNotBlank() && state.dateGiven.isNotBlank()) onclick()
                }
            )
        }
    }
}



@Preview
@Composable
fun AddVaccineInitialFormPreview(){
    AddVaccineInitialForm(
        onclick = {},
        onBack = {},
        viewModel = AddVaccineViewModel(),
        petsViewModel = viewModel()
    )
}
