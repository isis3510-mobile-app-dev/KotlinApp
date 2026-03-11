package com.example.petcare.ui.screens.petprofile.components.overview

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme

import androidx.compose.material.icons.outlined.Contactless
import com.example.petcare.ui.theme.OffWhite

@Composable
fun QuickActionGrid(
    onAddEventClick: () -> Unit,
    onAddVaccineClick: () -> Unit,
    onLostModeClick: () -> Unit,
    onNfcClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GridButton(
                vectorIcon = Icons.Default.Add,
                label = "Add Event",
                onClick = onAddEventClick,
                modifier = Modifier.weight(1f)
            )
            GridButton(
                vectorIcon = Icons.Default.AddModerator,
                label = "Add Vaccine",
                onClick = onAddVaccineClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GridButton(
                vectorIcon = Icons.Default.LocationOn,
                label = "Lost Mode",
                onClick = onLostModeClick,
                modifier = Modifier.weight(1f)
            )
            GridButton(
                vectorIcon = Icons.Outlined.Contactless,
                label = "NFC Active",
                onClick = onNfcClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GridButton(
    painter: Painter? = null,
    vectorIcon: ImageVector? = null,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = label,
                    tint = GreenDark,
                    modifier = Modifier.size(20.dp)
                )
            } else if (vectorIcon != null) {
                Icon(
                    imageVector = vectorIcon,
                    contentDescription = label,
                    tint = GreenDark,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = GreenDark,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun QuickActionGridPreview() {
    PetCareTheme {
        Box(modifier = Modifier.padding(16.dp).background(OffWhite)) {
            QuickActionGrid(
                onAddEventClick = {},
                onAddVaccineClick = {},
                onLostModeClick = {},
                onNfcClick = {}
            )
        }
    }
}
