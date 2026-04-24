package com.example.petcare.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.outlined.Contactless
import com.example.petcare.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.SuccessContent
import com.example.petcare.ui.theme.WarningContainer
import com.example.petcare.ui.theme.WarningContent
import com.example.petcare.ui.theme.ErrorContainer
import com.example.petcare.ui.theme.ErrorContent
import androidx.compose.ui.platform.LocalContext
import com.example.petcare.util.DisplayTextLimits
import com.example.petcare.util.truncateForDisplay



@Composable
fun PetAction(
    text: String,
    icon: ImageVector,
    color: Color
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(12.dp)
    ) {

        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(18.dp),
            tint = color
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            fontSize = 12.sp,
            color = color
        )
    }
}
@Composable
fun PetDetailsCard(
    petName: String, breed: String, age: Int, weight: Double,
    gender: String, status: String, photoUrl: String?, species: String,
    onPetSelect: () -> Unit,
    onVaccineSelect: () -> Unit,
    onLostSelect: () -> Unit,
    onNFCSelect: () -> Unit
) {
    val context = LocalContext.current
    val (statusColor, textStatusColor) = when (status.lowercase()) {
        "healthy" -> Pair(SuccessContainer, SuccessContent)
        "lost" -> Pair(ErrorContainer, ErrorContent)
        else -> Pair(WarningContainer, WarningContent)
    }
    val isLost = status.equals("lost", ignoreCase = true)

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val logo = when (species.lowercase()) {
        "dog" -> if (isDark) R.drawable.dog_logo_white else R.drawable.dog_logo
        else -> if (isDark) R.drawable.cat_logo_white else R.drawable.cat_logo
    }

    val genderIcon = when (gender.lowercase()) {
        "male" -> Icons.Default.Male
        "female" -> Icons.Default.Female
        else -> Icons.Default.Male
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPetSelect() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                when {
                    !photoUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(Uri.parse(photoUrl))
                                .crossfade(true)
                                .build(),
                            imageLoader = coil.Coil.imageLoader(context),
                            contentDescription = petName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            error = painterResource(R.drawable.pet)
                        )
                    }
                    else -> {
                        Image(
                            painter = painterResource(R.drawable.pet),
                            contentDescription = "Pet Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = petName.truncateForDisplay(DisplayTextLimits.COMPACT_TITLE),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(statusColor)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = status,
                                fontSize = 12.sp,
                                color = textStatusColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = logo),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = breed.truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.size(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cake, contentDescription = null, tint = ErrorContent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$age", fontSize = 14.sp, color = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.Balance, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$weight", fontSize = 14.sp, color = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(genderIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = gender, fontSize = 14.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Detail",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier.weight(1f).clickable { onLostSelect() },
                    contentAlignment = Alignment.Center
                ) {
                    PetAction(
                        text = if (isLost) "Report as Found" else "Report as Lost",
                        icon = if (isLost) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                VerticalDivider(color = MaterialTheme.colorScheme.onPrimary, thickness = 1.dp)
                Box(
                    modifier = Modifier.weight(1f).clickable { onNFCSelect() },
                    contentAlignment = Alignment.Center
                ) {
                    PetAction("NFC", Icons.Outlined.Contactless, MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun PetDetailsCardPreview () {
    PetDetailsCard(petName ="Max",
        breed = "Golden Retriever",
        age = 6,
        weight = 20.0,
        gender = "Male",
        status = "Healthy",
        photoUrl = null,
        species = "DOG",
        onPetSelect = {},
        onVaccineSelect = {},
        onLostSelect = {},
        onNFCSelect = {}
    )
}

@Preview(showBackground = false)
@Composable
fun PetDetailsCardPreview2 () {
    PetDetailsCard(petName ="Max",
        breed = "Golden Retriever",
        age = 6,
        weight = 20.0,
        gender = "Male",
        status = "Vaccine due",
        photoUrl = null,
        species = "CAT",
        onPetSelect = {},
        onVaccineSelect = {},
        onLostSelect = {},
        onNFCSelect = {}
    )
}
