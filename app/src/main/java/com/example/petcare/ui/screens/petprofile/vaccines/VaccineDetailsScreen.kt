package com.example.petcare.ui.screens.petprofile.vaccines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineFilterStatus
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineRecord
import com.example.petcare.ui.theme.*
import com.example.petcare.ui.components.AttachedDocumentsCard
import com.example.petcare.data.model.AttachedDocument

@Composable
fun VaccineDetailsScreen(
    viewModel: VaccineDetailsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vaccine = uiState.vaccine

    Scaffold(
        containerColor = OffWhite,
        topBar = {
            Column(modifier = Modifier.background(GreenDark)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vaccine Details",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Max · Dog",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 64.dp, bottom = 16.dp)
                )
            }
        },
        bottomBar = {
            StickyBottomActions(
                onDelete = { viewModel.deleteVaccine() },
                onEdit = { viewModel.editVaccine() }
            )
        }
    ) { paddingValues ->
        if (vaccine != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                VaccineHeaderCard(name = vaccine.name, status = vaccine.status)

                // Timeline Card
                TimelineCard(dateGiven = vaccine.dateGiven, nextDueDate = vaccine.nextDueDate)

                // Provider Card
                // Mock clinic for now as it's not split in model
                ProviderCard(veterinarian = vaccine.provider, clinic = "Happy Paws Clinic")

                // Attached Documents Card
                AttachedDocumentsCard(
                    documents = vaccine.attachedDocumentName?.let { 
                        listOf(AttachedDocument(id = "1", fileName = it)) 
                    } ?: emptyList(),
                    onAddClicked = { /* Add Document action */ }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun VaccineHeaderCard(name: String, status: VaccineFilterStatus) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val (backgroundColor, contentColor, text) = when (status) {
                VaccineFilterStatus.COMPLETED -> Triple(SuccessContainer, SuccessContent, "✓ Completed")
                VaccineFilterStatus.UPCOMING -> Triple(InfoContainer, InfoContent, "Upcoming")
                VaccineFilterStatus.OVERDUE -> Triple(ErrorContainer, ErrorContent, "! Overdue")
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TimelineCard(dateGiven: String, nextDueDate: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "TIMELINE",
                style = MaterialTheme.typography.labelMedium,
                color = GrayText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Date Given", fontSize = 12.sp, color = GrayText)
                    Text(text = dateGiven, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (nextDueDate != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = GrayBorder)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Next Due Date", fontSize = 12.sp, color = GrayText)
                        Text(text = nextDueDate, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(veterinarian: String, clinic: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "PROVIDER INFORMATION",
                style = MaterialTheme.typography.labelMedium,
                color = GrayText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Veterinarian", fontSize = 12.sp, color = GrayText)
                    Text(text = veterinarian, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = GrayBorder)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = GrayText, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Clinic", fontSize = 12.sp, color = GrayText)
                    Text(text = clinic, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}



@Composable
private fun StickyBottomActions(onDelete: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Delete Button Outline
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(30.dp))
                .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(30.dp))
                .clickable(onClick = onDelete)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
        
        // Edit Button Solid
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(30.dp))
                .background(GreenDark)
                .clickable(onClick = onEdit)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text("Edit", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VaccineDetailsScreenPreview() {
    PetCareTheme {
        VaccineDetailsScreen(viewModel = VaccineDetailsViewModel())
    }
}
