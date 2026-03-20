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
import com.example.petcare.data.analytics.FeatureClicksTracker
import com.example.petcare.ui.theme.PetCareTheme


@Composable
fun AddVaccineDetailsForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddVaccineViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            Stepper(currentStep = 2, stepLabels = listOf("Basic Info", "Details", "Overview"))

            TextFieldComponent(
                name = "Administered By", label = "e.g. Dr. Smith",
                value = state.administeredBy, onValueChange = viewModel::setAdministeredBy
            )
            TextFieldComponent(
                name = "Lot Number (optional)", label = "e.g. LOT123",
                value = state.lotNumber, onValueChange = viewModel::setLotNumber
            )
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
                bgColor = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.surface,
                width = 169.dp,
                height = 50.57.dp,
                text = "Continue",
                onclick = {
                    FeatureClicksTracker.recordClick()
                    onclick()
                }
            )
        }
    }
}

