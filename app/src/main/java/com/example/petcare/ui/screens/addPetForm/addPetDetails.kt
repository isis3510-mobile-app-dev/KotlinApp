package com.example.petcare.ui.screens.addPetForm

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
import androidx.compose.material3.Text
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
import com.example.petcare.ui.components.DateTextField
import com.example.petcare.ui.components.GenderSelector
import com.example.petcare.ui.components.Stepper
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.components.TransparentTopBar
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits

@Composable
fun AddPetDetailsForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddPetViewModel
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TransparentTopBar(title = "Add New Pet", onBackClick = onBack)

            Stepper(currentStep = 2, stepLabels = listOf("Basic Info", "Details", "Medical"))

            GenderSelector(onOptionSelected = viewModel::setGender)

            TextFieldComponent(
                name = "Weight (Kg)",
                label = "e.g. 4.5",
                value = state.weight,
                onValueChange = viewModel::setWeight,
                maxLength = InputTextLimits.WEIGHT,
                fieldPolicy = InputFieldPolicy.DECIMAL,
                maxNumericValue = 199.0,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            TextFieldComponent(
                name = "Color / Markings",
                label = "e.g. Golden, White Chest",
                value = state.color,
                onValueChange = viewModel::setColor,
                maxLength = InputTextLimits.COLOR
            )

            // FIX: restrict future dates for birth date
            DateTextField(
                name = "Date of Birth",
                onDateSelected = viewModel::setBirthDate,
                allowFutureDates = false
            )
        }

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row {
            ButtonOutline(
                bgColor = MaterialTheme.colorScheme.background,
                outlineColor = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.secondary,
                width = 169.dp, height = 50.57.dp,
                text = "Back", onclick = onBack
            )
            Spacer(modifier = Modifier.width(10.dp))
            ButtonDefault(
                bgColor = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.surface,
                width = 169.dp, height = 50.57.dp,
                text = "Continue", onclick = {
                    if (state.gender.isBlank()) {
                        viewModel.setError("Gender is required")
                    } else {
                        viewModel.clearError()
                        FeatureClicksTracker.recordClick()
                        onclick()
                    }
                }
            )
        }
    }
}
