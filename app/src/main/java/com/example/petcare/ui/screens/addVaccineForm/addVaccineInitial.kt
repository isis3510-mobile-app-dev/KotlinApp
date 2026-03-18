package com.example.petcare.ui.screens.addVaccineForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.ui.components.*
import com.example.petcare.ui.theme.PetCareTheme

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — Basic Info
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AddVaccineInitialForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddVaccineViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                    if (state.vaccineName.isNotBlank() && state.dateGiven.isNotBlank()) onclick()
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
        viewModel = AddVaccineViewModel()
    )
}