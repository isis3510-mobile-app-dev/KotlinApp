package com.example.petcare.ui.screens.petprofile.components.vaccines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.petcare.ui.theme.InfoContainer
import com.example.petcare.ui.theme.InfoContent
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.SuccessContent

enum class VaccineFilterStatus {
    COMPLETED, UPCOMING, OVERDUE
}

@Composable
fun VaccineStatusChips(
    completedCount: Int,
    upcomingCount: Int,
    overdueCount: Int,
    onFilterClick: (VaccineFilterStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (completedCount > 0) {
            item {
                StatusChip(
                    label = "$completedCount Completed",
                    backgroundColor = SuccessContainer,
                    contentColor = SuccessContent,
                    onClick = { onFilterClick(VaccineFilterStatus.COMPLETED) }
                )
            }
        }
        if (upcomingCount > 0) {
            item {
                StatusChip(
                    label = "$upcomingCount Upcoming",
                    backgroundColor = InfoContainer,
                    contentColor = InfoContent,
                    onClick = { onFilterClick(VaccineFilterStatus.UPCOMING) }
                )
            }
        }
        if (overdueCount > 0) {
            item {
                StatusChip(
                    label = "$overdueCount Overdue",
                    backgroundColor = ErrorContainer,
                    contentColor = ErrorContent,
                    onClick = { onFilterClick(VaccineFilterStatus.OVERDUE) }
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VaccineStatusChipsPreview() {
    PetCareTheme {
        VaccineStatusChips(
            completedCount = 2,
            upcomingCount = 1,
            overdueCount = 1,
            onFilterClick = {}
        )
    }
}
