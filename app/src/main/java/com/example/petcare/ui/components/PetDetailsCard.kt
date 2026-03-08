package com.example.petcare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.outlined.Contactless
import com.example.petcare.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun PetAction(
    text: String,
    icon: ImageVector,
    color: Long
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(12.dp)
    ) {

        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(18.dp),
            tint = Color(color)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(color)
        )
    }
}
@Composable
fun PetDetailsCard(petName: String, breed:String, age: Int, weight: Double, gender: String, status: String, photoPath: Int, species: String){
    val (statusColor, textStatusColor) = when (status.lowercase()) {
        "healthy" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))   // verde
        else -> Pair(Color(0xFFFFF8E1), Color(0xFFE65100))        // amarillo
    }

    val logo = when (species.lowercase()){
        "dog" -> R.drawable.dog_logo
        else -> R.drawable.cat_logo
    }

    val genderIcon = when (gender.lowercase()) {
        "Male" -> Icons.Default.Male
        "Female" -> Icons.Default.Female
        else -> Icons.Default.Male
    }

    Card(modifier = Modifier.fillMaxWidth().size(width = 350.dp, height = 153.33.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth().size(width = 350.dp, height = 153.33.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(80.dp).background(color = Color.Gray, shape= RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id= photoPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier.size(width = 220.dp, height = 71.79.dp)
                ) {
                    Column {
                        Row {
                            Text(
                                text = petName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(50.dp))
                                    .background(statusColor).padding(horizontal = 10.dp, vertical = 4.dp),

                                ) {
                                Row {
                                    Text(
                                        text = status,
                                        fontSize = 12.sp,
                                        color = textStatusColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                            }
                        }
                        Row {
                            Image(
                                painter = painterResource(id = logo),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = breed,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        Row(
                            modifier = Modifier.padding(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = "Detail",
                                tint = Color(0xFFE63600),
                                modifier = Modifier.padding(end = 5.dp).size(14.dp)
                            )
                            Text(
                                text = "$age",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Balance,
                                contentDescription = "Detail",
                                tint = Color.Black,
                                modifier = Modifier.padding(end = 5.dp).size(14.dp)
                            )
                            Text(
                                text = "$weight",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = genderIcon,
                                contentDescription = "Detail",
                                tint = Color.Black,
                                modifier = Modifier.padding(end = 5.dp).size(14.dp)
                            )
                            Text(
                                text = gender,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }

                    }
                }
                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Detail",
                    tint = Color.Black,
                    modifier = Modifier.padding(end = 16.dp)
                )

            }
            HorizontalDivider(
                color = Color(0xFFF0F4F3),
                thickness = 3.dp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                PetAction("Vaccines", Icons.Default.Vaccines, 0xFF006A60)
                VerticalDivider(
                    color = Color(0xFFF0F4F3),
                    thickness = 3.dp
                )
                PetAction("Lost Mode", Icons.Default.LocationOn, 0xFF3F4948)
                VerticalDivider(
                    color = Color(0xFFF0F4F3),
                    thickness = 3.dp
                )
                PetAction("NFC", Icons.Outlined.Contactless, 0xFF3949AB)
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
        photoPath = R.drawable.pet,
        species = "DOG")
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
        photoPath = R.drawable.pet,
        species = "CAT")
}