package com.example.petcare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.RobotoMedium
import com.example.petcare.ui.theme.RobotoRegular

@Composable
fun IconCardButton(
    icon: @Composable () -> Unit,
    text: String,
    textBottom: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedCard(
            modifier = Modifier
                .size(112.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Text(
                    text = text,
                    fontSize = 12.sp,
                    fontFamily = RobotoMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )

            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = textBottom,
            fontSize = 12.sp,
            fontFamily = RobotoRegular,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun IconCardButtonPreview() {
    PetCareTheme {
        Column{
            IconCardButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Photo",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    )
                },
                text = "Add Photo",
                textBottom = "Tap to upload or take a photo",
                onClick = {
                    println("Card !")
                }
            )

            IconCardButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Upload",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    )
                },
                text = "Add Documents",
                textBottom = "Tap to upload documents",
                onClick = {
                    println("Card !")
                }
            )
        }
    }
}