package com.example.petcare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.petcare.data.model.SuggestionDto
import com.example.petcare.ui.theme.ErrorContainer
import com.example.petcare.ui.theme.ErrorContent


@Composable
private fun SmartBannerCard(
    icon: ImageVector,
    background: Color,
    iconColor: Color,
    title: String,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector    = icon,
                contentDescription = null,
                tint           = iconColor,
                modifier       = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text       = title,
                    fontWeight = FontWeight.Bold,
                    color      = iconColor,
                    fontSize   = 14.sp
                )
                Text(
                    text     = message,
                    color    = iconColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun OverdueWarningBanner(overdueCount: Int) {
    SmartBannerCard(
        icon       = Icons.Default.Warning,
        background = ErrorContainer,
        iconColor  = ErrorContent,
        title      = "$overdueCount Overdue Vaccines",
        message    = "Schedule appointments immediately to keep your pets protected"
    )
}


@Composable
fun SuggestionBanner(
    suggestions: List<SuggestionDto>,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            val (background, iconColor, icon) = when (suggestion.type) {
                "danger"  -> Triple(ErrorContainer, ErrorContent, Icons.Default.Warning)
                "warning" -> Triple(Color(0xFFFFF3CD), Color(0xFFB45309), Icons.Default.Info)
                else      -> Triple(Color(0xFFE8F4FD), Color(0xFF1565C0), Icons.Default.Notifications)
            }
            SmartBannerCard(
                icon       = icon,
                background = background,
                iconColor  = iconColor,
                title      = suggestion.title,
                message    = suggestion.message
            )
        }
    }
}


@Preview(showBackground = false)
@Composable
fun PreviewOverdueWarningBanner() {
    OverdueWarningBanner(overdueCount = 4)
}

@Preview(showBackground = true)
@Composable
fun PreviewSuggestionBanner() {
    SuggestionBanner(
        suggestions = listOf(
            SuggestionDto("danger",  "Vaccine 'Rabies' overdue",    "Overdue by 45 days. Schedule an appointment soon."),
            SuggestionDto("warning", "Vaccine 'Parvovirus' expiring", "Expires in 10 days. Time to catch up."),
            SuggestionDto("info",    "Missing vaccine: 'Distemper'", "Never been recorded for your pet.")
        )
    )
}