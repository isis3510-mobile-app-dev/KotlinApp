package com.example.petcare.ui.screens.petprofile.components.vaccines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VaccineList(
    vaccines: List<VaccineRecord>,
    onFilterClick: (VaccineFilterStatus) -> Unit,
    onVaccineClick: (VaccineRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedCount = vaccines.count { it.status == VaccineFilterStatus.COMPLETED }
    val upcomingCount = vaccines.count { it.status == VaccineFilterStatus.UPCOMING }
    val overdueCount = vaccines.count { it.status == VaccineFilterStatus.OVERDUE }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
    ) {
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
}
