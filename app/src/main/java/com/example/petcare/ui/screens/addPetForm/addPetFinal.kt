package com.example.petcare.ui.screens.addPetForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.ButtonOutline
import com.example.petcare.ui.components.GenderSelector
import com.example.petcare.ui.components.Stepper
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.components.TransparentTopBar
import com.example.petcare.ui.theme.PetCareTheme


@Composable
fun AddPetFinalForm(){
    PetCareTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            TransparentTopBar(title = "Add New Pet") {
            }

            Stepper(
                currentStep = 3,
                stepLabels = listOf("Basic Info", "Details", "Medical")
            )

            GenderSelector() { selected ->
                println("Selected: $selected")
            }

            TextFieldComponent(
                name = "Veterinarian",
                label = "e.g. Dr. Smith"

            )

            TextFieldComponent(
                name = "Clinic Name",
                label = "e.g. Happy Paw Clinic"
            )

            TextFieldComponent(
                name = "Known Allergies",
                label = "e.g. None"
            )

            Row() {
                ButtonOutline(
                    bgColor = MaterialTheme.colorScheme.background,
                    outlineColor = MaterialTheme.colorScheme.secondary,
                    textColor = MaterialTheme.colorScheme.secondary,
                    width = 169.dp,
                    height = 50.57.dp,
                    text = "Back"
                )

                Spacer(modifier = Modifier.width(10.dp))

                ButtonDefault(
                    bgColor = MaterialTheme.colorScheme.secondary,
                    textColor = Color.White,
                    width = 169.dp,
                    height = 50.57.dp,
                    text = "Add Pet"
                )

            }

        }
    }

}


@Preview
@Composable
fun AddPetFinalFormPreview(){
    AddPetFinalForm()
}