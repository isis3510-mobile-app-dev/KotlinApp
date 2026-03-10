package com.example.petcare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.R
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.WarningYellow
import com.example.petcare.ui.theme.SuccessGreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale


enum class PetStatus {
    SUCCESS,
    WARNING,
    DEFAULT
}

@Composable
fun PetCard(
    image: Painter? = null,
    text: String = "Add pet",
    status: PetStatus = PetStatus.DEFAULT
) {
    val borderColor = if (status == PetStatus.DEFAULT)
        Color.Gray
    else
        MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.size(85.dp)) {

        Box(
            modifier = Modifier
                .size(65.dp)
                .align(Alignment.TopCenter)
        ) {

            OutlinedCard(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.size(61.33.dp)
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    if (status == PetStatus.DEFAULT) {

                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )

                    } else {

                        Image(
                            painter = image!!,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                    }
                }
            }

            if (status != PetStatus.DEFAULT) {

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = when (status) {
                            PetStatus.SUCCESS -> Icons.Default.CheckCircle
                            PetStatus.WARNING -> Icons.Default.Warning
                            else -> Icons.Default.Add
                        },
                        contentDescription = null,
                        tint = when (status) {
                            PetStatus.SUCCESS -> SuccessGreen
                            PetStatus.WARNING -> WarningYellow
                            else -> Color.Gray
                        },
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Text(
            text = text,
            modifier = Modifier
                .padding(2.dp)
                .align(Alignment.BottomCenter),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PetCardPreview() {
    PetCareTheme {

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            PetCard(
                image = painterResource(R.drawable.pet),
                text = "Max",
                status = PetStatus.SUCCESS
            )

            PetCard(
                image = painterResource(R.drawable.pet),
                text = "Luna",
                status = PetStatus.WARNING
            )

            PetCard()
        }
    }
}


