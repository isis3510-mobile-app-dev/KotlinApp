package com.example.petcare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.theme.ErrorContainer
import com.example.petcare.ui.theme.ErrorContent
import com.example.petcare.ui.theme.GrayDark
import com.example.petcare.ui.theme.GrayLight
import com.example.petcare.ui.theme.GrayMedium
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.PurpleContainer
import com.example.petcare.ui.theme.PurpleContent
import com.example.petcare.util.DisplayTextLimits
import com.example.petcare.util.truncateForDisplay

data class MedicalEventData(
    val eventType: String,
    val petName: String,
    val clinicName: String,
    val date: String,
    val time: String,
    val cost: String
)

@Composable
fun MedicalEventItem(event: MedicalEventData, onClick: () -> Unit = {}
){
    val (icon, iconColor, iconBackground) = when (event.eventType.lowercase()){
        "checkup" -> Triple(Icons.Default.ContentPaste, PurpleContent, PurpleContainer)
        "emergency" -> Triple(Icons.Default.AddBox, ErrorContent, ErrorContainer)
        "dental" -> Triple(Icons.Default.HealthAndSafety, GrayDark, GrayLight)
        else -> Triple(Icons.Default.MedicalServices, GrayText, GrayMedium)
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable{ onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = event.eventType,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.eventType.truncateForDisplay(DisplayTextLimits.COMPACT_TITLE),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${event.petName} · ${event.clinicName}"
                        .truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${event.date} · ${event.time} · ${event.cost}"
                        .truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Detail",
                tint = Color.Gray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MedicalEventItemPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        MedicalEventItem(MedicalEventData("Checkup", "Max", "Happy Paws Clinic", "Nov 19, 2024", "10:00 AM", "$120"))
        MedicalEventItem(MedicalEventData("Checkup", "Luna", "Cat Care Center", "Oct 14, 2024", "02:00 PM", "$95"))
        MedicalEventItem(MedicalEventData("Emergency", "Luna", "City Animal Emergency", "Aug 29, 2024", "11:30 PM", "$340"))
        MedicalEventItem(MedicalEventData("Dental", "Max", "City Vet Center", "Jun 4, 2024", "09:15 AM", "$280"))
    }
}
