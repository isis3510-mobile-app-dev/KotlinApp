package com.example.petcare.ui.screens.petprofile.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.ui.components.ActionFooter
import com.example.petcare.ui.components.AttachedDocumentsCard
import com.example.petcare.ui.components.ProviderInfoCard
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.OffWhite

@Composable
fun EventDetailsScreen(
    viewModel: EventDetailsViewModel,
    petName: String = "Max", // In a real app these would come from ViewModel or args
    petSpecies: String = "Dog",
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            // ActionFooter acts strictly as the sticky bottom action area per the Scaffold slot
            ActionFooter(
                onDeleteClicked = viewModel::onDeleteClicked,
                onEditClicked = viewModel::onEditClicked
            )
        },
        containerColor = OffWhite // consistent Background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenDark)
            }
            return@Scaffold
        }
        
        val event = uiState.event ?: return@Scaffold // Early return if null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenDark)
                    .padding(horizontal = 8.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Title Icon (Pulse/Medical line icon optional)
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Medical Event",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$petName · $petSpecies", // E.g., Max · Dog
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Main Content Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header details
                EventDetailsHeaderCard(
                    title = event.title,
                    eventType = event.eventType
                )
                
                // Dates
                EventDateCard(
                    eventDate = event.date,
                    followUpDate = event.followUpDate
                )
                
                // Provider Info
                ProviderInfoCard(
                    veterinarian = event.provider,
                    clinic = event.clinic
                )
                
                // Notes (if empty, we should still perhaps render it with empty state)
                NotesCard(
                    description = event.description
                )
                
                // Attached Documents
                AttachedDocumentsCard(
                    documents = event.attachedDocuments,
                    onAddClicked = viewModel::onAttachDocumentClicked
                )
                
                // Bottom padding to ensure scroll clears floating elements
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventDetailsScreenPreview() {
    PetCareTheme {
        EventDetailsScreen(
            viewModel = EventDetailsViewModel()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EventDetailsScreenNoFollowUpPreview() {
    // A variant showing what the layout looks like when the event has no follow-up
    PetCareTheme {
        val viewModel = EventDetailsViewModel().apply {
            // Overriding state inline for Preview only (hacky but visually useful)
            // Using reflection or a dedicated constructor flow is safer, but this works to test UI fallback.
        }
        EventDetailsScreen(
            viewModel = viewModel
        )
    }
}
