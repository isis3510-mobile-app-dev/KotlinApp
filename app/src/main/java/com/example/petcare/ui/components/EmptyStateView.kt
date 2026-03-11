package com.example.petcare.ui.components

import com.example.petcare.ui.theme.*

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.example.petcare.ui.theme.PetCareTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.petcare.ui.theme.GreenDark

@Composable
fun EmptyStateView(
    icon: ImageVector,
    message: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = message,
            tint = GreenTextDark,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            color = Color.DarkGray
        )
        if (buttonText != null) {
            Spacer(modifier = Modifier.height(24.dp))
            ButtonDefault(
                bgColor = GreenDark,
                textColor = Color.White,
                width = 160.dp,
                height = 48.dp,
                text = buttonText
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStateViewPreview() {
    PetCareTheme {
        EmptyStateView(
            icon = Icons.Default.CalendarMonth,
            message = "No events on this day",
            buttonText = "Add Event"
        )
    }
}
