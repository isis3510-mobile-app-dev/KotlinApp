package com.example.petcare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.GreenLight
import com.example.petcare.ui.theme.White

@Composable
fun CircularIconButton(
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    hasNotificationDot: Boolean = false,
    useShadow: Boolean = true
) {
    Box(modifier = Modifier.size(52.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (useShadow) Modifier.shadow(4.dp, CircleShape) else Modifier)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        if (hasNotificationDot) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935)) // Red dot
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
            )
        }
    }
}

@Composable
fun NfcButton(onClick: () -> Unit = {}) {
    CircularIconButton(
        icon = Icons.Outlined.Contactless,
        contentDescription = "NFC",
        backgroundColor = GreenLight,
        iconColor = GreenDark,
        onClick = onClick,
        useShadow = false
    )
}

@Composable
fun NotificationButton(hasNotifications: Boolean = true, onClick: () -> Unit = {}) {
    CircularIconButton(
        icon = Icons.Outlined.Notifications,
        contentDescription = "Notifications",
        backgroundColor = White,
        iconColor = GreenDark,
        onClick = onClick,
        hasNotificationDot = hasNotifications
    )
}

@Preview(showBackground = true)
@Composable
fun HomeActionButtonsPreview() {
    Row(modifier = Modifier.padding(16.dp).background(Color(0xFFF6FCFB))) {
        NfcButton()
        Spacer(modifier = Modifier.width(8.dp))
        NotificationButton()
    }
}
