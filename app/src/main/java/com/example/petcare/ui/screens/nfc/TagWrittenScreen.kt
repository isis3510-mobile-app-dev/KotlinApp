package com.example.petcare.ui.screens.nfc

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.MainActivity
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.ButtonOutline
import com.example.petcare.ui.screens.nfc.components.NFCCheckMark
import com.example.petcare.ui.screens.nfc.components.NFCWriteInfoCard
import com.example.petcare.ui.theme.GrayDark
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagWrittenScreen(
    onBack: () -> Unit = {},
    onDone: () -> Unit = {},
    onAnother: () -> Unit = {},
    // Passed in from NavHost where authViewModel is already available
    ownerName: String = "",
    ownerPhone: String = ""
) {
    val activity     = LocalActivity.current as MainActivity
    val nfcViewModel = activity.nfcViewModel
    val uiState      by nfcViewModel.uiState.collectAsStateWithLifecycle()

    // petName comes from the WriteSuccess state produced by NfcViewModel
    val petName = (uiState as? NfcUiState.WriteSuccess)?.petName ?: "your pet"

    // Find breed from petsViewModel — no need to access authViewModel here
    val petsUiState by activity.petsViewModel.uiState.collectAsStateWithLifecycle()
    val writtenPet  = petsUiState.pets.find { it.name == petName }
    val breedLine   = if (writtenPet != null && writtenPet.breed.isNotBlank()) {
        "${writtenPet.name} · ${writtenPet.breed}"
    } else {
        petName
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC Tag", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        nfcViewModel.reset()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            NFCCheckMark()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text      = "¡Etiqueta escrita!",
                style     = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize  = 24.sp,
                color     = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text       = "La información de $petName se guardó correctamente en la etiqueta NFC. Cualquier persona que la escanee podrá contactarte al instante.",
                style      = MaterialTheme.typography.bodyLarge,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            NFCWriteInfoCard(
                petNameAndBreed = breedLine,
                ownerName       = ownerName.ifBlank { "—" },
                ownerPhone      = ownerPhone.ifBlank { "—" },
                microchip       = "—"
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier  = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ButtonOutline(
                        bgColor      = MaterialTheme.colorScheme.surface,
                        outlineColor = MaterialTheme.colorScheme.secondary,
                        textColor    = MaterialTheme.colorScheme.secondary,
                        width        = 200.dp,
                        height       = 56.dp,
                        text         = "Escribir otra",
                        onclick      = {
                            nfcViewModel.reset()
                            onAnother()
                        }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ButtonDefault(
                        bgColor   = MaterialTheme.colorScheme.secondary,
                        textColor = MaterialTheme.colorScheme.onSecondary,
                        width     = 200.dp,
                        height    = 56.dp,
                        text      = "Listo",
                        onclick   = {
                            nfcViewModel.reset()
                            onDone()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TagWrittenScreenPreview() {
    PetCareTheme {
        TagWrittenScreen(ownerName = "Sarah Johnson", ownerPhone = "+57 300 000 0000")
    }
}