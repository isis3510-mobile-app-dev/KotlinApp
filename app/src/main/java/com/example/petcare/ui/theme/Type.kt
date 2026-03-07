package com.example.petcare.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.petcare.R
import androidx.compose.ui.text.font.Font

// Set of Material typography styles to start with
val RobotoBold = FontFamily(
    Font(R.font.roboto_bold)
)

val RobotoMedium = FontFamily(
    Font(R.font.roboto_medium)
)

val RobotoRegular = FontFamily(
    Font(R.font.roboto_regular)
)
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = RobotoMedium,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),

    titleLarge = TextStyle(
        fontFamily = RobotoBold,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 33.8.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

)