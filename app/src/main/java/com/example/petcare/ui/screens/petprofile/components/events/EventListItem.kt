package com.example.petcare.ui.screens.petprofile.components.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.R
import com.example.petcare.data.model.EventType
import com.example.petcare.data.model.MedicalEvent
import com.example.petcare.ui.theme.GrayDark
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.GreenLight
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.OffWhite
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.DentalContainer
import com.example.petcare.ui.theme.ErrorContainer
import com.example.petcare.ui.theme.InfoContainer

@Composable
fun EventListItem(
    event: MedicalEvent,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable { onClick(event.id) }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left Icon
            val iconBackground = when(event.eventType) {
                EventType.CHECKUP, EventType.OTHER -> SuccessContainer
                EventType.DENTAL -> DentalContainer
                EventType.SURGERY -> ErrorContainer
                EventType.VACCINE -> InfoContainer
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                // Determine icon based on type. The mockups have specific icons (e.g. tooth for Dental).
                // If checking for Dental we can use an emoji or vector if available, but for now we'll mock it 
                // using Text for emojis if we don't have dedicated tooth icons
                when(event.eventType) {
                    EventType.DENTAL -> Text("🦷", fontSize = 24.sp)
                    EventType.CHECKUP -> Icon(Icons.Default.LocalHospital, null, tint = GrayDark)
                    EventType.SURGERY -> Icon(Icons.Default.MedicalServices, null, tint = GrayDark)
                    EventType.VACCINE -> Icon(Icons.Default.Vaccines, null, tint = GrayDark)
                    EventType.OTHER -> Text("⚕️", fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Content
            Column(modifier = Modifier.weight(1f)) {
                // Top Row: Title & Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (event.price != null) {
                        Text(
                            text = "$${event.price.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GrayDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Provider and Date
                Text(
                    text = "${event.provider} · ${event.clinic}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.W500 // Slightly bold
                )

                // Conditionally render Follow-up pill
                if (event.followUpDate != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenLight)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = "Follow Up",
                            tint = GreenDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Follow-up: ${event.followUpDate}",
                            color = GreenDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun EventListItemPreview() {
    PetCareTheme {
        Box(modifier = Modifier.background(OffWhite).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EventListItem(
                    event = MedicalEvent(
                        id = "1",
                        petId = "1",
                        title = "Checkup",
                        eventType = EventType.CHECKUP,
                        price = 120.0,
                        provider = "Dr. Smith",
                        clinic = "Happy Paws Clinic",
                        date = "Nov 19, 2024",
                        description = "Annual wellness exam. All vitals normal. Weight stable at 28.5kg.",
                        followUpDate = "Nov 19, 2025"
                    ),
                    onClick = {}
                )

                EventListItem(
                    event = MedicalEvent(
                        id = "2",
                        petId = "1",
                        title = "Dental",
                        eventType = EventType.DENTAL,
                        price = 280.0,
                        provider = "Dr. Johnson",
                        clinic = "City Vet Center",
                        date = "Jun 4, 2024",
                        description = "Routine dental cleaning. No extractions needed."
                    ),
                    onClick = {}
                )
            }
        }
    }
}
