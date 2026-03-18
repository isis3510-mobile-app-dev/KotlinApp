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
import com.example.petcare.data.model.EventType
import com.example.petcare.ui.components.*
import com.example.petcare.ui.theme.GrayBorder
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.PetCareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventInitialForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEventViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Local dropdown state
    var dropdownExpanded by remember { mutableStateOf(false) }

    val eventTypeOptions = listOf(
        "Checkup"  to EventType.CHECKUP,
        "Dental"   to EventType.DENTAL,
        "Surgery"  to EventType.SURGERY,
        "Vaccine"  to EventType.VACCINE,
        "Other"    to EventType.OTHER
    )

    val selectedLabel = eventTypeOptions
        .find { it.second == state.eventType }?.first ?: "Checkup"

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
                TransparentTopBar(title = "Add New Event", onBackClick = onBack)
                Stepper(currentStep = 1, stepLabels = listOf("Basic Info", "Details", "Overview"))

                TextFieldComponent(
                    name          = "Event Name *",
                    label         = "e.g. Doctor's Appointment",
                    value         = state.title,
                    onValueChange = viewModel::setTitle
                )

                DateTextField(name = "Date *", onDateSelected = viewModel::setDate)

                TextFieldComponent(
                    name          = "Time",
                    label         = "e.g. 9:00 am",
                    value         = state.time,
                    onValueChange = viewModel::setTime
                )

                // ── Controlled EventType dropdown ────────────────────────
                Column {
                    Text(
                        text  = "Event Type *",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded          = dropdownExpanded,
                        onExpandedChange  = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value         = selectedLabel,
                            onValueChange = {},
                            readOnly      = true,
                            trailingIcon  = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                            },
                            shape    = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors   = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = GrayBorder,
                                focusedTextColor     = Color.Black,
                                unfocusedTextColor   = Color.Black
                            )
                        )

                        ExposedDropdownMenu(
                            expanded          = dropdownExpanded,
                            onDismissRequest  = { dropdownExpanded = false }
                        ) {
                            eventTypeOptions.forEach { (label, type) ->
                                DropdownMenuItem(
                                    text    = { Text(label) },
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
                bgColor = com.example.petcare.ui.theme.GreenDark,
                textColor = Color.White,
                width     = 342.dp,
                height    = 56.dp,
                text      = "Continue",
                onclick   = {
                    if (state.title.isNotBlank() && state.date.isNotBlank()) onclick()
                }
            )
        }
    }
}

@Preview
@Composable
fun AddEventInitialFormPreview() {
    AddEventInitialForm(onclick = {}, onBack = {}, viewModel = AddEventViewModel())
}