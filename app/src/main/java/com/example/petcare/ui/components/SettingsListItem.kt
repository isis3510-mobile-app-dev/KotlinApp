package com.example.petcare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GreenLight
import com.example.petcare.ui.theme.PetCareTheme

@Composable
fun SettingsListItem(
    icon: ImageVector,
    iconBackgroundColor: Color? = null,
    iconTintColor: Color = MaterialTheme.colorScheme.onSurface,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        val iconModifier = Modifier.size(40.dp)
        if (iconBackgroundColor != null) {
            Box(
                modifier = iconModifier
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTintColor,
                modifier = iconModifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = titleColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayText
                )
            }
        }

        // Trailing Content
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsListItemPreview() {
    PetCareTheme {
        Column {
            SettingsListItem(
                icon = Icons.Default.Person,
                iconBackgroundColor = GreenLight.copy(alpha = 0.2f),
                iconTintColor = MaterialTheme.colorScheme.secondary,
                title = "Edit Profile",
                subtitle = "Sarah Johnson",
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit",
                        tint = GrayText
                    )
                },
                onClick = {}
            )
            
            SettingsListItem(
                icon = Icons.Default.Person,
                iconBackgroundColor = Color.LightGray.copy(alpha = 0.2f),
                title = "Dark Mode",
                trailingContent = {
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        modifier = Modifier.semantics { contentDescription = "Toggle Dark Mode" }
                    )
                }
            )
        }
    }
}
