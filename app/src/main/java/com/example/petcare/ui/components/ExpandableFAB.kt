package com.example.petcare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.theme.GreenLight

@Composable
fun FABMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(GreenLight)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ){
            Text(label, fontSize = 14.sp)
        }

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = Color(0xFFB2EBE0),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF00695C)
            )
        }
    }
}

@Composable
fun ExpandableFAB(){
    var expanded by remember { mutableStateOf(false) }

    val fabItems = listOf(
        Triple("Add Pet", Icons.Default.Pets) {},
        Triple("Add Vaccine", Icons.Default.Vaccines) {},
        Triple("Add Event", Icons.Default.CalendarMonth) {},
        Triple("Scan NFC", Icons.Outlined.Contactless) {}
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ){
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            if (expanded){
                fabItems.forEach { (label, icon, onClick) ->
                    FABMenuItem(label=label, icon=icon, onClick=onClick)
                }
            }

            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = Color(0xFF00695C),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Expand",
                    tint = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun ExpandableFabPreview(){
    ExpandableFAB()
}

@Preview(showBackground = false)
@Composable
fun ExpandableFABExpandedPreview() {
    // expanded state forced to true for preview purposes
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            FABMenuItem("Add Pet", Icons.Default.Pets) {}
            FABMenuItem("Add Vaccine", Icons.Default.Vaccines) {}
            FABMenuItem("Add Event", Icons.Default.CalendarMonth) {}

            FloatingActionButton(
                onClick = {},
                containerColor = Color(0xFF00695C),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
            }
        }
    }
}
