package com.example.petcare.ui.screens.addPetForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.ui.components.*
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.RobotoBold
import com.example.petcare.ui.theme.RobotoRegular

@Composable
fun AddPetFinalForm(
    onBack: () -> Unit,
    /**
     * Called with the newly created petId so the caller can navigate
     * directly to that pet's profile.
     */
    onclick: (petId: String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TransparentTopBar(title = "Add New Pet", onBackClick = onBack)

            Stepper(currentStep = 3, stepLabels = listOf("Basic Info", "Details", "Medical"))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(width = 350.dp, height = 88.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column {
                    Text(
                        text = "Almost done!",
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        textAlign = TextAlign.Start, fontFamily = RobotoBold,
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Add optional medical info. You can always update this later from the pet's profile.",
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp),
                        textAlign = TextAlign.Start, fontFamily = RobotoRegular,
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary,
                        lineHeight = 16.sp
                    )
                }
            }

            TextFieldComponent(
                name = "Veterinarian", label = "e.g. Dr. Smith",
                value = state.defaultVet, onValueChange = viewModel::setDefaultVet
            )
            TextFieldComponent(
                name = "Clinic Name", label = "e.g. Happy Paw Clinic",
                value = state.defaultClinic, onValueChange = viewModel::setDefaultClinic
            )
            TextFieldComponent(
                name = "Known Allergies", label = "e.g. None",
                value = state.knownAllergies, onValueChange = viewModel::setKnownAllergies
            )

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Card(
                modifier = Modifier.size(width = 350.dp, height = 68.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Contactless,
                        contentDescription = null, tint = GreenDark,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Set up NFC tag later?",
                            textAlign = TextAlign.Start, fontFamily = RobotoBold,
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Write your pet's info to an NFC tag from their profile",
                            textAlign = TextAlign.Start, fontFamily = RobotoRegular,
                            fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary,
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
                width = 169.dp, height = 50.57.dp,
                text = "Back", onclick = onBack
            )
            Spacer(modifier = Modifier.width(10.dp))
            ButtonDefault(
                bgColor = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.surface,
                width = 169.dp, height = 50.57.dp,
                text = if (state.isLoading) "Saving…" else "Add Pet",
                onclick = { viewModel.submit { petId -> onclick(petId) } }
            )
        }
    }
}

/**
@Preview
@Composable
fun AddPetFinalFormPreview() {
    AddPetFinalForm(onBack = {}, onclick = {}, viewModel = AddPetViewModel())
}
 **/