package com.example.petcare.ui.screens.nfc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.petcare.R
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.NavBar
import com.example.petcare.ui.screens.nfc.components.NFCHeader
import com.example.petcare.ui.screens.nfc.components.NFCToggle
import com.example.petcare.ui.screens.nfc.components.PetOption
import com.example.petcare.ui.screens.nfc.components.PetSelectionRow
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteNFCScreen() {
    var isReadMode by remember { mutableStateOf(false) } // Default to false for Write Screen
    var selectedPetId by remember { mutableStateOf("1") }
    
    val myPets = listOf(
        PetOption("1", "Max", R.drawable.pet),
        PetOption("2", "Luna", R.drawable.pet),
        PetOption("3", "Coco", R.drawable.pet)
    )
    
    val selectedPet = myPets.find { it.id == selectedPetId } ?: myPets.first()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC Tag", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back navigation */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            NavBar(currentRoute = "home", onItemClick = {})
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            NFCToggle(isReadMode = isReadMode, onModeChanged = { isReadMode = it })
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PetSelectionRow(
                pets = myPets,
                selectedPetId = selectedPetId,
                onPetSelected = { selectedPetId = it }
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            NFCHeader(
                title = "Write Tag for ${selectedPet.name}",
                subtitle = "Hold your phone near a blank NFC tag to write ${selectedPet.name}'s emergency info"
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            ButtonDefault(
                bgColor = GreenDark,
                textColor = Color.White,
                width = 342.dp,
                height = 56.dp,
                text = "Start Writing"
            )
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
