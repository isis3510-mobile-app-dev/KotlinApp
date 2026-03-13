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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.ButtonOutline
import com.example.petcare.ui.components.Stepper
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.components.TransparentTopBar
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.RobotoBold
import com.example.petcare.ui.theme.RobotoRegular
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Switch
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.petcare.ui.components.SettingsListItem



@Composable
fun AddPetFinalForm(){
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
                    currentStep = 3,
                    stepLabels = listOf("Basic Info", "Details", "Overview")
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .size(width = 350.dp, height = 88.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column() {
                        Text(
                            text = "Almost done!",
                            modifier = Modifier
                                .padding(start = 16.dp,
                                    top = 16.dp,
                                    end = 16.dp),
                            textAlign = TextAlign.Start,
                            fontFamily = RobotoBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Confirm your event’s information and set a reminder.",
                            modifier = Modifier
                                .padding(start = 16.dp,
                                    top = 4.dp,
                                    end = 16.dp),
                            textAlign = TextAlign.Start,
                            fontFamily = RobotoRegular,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            lineHeight = 16.sp
                        )
                    }
                }

                TextFieldComponent(
                    name = "Event Name",
                    label = "Doctor's Appointment"

                )

                TextFieldComponent(
                    name = "Date",
                    label = "07/03/2026 09:00 am"
                )


                TextFieldComponent(
                    name = "Description",
                    label = "Doctor's Appointment"
                )


                SettingsListItem(
                    icon = Icons.Default.Notifications,
                    iconBackgroundColor = Color(0x99FFECB3),
                    iconTintColor = Color(0xFFFBBC05),
                    title = "Set a reminder",
                    trailingContent = {
                        Switch(
                            checked = true,
                            onCheckedChange = {},
                            modifier = Modifier.semantics { contentDescription = "Set a reminder" }
                        )
                    }
                )


            }
            Row {
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
                    text = "Add Event"
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