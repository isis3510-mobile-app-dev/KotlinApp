package com.example.petcare.ui.screens.petprofile.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.HorizontalDivider
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
import com.example.petcare.ui.theme.GrayMedium
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.OffWhite

@Composable
fun EventDateCard(
    eventDate: String,
    followUpDate: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Text(
            text = "DATE",
            style = MaterialTheme.typography.labelMedium,
            color = GrayText,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Event Date Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = "Event Date",
                tint = GrayText,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Event Date", style = MaterialTheme.typography.bodySmall, color = GrayText)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = eventDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
            }
        }
        
        if (followUpDate != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = GrayMedium, thickness = 1.dp)
            
            // Follow-up Date Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Follow-up Date",
                    tint = GrayText,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Follow-up Date", style = MaterialTheme.typography.bodySmall, color = GrayText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = followUpDate,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun EventDateCardPreview() {
    PetCareTheme {
        Box(modifier = Modifier.background(OffWhite).padding(16.dp)) {
            EventDateCard(
                eventDate = "Nov 19, 2024",
                followUpDate = "Nov 19, 2025"
            )
        }
    }
}
