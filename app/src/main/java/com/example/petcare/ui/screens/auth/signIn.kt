package com.example.petcare.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.R
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.ButtonOutline
import com.example.petcare.ui.components.ButtonSwitch
import com.example.petcare.ui.components.TextFieldComponent
import com.example.petcare.ui.theme.PetCareTheme
import androidx.compose.foundation.layout.Spacer


@Composable
fun SignInScreen(){
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
            selectedIndex = 0,
            onSelectionChange = {}
        )


        Spacer(modifier = Modifier.height(24.dp))

        TextFieldComponent(
            name = "Email Address",
            label = "you@gmail.com"
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextFieldComponent(
            name = "Password",
            label = "........",
            icon = { Icon(
                imageVector = Icons.Default.RemoveRedEye,
                contentDescription = "Password",
                tint = Color.LightGray
            )
            }
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Forgot password?",
                modifier = Modifier.align(Alignment.CenterEnd),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        ButtonDefault(
            bgColor = MaterialTheme.colorScheme.secondary,
            textColor = Color.White,
            width = 342.dp,
            height = 56.dp,
            text = "Sign In"
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
            outlineColor = Color(0xFFBEC9C8),
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
fun SignInScreenPreview(){
    PetCareTheme() {
        SignInScreen()
    }
}