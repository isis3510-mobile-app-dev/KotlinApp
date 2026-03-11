package com.example.petcare.ui.screens.nfc.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.GreenLight
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.SuccessGreen

@Composable
fun NFCScanLoadingRing() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(GreenLight.copy(alpha = 0.2f))
        )
        // Middle ring
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(GreenLight.copy(alpha = 0.4f))
        )
        // Inner circle
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(GreenLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Contactless,
                contentDescription = "Scanning",
                tint = GreenDark,
                modifier = Modifier.size(70.dp)
            )
        }
    }
}

@Composable
fun NFCCheckMark() {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(SuccessContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Success",
            tint = SuccessGreen,
            modifier = Modifier.size(50.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NFCScanLoadingRingPreview() {
    PetCareTheme {
        NFCScanLoadingRing()
    }
}

@Preview(showBackground = true)
@Composable
fun NFCCheckMarkPreview() {
    PetCareTheme {
        NFCCheckMark()
    }
}
