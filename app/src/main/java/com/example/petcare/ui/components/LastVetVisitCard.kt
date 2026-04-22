package com.example.petcare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.petcare.ui.theme.*

@Composable
fun LastVetVisitCard(
    daysSince: Int,
    petName: String,
    lastDate: String
) {
    val (bgColor, textColor) = when {
        daysSince > 180 -> Pair(ErrorContainer, ErrorContent)
        daysSince > 90  -> Pair(WarningContainer, WarningContent)
        else            -> Pair(SuccessContainer, SuccessContent)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint               = textColor,
                    modifier           = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text       = "Last Vet Visit",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text     = "$petName · $lastDate",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text       = "${daysSince}d ago",
                fontSize   = 11.sp,
                color      = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LastVetVisitCardPreview_Healthy() {
    LastVetVisitCard(
        daysSince = 30,
        petName = "Max",
        lastDate = "2026-02-01"
    )
}

@Preview(showBackground = true)
@Composable
fun LastVetVisitCardPreview_Warning() {
    LastVetVisitCard(
        daysSince = 120,
        petName = "Luna",
        lastDate = "2025-12-15"
    )
}

@Preview(showBackground = true)
@Composable
fun LastVetVisitCardPreview_Overdue() {
    LastVetVisitCard(
        daysSince = 200,
        petName = "Buddy",
        lastDate = "2025-10-14"
    )
}