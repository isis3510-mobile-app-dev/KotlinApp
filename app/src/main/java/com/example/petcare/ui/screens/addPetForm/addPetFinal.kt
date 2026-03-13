package com.example.petcare.ui.screens.addPetForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.RobotoBold
import com.example.petcare.ui.theme.RobotoRegular


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
                TransparentTopBar(title = "Add New Pet") {
                }

                Stepper(
                    currentStep = 3,
                    stepLabels = listOf("Basic Info", "Details", "Medical")
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
                            text = "Add optional medical info. You can always update this later from the pet's profile.",
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

                Card(
                    modifier = Modifier
                        .size( width = 350.dp, height = 68.dp ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA),
                    ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.Contactless,
                            contentDescription = "NFC Scan Logo",
                            tint = GreenDark,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Set up NFC tag later?",
                                textAlign = TextAlign.Start,
                                fontFamily = RobotoBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )

                            Text(
                                text = "Write your pet's info to an NFC tag from their profile",
                                textAlign = TextAlign.Start,
                                fontFamily = RobotoRegular,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

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