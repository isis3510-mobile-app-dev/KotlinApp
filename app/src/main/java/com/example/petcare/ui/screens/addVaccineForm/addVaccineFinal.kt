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


@Composable
fun AddVaccineFinalForm(
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
                Stepper(currentStep = 3, stepLabels = listOf("Basic Info", "Details", "Overview"))

                // Summary (read-only except administeredBy which can still be edited)
                TextFieldComponent(
                    name = "Vaccine Name", label = state.vaccineName,
                    value = state.vaccineName, onValueChange = {}
                )
                TextFieldComponent(
                    name = "Date Given", label = state.dateGiven,
                    value = state.dateGiven, onValueChange = {}
                )
                TextFieldComponent(
                    name = "Administered By", label = state.administeredBy,
                    value = state.administeredBy, onValueChange = viewModel::setAdministeredBy
                )
                DateTextField(
                    name = "Next Due Date (optional)",
                    onDateSelected = viewModel::setNextDueDate
                )

                state.error?.let {
                    Text(
                        text = it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row {
                ButtonOutline(
                    bgColor = MaterialTheme.colorScheme.background,
                    outlineColor = MaterialTheme.colorScheme.secondary,
                    textColor = MaterialTheme.colorScheme.secondary,
                    width = 169.dp, height = 50.57.dp, text = "Back", onclick = onBack
                )
                Spacer(modifier = Modifier.width(10.dp))
                ButtonDefault(
                    bgColor = com.example.petcare.ui.theme.GreenDark, textColor = Color.White,
                    width = 169.dp, height = 50.57.dp,
                    text = if (state.isLoading) "Saving…" else "Add Vaccine",
                    onclick = { viewModel.submit { onclick() } }
                )
            }
        }
    }
}


@Preview
@Composable
fun AddVaccineFinalFormPreview(){
    AddVaccineFinalForm(
        onclick = {},
        onBack = {},
        viewModel = AddVaccineViewModel()
    )
}