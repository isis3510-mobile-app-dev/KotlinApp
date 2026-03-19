package com.example.petcare.ui.screens.addPetForm

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.petcare.ui.components.*
import com.example.petcare.ui.theme.PetCareTheme
import java.io.File

@Composable
fun AddPetInitialForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddPetViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPhotoOptions by remember { mutableStateOf(false) }

    // URI temporal para la cámara
    val cameraUri = remember {
        val file = File(context.cacheDir, "photos/temp_${System.currentTimeMillis()}.jpg")
            .also { it.parentFile?.mkdirs() }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.setPhotoUri(cameraUri)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.setPhotoUri(uri)
        showPhotoOptions = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showPhotoOptions = false
            cameraLauncher.launch(cameraUri)
        }
    }

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

            if (state.photoUri != null) {
                Box {
                    AsyncImage(
                        model = state.photoUri,
                        contentDescription = "Pet photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { viewModel.clearPhoto() },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove photo",
                            tint = Color.White
                        )
                    }
                }
            } else {
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
                    onClick = { showPhotoOptions = true }
                )
            }

            if (showPhotoOptions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showPhotoOptions = false
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Camera")
                    }
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gallery")
                    }
                }
            }

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
            textColor = MaterialTheme.colorScheme.surface,
            width = 342.dp,
            height = 56.dp,
            text = "Continue",
            onclick = {
                if (state.name.isNotBlank() && state.species.isNotBlank()) {
                    onclick()
                } else {
                    if (state.name.isBlank()) viewModel.setName("")
                    if (state.species.isBlank()) viewModel.setSpecies("")
                    viewModel.clearError()
                }
            }
        )
    }
}


/**
@Preview
@Composable
fun AddPetInitialFormPreview() {
    AddPetInitialForm(
        onclick = {},
        onBack = {},
        viewModel = AddPetViewModel()
    )
}
 **/