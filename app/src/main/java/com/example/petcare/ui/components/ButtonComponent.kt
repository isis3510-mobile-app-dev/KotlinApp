package com.example.petcare.ui.components

import com.example.petcare.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.petcare.ui.theme.PetCareTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.shadow
import com.example.petcare.ui.theme.GrayMedium
import com.example.petcare.ui.theme.GreenTextDark


@Composable
fun ButtonDefault(
    bgColor: Color,
    textColor: Color,
    width: Dp,
    height: Dp, text: String,
    icon: (@Composable () -> Unit)? = null
) {
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = bgColor),
            modifier = Modifier.size(width, height)
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(text,
                style = MaterialTheme.typography.labelMedium,
                color = textColor)
        }
}

@Composable
fun ButtonOutline(
    bgColor: Color,
    outlineColor: Color,
    textColor: Color,
    width: Dp,
    height: Dp, text: String,
    icon: (@Composable () -> Unit)? = null,
    iconSize: Dp = 40.dp
) {
    OutlinedButton(
        onClick = { },
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        modifier = Modifier.size(width, height),
        border = BorderStroke(1.dp, outlineColor)
    ) {
        if (icon != null) {
            Box(modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center) {
                icon()
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}


@Composable
fun ButtonSwitch(
    option1: String,
    option2: String,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit
) {

    val options = listOf(option1, option2)

    Box(
        modifier = Modifier
            .background(GrayMedium, RoundedCornerShape(40.dp))
            .padding(4.dp).size( width= 342.dp, height= 48.dp)
    ) {

        Row {
            options.forEachIndexed { index, text ->

                val isSelected = index == selectedIndex

                Box(
                    modifier = Modifier
                        .then(
                            if (isSelected)
                                Modifier.shadow(6.dp, RoundedCornerShape(40.dp))
                            else Modifier
                        )
                        .clip(RoundedCornerShape(40.dp))
                        .background(
                            if (isSelected) Color.White else Color.Transparent
                        )
                        .clickable { onSelectionChange(index) }
                        .padding(vertical = 14.dp)
                        .size(width=167.dp, height= 40.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = text,
                        color = if (isSelected)
                            GreenTextDark
                        else
                            Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ButtonPreview() {
    PetCareTheme {
        ButtonDefault(
            bgColor = MaterialTheme.colorScheme.secondary,
            textColor = Color.White,
            width = 342.dp,
            height = 56.dp,
            text = "Create Account"
        )
    }
}

@Preview
@Composable
fun IconButtonPreview() {
    PetCareTheme {
        ButtonDefault(
            bgColor = MaterialTheme.colorScheme.secondary,
            textColor = Color.White,
            width = 342.dp,
            height = 56.dp,
            text = "Call Owner Now",
            icon = { Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call Owner Now",
                    tint = Color(0xFFFFFFFF)
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OutlinedButtonIconPreview() {
    PetCareTheme {
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

@Preview(showBackground = true)
@Composable
fun OutlinedButtonPreview() {
    PetCareTheme {
        ButtonOutline(
            bgColor = MaterialTheme.colorScheme.background,
            outlineColor = MaterialTheme.colorScheme.secondary,
            textColor = MaterialTheme.colorScheme.secondary,
            width = 169.dp,
            height = 50.57.dp,
            text = "Back"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SegmentedAuthSwitchPreview() {

    val selected = 0

    ButtonSwitch(
        option1 = "Sign In",
        option2 = "Create Account",
        selectedIndex = selected,
        onSelectionChange = { }
    )
}