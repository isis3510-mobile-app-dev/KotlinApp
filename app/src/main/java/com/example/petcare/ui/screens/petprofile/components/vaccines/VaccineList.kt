package com.example.petcare.ui.screens.petprofile.components.vaccines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun LazyListScope.vaccineTabContent(
    vaccines: List<VaccineRecord>,
    onFilterClick: (VaccineFilterStatus) -> Unit,
    onVaccineClick: (VaccineRecord) -> Unit
) {
    val completedCount = vaccines.count { it.status == VaccineFilterStatus.COMPLETED }
    val upcomingCount = vaccines.count { it.status == VaccineFilterStatus.UPCOMING }
    val overdueCount = vaccines.count { it.status == VaccineFilterStatus.OVERDUE }

    item {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            VaccineStatusChips(
                completedCount = completedCount,
                upcomingCount = upcomingCount,
                overdueCount = overdueCount,
                onFilterClick = onFilterClick
            )
        }
    }
    
    itemsIndexed(vaccines) { index, vaccine ->
        VaccineTimelineItem(
            vaccine = vaccine,
            isLastItem = index == vaccines.lastIndex,
            onClick = { onVaccineClick(vaccine) },
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
