package com.example.petcare.ui.screens.nfc

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.MainActivity
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.screens.nfc.components.NFCHeader
import com.example.petcare.ui.screens.nfc.components.NFCToggle
import com.example.petcare.ui.screens.nfc.components.PetOption
import com.example.petcare.ui.screens.nfc.components.PetSelectionRow
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteNFCScreen(
    onBack: () -> Unit = {},
    onDone: () -> Unit = {},
    onRead: () -> Unit = {}
) {
    val activity = LocalActivity.current as MainActivity
    val nfcViewModel = activity.nfcViewModel
    val uiState by nfcViewModel.uiState.collectAsStateWithLifecycle()

    // ── Load pets from the shared PetsViewModel ────────────────────────────
    val petsUiState by activity.petsViewModel.uiState.collectAsStateWithLifecycle()
    val pets = petsUiState.pets

    // Map domain Pet → PetOption used by the selection row
    // We use a placeholder image (R.drawable.pet) until photo loading is added
    val petOptions = pets.map { pet ->
        PetOption(
            id        = pet.id,
            name      = pet.name,
            imageRes  = com.example.petcare.R.drawable.pet
        )
    }

    // Default to the first real pet (or empty when list is empty)
    var selectedPetId by remember(petOptions) {
        mutableStateOf(petOptions.firstOrNull()?.id ?: "")
    }

    val selectedPet = petOptions.find { it.id == selectedPetId } ?: petOptions.firstOrNull()

    // Navigate to ScanningNFCScreen once the payload is ready
    LaunchedEffect(uiState) {
        when (uiState) {
            is NfcUiState.WaitingForTag -> onDone()
            is NfcUiState.Error         -> { /* handled in ScanningScreen */ }
            else                        -> Unit
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC Tag", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = Color.Transparent,
                    scrolledContainerColor     = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor          = Color.Unspecified,
                    actionIconContentColor     = Color.Unspecified
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            NFCToggle(isReadMode = false, onModeChanged = { onRead() })

            Spacer(modifier = Modifier.height(24.dp))

            when {
                petsUiState.isLoading -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(color = GreenDark)
                }

                petOptions.isEmpty() -> {
                    Spacer(modifier = Modifier.height(48.dp))
                    NFCHeader(
                        title    = "No pets found",
                        subtitle = "Add a pet first before writing an NFC tag"
                    )
                }

                else -> {
                    PetSelectionRow(
                        pets          = petOptions,
                        selectedPetId = selectedPetId,
                        onPetSelected = { selectedPetId = it }
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    NFCHeader(
                        title    = "Write Tag for ${selectedPet?.name ?: ""}",
                        subtitle = "Hold your phone near a blank NFC tag to write ${selectedPet?.name ?: "your pet"}'s emergency info"
                    )

                    Spacer(modifier = Modifier.height(64.dp))

                    ButtonDefault(
                        bgColor   = GreenDark,
                        textColor = Color.White,
                        width     = 342.dp,
                        height    = 56.dp,
                        text      = "Start Writing",
                        onclick   = {
                            if (selectedPetId.isNotBlank()) {
                                nfcViewModel.prepareWrite(selectedPetId, "")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WriteNFCScreenPreview() {
    PetCareTheme {
        WriteNFCScreen()
    }
}
