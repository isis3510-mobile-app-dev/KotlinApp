package com.example.petcare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.ui.theme.GreenDark

@Composable
fun Filters(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                shape = RoundedCornerShape(50.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GreenDark,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = Color.Gray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == filter,
                    borderColor = Color.LightGray,
                    selectedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun FiltersPreview() {
    Filters(
        filters = listOf("All Pets", "Healthy", "Vaccine Due", "Lost"),
        selectedFilter = "All Pets",
        onFilterSelected = {}
    )
}