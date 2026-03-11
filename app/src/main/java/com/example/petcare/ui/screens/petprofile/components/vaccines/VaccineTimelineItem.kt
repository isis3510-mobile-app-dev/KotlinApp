package com.example.petcare.ui.screens.petprofile.components.vaccines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
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
import com.example.petcare.ui.theme.ErrorContainer
import com.example.petcare.ui.theme.ErrorContent
import com.example.petcare.ui.theme.GrayBorder
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.InfoContainer
import com.example.petcare.ui.theme.InfoContent
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.SuccessContent
import com.example.petcare.ui.theme.OffWhite

data class VaccineRecord(
    val id: String,
    val name: String,
    val provider: String,
    val dateGiven: String,
    val nextDueDate: String?,
    val lotNumber: String?,
    val status: VaccineFilterStatus,
    val attachedDocumentName: String? = null
)

@Composable
fun VaccineTimelineItem(
    vaccine: VaccineRecord,
    isLastItem: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
    ) {
        // Left Column: Timeline line and indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp)) // Aligns the indicator with the card title
            
            // Indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when (vaccine.status) {
                            VaccineFilterStatus.COMPLETED -> SuccessContent
                            VaccineFilterStatus.UPCOMING -> Color.White // Outline only
                            VaccineFilterStatus.OVERDUE -> ErrorContent
                        }
                    )
                    .border(
                        width = 2.dp,
                        color = when (vaccine.status) {
                            VaccineFilterStatus.UPCOMING -> InfoContent
                            else -> Color.Transparent
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (vaccine.status == VaccineFilterStatus.COMPLETED) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                } else if (vaccine.status == VaccineFilterStatus.OVERDUE) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Overdue",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            // Line
            if (!isLastItem) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(GrayBorder)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Column: Content Card
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(16.dp)
        ) {
            Column {
                // Header: Name and Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = vaccine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    StatusPillSmall(status = vaccine.status)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Provider
                Text(
                    text = vaccine.provider,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Conditionally display dates / lot numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Date Given
                    InfoBlock(label = "Date given", value = vaccine.dateGiven)

                    // Conditional Next Due Date
                    if (vaccine.nextDueDate != null) {
                        InfoBlock(label = "Next due", value = vaccine.nextDueDate)
                    }

                    // Conditional Lot Number
                    if (vaccine.lotNumber != null) {
                        InfoBlock(label = "Lot #", value = vaccine.lotNumber)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPillSmall(status: VaccineFilterStatus) {
    val (backgroundColor, contentColor, text) = when (status) {
        VaccineFilterStatus.COMPLETED -> Triple(SuccessContainer, SuccessContent, "completed")
        VaccineFilterStatus.UPCOMING -> Triple(InfoContainer, InfoContent, "upcoming")
        VaccineFilterStatus.OVERDUE -> Triple(ErrorContainer, ErrorContent, "overdue")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = GrayText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = false)
@Composable
fun VaccineTimelineItemPreview() {
    PetCareTheme {
        Box(modifier = Modifier.background(OffWhite).padding(16.dp)) {
            Column {
                VaccineTimelineItem(
                    vaccine = VaccineRecord("3", "Leptospirosis", "Dr. Johnson · City Vet Center", "Oct 1, 2023", null, null, VaccineFilterStatus.OVERDUE), // Example of missing Next Due Date
                    isLastItem = false,
                    onClick = {}
                )
                VaccineTimelineItem(
                    vaccine = VaccineRecord("2", "Rabies", "Dr. Smith · Happy Paws Clinic", "Mar 14, 2024", "Mar 14, 2025", "LP2024-0315", VaccineFilterStatus.UPCOMING),
                    isLastItem = false,
                    onClick = {}
                )
                VaccineTimelineItem(
                    vaccine = VaccineRecord("1", "Bordetella", "Dr. Smith · Happy Paws Clinic", "Sep 19, 2024", "Sep 19, 2025", null, VaccineFilterStatus.COMPLETED),
                    isLastItem = true,
                    onClick = {}
                )
            }
        }
    }
}
