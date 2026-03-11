package com.example.petcare.ui.screens.petprofile.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.data.model.EventType
import com.example.petcare.ui.theme.GrayDark
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.GreenLight
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.OffWhite

@Composable
fun EventDetailsHeaderCard(
    title: String,
    eventType: EventType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            // Status Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(GreenLight)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Determine icon based on type
                when(eventType) {
                    EventType.DENTAL -> Text("🦷", fontSize = 16.sp)
                    EventType.CHECKUP -> Icon(Icons.Default.LocalHospital, null, tint = GreenDark, modifier = Modifier.size(16.dp))
                    EventType.SURGERY -> Icon(Icons.Default.MedicalServices, null, tint = GreenDark, modifier = Modifier.size(16.dp))
                    EventType.VACCINE -> Icon(Icons.Default.Vaccines, null, tint = GreenDark, modifier = Modifier.size(16.dp))
                    EventType.OTHER -> Text("⚕️", fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = eventType.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = GreenDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun EventDetailsHeaderCardPreview() {
    PetCareTheme {
        Box(modifier = Modifier.background(OffWhite).padding(16.dp)) {
            EventDetailsHeaderCard(
                title = "Checkup",
                eventType = EventType.CHECKUP
            )
        }
    }
}
