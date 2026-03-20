package com.example.petcare.ui.screens.petprofile.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
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
import com.example.petcare.util.EventDateUtils

@Composable
fun EventDateCard(
    eventDate: String,
    followUpDate: String?,
    modifier: Modifier = Modifier
) {
    val (date, time) = EventDateUtils.splitToAppDateTime(eventDate)
    val followUpLabel = followUpDate?.let { EventDateUtils.splitToAppDateTime(it).first }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DATE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Event Date",
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Date", style = MaterialTheme.typography.bodySmall, color = GrayText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            if (time.isNotBlank()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TIME",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Event Time",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Time", style = MaterialTheme.typography.bodySmall, color = GrayText)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
        
        if (followUpLabel != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = GrayMedium, thickness = 1.dp)
            
            // Follow-up Date Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Follow-up Date",
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Follow-up Date", style = MaterialTheme.typography.bodySmall, color = GrayText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = followUpLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
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
