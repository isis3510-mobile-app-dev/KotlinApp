package com.example.petcare.ui.screens.auth

import com.example.petcare.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.R
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.ButtonOutline
import com.example.petcare.ui.components.ButtonSwitch
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.theme.PetCareTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.theme.RobotoMedium
import androidx.compose.foundation.layout.Spacer


@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Column (
        horizontalAlignment = Alignment.CenterHorizontally
    ){

        Card(
            modifier = Modifier
                .width(64.dp)
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(45.dp).rotate(45f)
                )

            }
        }
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(5.dp)
        ){

            Text(
                text = title,
                color = MaterialTheme.colorScheme.tertiary,
                fontFamily = RobotoMedium,
                fontSize = 26.sp
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
@Composable
fun LoginScreen(){
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)
        .fillMaxSize()
        .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            FeatureCard(
                title = "PetCare",
                subtitle = "Your pet's health companion",
                icon = Icons.Outlined.Pets
            )
        }
        Spacer(modifier = Modifier.height(30.dp))

        ButtonSwitch(
            option1 = "Sign In",
            option2 = "Create account",
            selectedIndex = 1,
            onSelectionChange = {}
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextFieldComponent(
            name = "Full Name",
            label = "Sarah Johnson"
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextFieldComponent(
            name = "Email Address",
            label = "you@gmail.com"
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextFieldComponent(
            name = "Password",
            label = "Min. 8 characters",
            icon = { Icon(
                imageVector = Icons.Default.RemoveRedEye,
                contentDescription = "Password",
                tint = Color.LightGray
            )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        ButtonDefault(
            bgColor = MaterialTheme.colorScheme.secondary,
            textColor = Color.White,
            width = 342.dp,
            height = 56.dp,
            text = "Create Account"
        )

        Spacer(modifier = Modifier.height(24.dp))


        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp), thickness = DividerDefaults.Thickness, color = Color.LightGray
            )

            Text(
                text = "or continue with",
                modifier = Modifier.padding(horizontal = 12.dp),
                color = Color.Gray
            )

            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp), thickness = DividerDefaults.Thickness, color = Color.LightGray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        ButtonOutline(
            bgColor = MaterialTheme.colorScheme.background,
            outlineColor = GrayBorder,
            textColor = MaterialTheme.colorScheme.tertiary,
            width = 342.dp,
            height = 56.dp,
            text = "Continue with Google",
            icon = { Icon(
                painter = painterResource(id = R.drawable.google),
                contentDescription = "Call Owner Now",
                tint = Color.Unspecified
            )
            }
        )
    }
}

@Preview
@Composable
fun LoginScreenPreview(){
    PetCareTheme() {
    LoginScreen()
    }
}