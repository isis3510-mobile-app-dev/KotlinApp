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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale

@Composable
fun PetCard() {
    PetCareTheme {
        Box(modifier = Modifier.size(85.dp)) {
            Box(modifier = Modifier
                .size(65.dp)
                .align(Alignment.TopCenter)
            ) {
                OutlinedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .size(width = 61.33.dp, height = 61.33.dp)

                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val image = painterResource(R.drawable.pet)
                        Image(
                            painter = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .shadow(4.dp, CircleShape)
                        //.offset(x = (-1).dp, y = (-1).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .align(Alignment.Center)
                            //.padding(1.dp)
                            .size(14.dp)

                    )
                }
            }
            Text(
                text = "Max",
                modifier = Modifier
                    .padding(2.dp)
                    .align(Alignment.BottomCenter),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }

    }
}

@Preview
@Composable
fun OutlinedCardExample() {
    PetCareTheme {
        Box(modifier = Modifier.size(85.dp)) {
            Box(modifier = Modifier
                .size(65.dp)
                .align(Alignment.TopCenter)
            ) {
                OutlinedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .size(width = 61.33.dp, height = 61.33.dp)

                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val image = painterResource(R.drawable.pet)
                        Image(
                            painter = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .shadow(4.dp, CircleShape)
                        //.offset(x = (-1).dp, y = (-1).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .align(Alignment.Center)
                            //.padding(1.dp)
                            .size(10.dp)

                    )
                }
            }
            Text(
                text = "Max",
                modifier = Modifier
                    .padding(2.dp)
                    .align(Alignment.BottomCenter),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }

}
}
