package com.example.petcare.ui.screens.nfc

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.MainActivity
import com.example.petcare.R
import com.example.petcare.ui.screens.nfc.components.NFCCheckMark
import com.example.petcare.ui.screens.nfc.components.NFCPetCard
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedSuccessScreen(
    onBack: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    val activity     = LocalActivity.current as MainActivity
    val nfcViewModel = activity.nfcViewModel
    val uiState by nfcViewModel.uiState.collectAsStateWithLifecycle()

    val payload = (uiState as? NfcUiState.ReadSuccess)?.payload

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC Tag", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
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
            Spacer(modifier = Modifier.height(16.dp))
            
            NFCCheckMark()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Tag scanned successfully",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SuccessGreen,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            NFCPetCard(
                petName         = payload?.petName       ?: "Unknown pet",
                breedAndSpecies = "${payload?.breed ?: ""} · ${payload?.species ?: ""}",
                photoPath       = R.drawable.pet,
                ownerName       = payload?.ownerName     ?: "",
                ownerPhone      = payload?.ownerPhone    ?: "",
                ownerInitials   = payload?.ownerInitials ?: "?",
                medicalNotes    = "Contact the owner immediately if you found this pet."
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Scan another tag",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { nfcViewModel.readAnother()
                    onDone() } // Modificar una vez sepa que es funcional.
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScannedSuccessScreenPreview() {
    PetCareTheme {
        ScannedSuccessScreen()
    }
}
