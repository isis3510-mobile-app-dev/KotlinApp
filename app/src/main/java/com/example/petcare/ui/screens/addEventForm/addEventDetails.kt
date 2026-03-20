package com.example.petcare.ui.screens.addEventForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.data.analytics.FeatureClicksTracker
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.ButtonOutline
import com.example.petcare.ui.components.Stepper
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.components.TransparentTopBar
import com.example.petcare.util.InputTextLimits


@Composable
fun AddEventDetailsForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEventViewModel
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
            TransparentTopBar(title = "Add New Event", onBackClick = onBack)
            Stepper(currentStep = 2, stepLabels = listOf("Basic Info", "Details", "Overview"))

            TextFieldComponent(
                name = "Description", label = "e.g. General Check-up",
                value = state.description,
                onValueChange = viewModel::setDescription,
                maxLength = InputTextLimits.NOTES
            )
            TextFieldComponent(
                name = "Provider / Doctor", label = "e.g. Dr. Smith",
                value = state.provider,
                onValueChange = viewModel::setProvider,
                maxLength = InputTextLimits.PROVIDER_OR_CLINIC
            )
            TextFieldComponent(
                name = "Clinic", label = "e.g. Happy Paws Clinic",
                value = state.clinic,
                onValueChange = viewModel::setClinic,
                maxLength = InputTextLimits.PROVIDER_OR_CLINIC
            )

            // FIX: price field decimal only, no text allowed
            TextFieldComponent(
                name = "Price (optional)", label = "e.g. 50",
                value = state.price,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                    val dotCount = filtered.count { it == '.' }
                    if (dotCount <= 1) viewModel.setPrice(filtered)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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