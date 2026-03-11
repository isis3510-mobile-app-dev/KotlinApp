package com.example.petcare.ui.screens.nfc.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.R
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.ButtonOutline
import com.example.petcare.ui.theme.GrayBorder
import com.example.petcare.ui.theme.GrayDark
import com.example.petcare.ui.theme.GreenLight
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.InfoContainer
import com.example.petcare.ui.theme.InfoContent
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.SuccessGreen
import com.example.petcare.ui.theme.WarningContainer
import com.example.petcare.ui.theme.WarningContent
import com.example.petcare.ui.theme.WarningYellow

@Composable
fun NFCPetCard(
    petName: String,
    breedAndSpecies: String,
    photoPath: Int,
    ownerName: String,
    ownerPhone: String,
    ownerInitials: String,
    medicalNotes: String,
    isHealthy: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Top Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = painterResource(id = photoPath),
                    contentDescription = petName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )

                // Dark Gradient overlay for text readability (simulated with semi-transparent box at bottom)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                // Status Badge
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isHealthy) SuccessContainer else WarningContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isHealthy) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = "Status",
                        tint = if (isHealthy) SuccessGreen else WarningYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHealthy) "Healthy" else "Attention",
                        color = if (isHealthy) SuccessGreen else WarningYellow,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Pet Info Text
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(
                        text = petName,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = breedAndSpecies,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Bottom Content Section
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "OWNER INFORMATION",
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayDark,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Owner Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ownerInitials,
                            color = GreenDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = ownerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ownerPhone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrayDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                ButtonDefault(
                    bgColor = GreenDark,
                    textColor = Color.White,
                    width = 342.dp,
                    height = 56.dp,
                    text = "Call Owner Now",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call",
                            tint = Color.White
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ButtonDefault(
                            bgColor = InfoContainer,
                            textColor = InfoContent,
                            width = 200.dp,
                            height = 60.dp,
                            text = "Send SMS",
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "SMS",
                                    tint = InfoContent
                                )
                            }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ButtonDefault(
                            bgColor = WarningContainer,
                            textColor = WarningContent,
                            width = 200.dp,
                            height = 60.dp,
                            text = "Share",
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Share",
                                    tint = WarningContent
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Medical Notes
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WarningContainer),
                    border = BorderStroke(1.dp, WarningYellow.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = WarningContent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Medical Notes",
                                color = WarningContent,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = medicalNotes,
                            color = WarningContent,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NFCPetCardPreview() {
    PetCareTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NFCPetCard(
                petName = "Max",
                breedAndSpecies = "Golden Retriever · Dog",
                photoPath = R.drawable.pet,
                ownerName = "Sarah Johnson",
                ownerPhone = "+1 (555) 012-3456",
                ownerInitials = "SJ",
                medicalNotes = "No known allergies."
            )
        }
    }
}
