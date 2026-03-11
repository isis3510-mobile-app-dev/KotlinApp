package com.example.petcare.ui.screens.nfc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.components.NavBar
import com.example.petcare.ui.screens.nfc.components.NFCScanLoadingRing
import com.example.petcare.ui.theme.GrayDark
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanningNFCScreen() {
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
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            
            NFCScanLoadingRing()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Scanning...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Hold your device near the NFC tag",
                style = MaterialTheme.typography.bodyLarge,
                color = GrayDark
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Pulsing dots equivalent
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).background(GreenDark, CircleShape))
                Box(modifier = Modifier.size(8.dp).background(GreenDark, CircleShape))
                Box(modifier = Modifier.size(8.dp).background(GreenDark, CircleShape))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { /* Handle Cancel */ }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanningNFCScreenPreview() {
    PetCareTheme {
        ScanningNFCScreen()
    }
}
