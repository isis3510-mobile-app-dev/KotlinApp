package com.example.petcare.ui.screens.addEventForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.ui.components.*
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.RobotoBold
import com.example.petcare.ui.theme.RobotoRegular



@Composable
fun AddEventInitialForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEventViewModel
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
                TransparentTopBar(title = "Add New Event", onBackClick = onBack)
                Stepper(currentStep = 1, stepLabels = listOf("Basic Info", "Details", "Overview"))

                TextFieldComponent(
                    name = "Event Name *", label = "e.g. Doctor's Appointment",
                    value = state.title, onValueChange = viewModel::setTitle
                )
                DateTextField(name = "Date *", onDateSelected = viewModel::setDate)
                TextFieldComponent(
                    name = "Time", label = "e.g. 9:00 am",
                    value = state.time, onValueChange = viewModel::setTime
                )
                DropdownSelector(
                    title = "Event Type",
                    options = listOf("checkup", "dental", "surgery", "vaccine", "other"),
                    onOptionSelected = viewModel::setEventType
                )
            }
            ButtonDefault(
                bgColor = MaterialTheme.colorScheme.secondary,
                textColor = Color.White,
                width = 342.dp, height = 56.dp, text = "Continue",
                onclick = { if (state.title.isNotBlank() && state.date.isNotBlank()) onclick() }
            )
        }
    }
}


@Preview
@Composable
fun AddEventInitialFormPreview(){
    AddEventInitialForm(
        onclick = {},
        onBack = {},
        viewModel = AddEventViewModel()
    )
}