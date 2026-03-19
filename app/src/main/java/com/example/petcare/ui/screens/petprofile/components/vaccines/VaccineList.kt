package com.example.petcare.ui.screens.petprofile.components.vaccines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vaccines
import com.example.petcare.ui.components.EmptyStateView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

fun LazyListScope.vaccineTabContent(
    vaccines: List<VaccineRecord>,
    onFilterClick: (VaccineFilterStatus) -> Unit,
    onVaccineClick: (VaccineRecord) -> Unit,
    onAddVaccineClick: () -> Unit
) {
    if (vaccines.isEmpty()) {
        item { 
            Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                EmptyStateView(
                    icon = Icons.Default.Vaccines,
                    message = "No vaccines recorded yet",
                    buttonText = "Add Vaccine",
                    onButtonClick = onAddVaccineClick
                )
            }
        }
        return
    }

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
