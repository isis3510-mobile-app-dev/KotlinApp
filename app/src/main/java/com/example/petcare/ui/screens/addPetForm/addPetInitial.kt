package com.example.petcare.ui.screens.addPetForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.ui.components.*
import com.example.petcare.ui.theme.PetCareTheme

@Composable
fun AddPetInitialForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddPetViewModel
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
                TransparentTopBar(title = "Add New Pet", onBackClick = onBack)

                Stepper(currentStep = 1, stepLabels = listOf("Basic Info", "Details", "Medical"))

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
                    onClick = {}
                )

                TextFieldComponent(
                    name = "Pet Name *",
                    label = "e.g. Buddy",
                    value = state.name,
                    onValueChange = viewModel::setName
                )

                TextFieldComponent(
                    name = "Breed",
                    label = "e.g. Golden Retriever",
                    value = state.breed,
                    onValueChange = viewModel::setBreed
                )

                SpeciesSelector(onOptionSelected = viewModel::setSpecies)
            }

            // Show validation error
            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            ButtonDefault(
                bgColor = MaterialTheme.colorScheme.secondary,
                textColor = Color.White,
                width = 342.dp,
                height = 56.dp,
                text = "Continue",
                onclick = {
                    if (state.name.isNotBlank() && state.species.isNotBlank()) {
                        onclick()
                    } else {
                        // Trigger error message via ViewModel
                        if (state.name.isBlank())    viewModel.setName("")
                        if (state.species.isBlank())  viewModel.setSpecies("")
                        // Force state update so error shows
                        viewModel.clearError()
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun AddPetInitialFormPreview(){
    AddPetInitialForm(
        onclick = {},
        onBack = {},
        viewModel = AddPetViewModel()
    )
}