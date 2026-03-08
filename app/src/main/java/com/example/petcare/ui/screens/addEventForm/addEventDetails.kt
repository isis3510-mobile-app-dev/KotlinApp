package com.example.petcare.ui.screens.addEventForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.ButtonOutline
import com.example.petcare.ui.components.IconCardButton
import com.example.petcare.ui.components.Stepper
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.components.TransparentTopBar
import com.example.petcare.ui.theme.PetCareTheme


@Composable
fun AddEventDetailsForm(){
    PetCareTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            TransparentTopBar(title = "Add New Event") {
            }

            Stepper(
                currentStep = 2,
                stepLabels = listOf("Basic Info", "Details", "Overview")
            )


            TextFieldComponent(
                name = "Description",
                label = "e.g. General Check-up"

            )

            Text(
                text = "Additional Files",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
            )

            IconCardButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Upload",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    )
                },
                text = "Add Documents",
                textBottom = "Tap to upload documents",
                onClick = {
                    println("Card !")
                }
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
                    text = "Continue"
                )

            }

        }
    }

}


@Preview
@Composable
fun AddEventDetailsFormPreview(){
    AddEventDetailsForm()
}
