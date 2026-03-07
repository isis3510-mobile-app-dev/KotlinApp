package com.example.petcare.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.GreenLight

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun NavBar(
    currentRoute: String,
    onItemClick: (String) -> Unit
    ) {
    val items = listOf(
        BottomNavItem("home", "Home", Icons.Default.Home),
        BottomNavItem("pets", "Pets", Icons.Default.Pets),
        BottomNavItem("records", "Records", Icons.AutoMirrored.Outlined.Article),
        BottomNavItem("calendar", "Calendar", Icons.Default.CalendarMonth),
        BottomNavItem("profile", "Profile", Icons.Default.Person)
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onItemClick(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GreenDark,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = GreenDark,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = GreenLight
                )

            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NavBarPreview(){
    NavBar(
        "home",
        onItemClick = {}
    )
}