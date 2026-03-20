package com.example.petcare.ui.screens.petprofile.components.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.petcare.data.model.Event
import com.example.petcare.ui.theme.GreenDark

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.MaterialTheme
import com.example.petcare.ui.components.EmptyStateView

fun LazyListScope.eventTabContent(
    events: List<Event>,
    onEventClick: (String) -> Unit,
    onAddEventClick: () -> Unit
) {
    if (events.isEmpty()) {
        item { 
            Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                EmptyStateView(
                    icon = Icons.Default.CalendarMonth,
                    message = "No medical events yet",
                    buttonText = "Add Medical Event",
                    onButtonClick = onAddEventClick
                )
            }
        }
        return
    }

    items(events) { event ->
        EventListItem(
            event = event,
            onClick = onEventClick,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onAddEventClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 80.dp) // padding for FAB
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary,
                containerColor = Color.Transparent
            )
        ) {
            Text(
                text = "+ Add Medical Event",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
