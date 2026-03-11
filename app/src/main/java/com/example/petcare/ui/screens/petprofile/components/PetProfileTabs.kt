package com.example.petcare.ui.screens.petprofile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme

enum class PetProfileTab(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.MonitorHeart),
    VACCINES("Vaccines", Icons.Default.Vaccines),
    EVENTS("Events", Icons.Default.History)
}

@Composable
fun PetProfileTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.White,
        contentColor = GreenDark,
        indicator = { tabPositions ->
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[selectedTabIndex])
                    .height(3.dp)
                    .background(GreenDark)
            )
        }
    ) {
        PetProfileTab.entries.forEachIndexed { index, tab ->
            val isSelected = selectedTabIndex == index
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) GreenDark else GrayText
                    )
                },
                text = {
                    Text(
                        text = tab.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) GreenDark else GrayText
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PetProfileTabsPreview() {
    PetCareTheme {
        PetProfileTabs(selectedTabIndex = 0, onTabSelected = {})
    }
}
