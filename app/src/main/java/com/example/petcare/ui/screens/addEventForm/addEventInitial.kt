package com.example.petcare.ui.screens.addEventForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.DateTextField
import com.example.petcare.ui.components.DropdownSelector
import com.example.petcare.ui.components.Stepper
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.components.TransparentTopBar
import com.example.petcare.ui.theme.PetCareTheme


@Composable
fun AddEventInitialForm(){
    PetCareTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(24.dp)
             ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                TransparentTopBar(title = "Add New Event") {
                }

                Stepper(
                    currentStep = 1,
                    stepLabels = listOf("Basic Info", "Details", "Overview")
                )


                TextFieldComponent(
                    name = "Event Name * ",
                    label = "e.g. Doctor's Appointment"

                )

                DateTextField(
                    name = "Date *",
                    onDateSelected = { date ->
                        println("Selected date: $date")
                    }
                )

                TextFieldComponent(
                    name = "Time",
                    label = "e.g. 9:00 am"
                )

                DropdownSelector(
                    title = "Pet Name * ",
                    options = listOf("Max", "Luna", "Coco")
                ) {}

            }

            ButtonDefault(
                bgColor = MaterialTheme.colorScheme.secondary,
                textColor = Color.White,
                width = 342.dp,
                height = 56.dp,
                text = "Continue"
            )

        }
    }

}


@Preview
@Composable
fun AddEventInitialFormPreview(){
    AddEventInitialForm()
}