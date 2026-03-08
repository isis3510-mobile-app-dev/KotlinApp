package com.example.petcare.ui.screens.addPetForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.IconCardButton
import com.example.petcare.ui.components.SpeciesSelector
import com.example.petcare.ui.components.Stepper
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.components.TransparentTopBar
import com.example.petcare.ui.theme.PetCareTheme


@Composable
fun AddPetInitialForm(){
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
                currentStep = 1,
                stepLabels = listOf("Basic Info", "Details", "Medical")
            )

            IconCardButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Photo",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    )
                },
                text = "Add Photo",
                textBottom = "Tap to upload or take a photo",
                onClick = {
                    println("Card !")
                }
            )

            TextFieldComponent(
                name = "Pet Name *",
                label = "e.g. Buddy"

            )

            TextFieldComponent(
                name = "Breed",
                label = "e.g. Golden Retriever"
            )

            SpeciesSelector { selected ->
                println("Selected: $selected")
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
fun AddPetInitialFormPreview(){
    AddPetInitialForm()
}