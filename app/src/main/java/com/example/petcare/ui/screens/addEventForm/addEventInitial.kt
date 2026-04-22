package com.example.petcare.ui.screens.addEventForm

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
import com.example.petcare.data.model.EventType
import com.example.petcare.ui.components.*
import com.example.petcare.data.analytics.FeatureClicksTracker
import com.example.petcare.ui.screens.pets.PetsViewModel
import com.example.petcare.ui.theme.GrayBorder
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.trimToNullIfBlank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventInitialForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEventViewModel,
    petsViewModel: PetsViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val petsState by petsViewModel.uiState.collectAsStateWithLifecycle()

    // Local dropdown state
    var dropdownExpanded by remember { mutableStateOf(false) }
    var petDropdownExpanded by remember { mutableStateOf(false) }

    val eventTypeOptions = listOf(
        "Checkup" to EventType.CHECKUP,
        "Dental" to EventType.DENTAL,
        "Surgery" to EventType.SURGERY,
        "Vaccine" to EventType.VACCINE,
        "Other" to EventType.OTHER
    )

    val selectedLabel = eventTypeOptions
        .find { it.second == state.eventType }?.first ?: "Checkup"

    val selectedPetName = petsState.pets.find { it.id == state.petId }?.name ?: "Select a Pet"

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
            TransparentTopBar(title = "Add New Event", onBackClick = {
                FeatureClicksTracker.cancelRoute()
                onBack()
            })
            Stepper(currentStep = 1, stepLabels = listOf("Basic Info", "Details", "Overview"))

            // ── Pet Selection Dropdown ────────────────────────
            Column {
                Text(
                    text = "Pet *",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = petDropdownExpanded,
                    onExpandedChange = { petDropdownExpanded = !petDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPetName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = petDropdownExpanded)
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = GrayBorder,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = petDropdownExpanded,
                        onDismissRequest = { petDropdownExpanded = false }
                    ) {
                        petsState.pets.forEach { pet ->
                            DropdownMenuItem(
                                text = { Text(pet.name) },
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
                name = "Event Name *",
                label = "e.g. Doctor's Appointment",
                value = state.title,
                onValueChange = viewModel::setTitle,
                maxLength = InputTextLimits.EVENT_TITLE
            )

            DateTextField(name = "Date *", onDateSelected = viewModel::setDate)

            TimeTextField(
                name = "Time",
                onTimeSelected = viewModel::setTime
            )

            // ── Controlled EventType dropdown ────────────────────────
            Column {
                Text(
                    text = "Event Type *",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = GrayBorder,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        eventTypeOptions.forEach { (label, type) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.setEventType(type)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        ButtonDefault(
            bgColor = MaterialTheme.colorScheme.secondary,
            textColor = MaterialTheme.colorScheme.surface,
            width = 342.dp,
            height = 56.dp,
            text = "Continue",
            onclick = {
                if (state.petId.isNotBlank() && state.title.trimToNullIfBlank() != null && state.date.isNotBlank()) {
                    FeatureClicksTracker.recordClick()
                    onclick()
                } else if (state.title.trimToNullIfBlank() == null) {
                    viewModel.clearError()
                }
            }
        )
    }
}

@Preview
@Composable
fun AddEventInitialFormPreview() {
    AddEventInitialForm(
        onclick = {},
        onBack = {},
        viewModel = viewModel(),
        petsViewModel = viewModel()
    )
}
