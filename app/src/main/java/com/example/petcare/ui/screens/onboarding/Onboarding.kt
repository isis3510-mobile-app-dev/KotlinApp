package com.example.petcare.ui.screens.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.R
import com.example.petcare.ui.components.ButtonOutline
import com.example.petcare.ui.components.PageIndicator
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.RobotoBold
import com.example.petcare.ui.theme.RobotoMedium
import com.example.petcare.ui.theme.RobotoRegular


@Composable
fun GradientBackground(backgroundStart: Color, backgroundEnd: Color) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        backgroundStart,
                        backgroundEnd
                    )
                )
            )
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = size.width * 0.4f,
                center = Offset(size.width * 0.9f, size.height * 0.1f)
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = size.width * 0.3f,
                center = Offset(size.width * 0.1f, size.height * 0.9f)
            )
        }
    }
}


@Composable
fun OnBoardingScreen(page: Int, title: String, description: String, backgroundStart: Color, backgroundEnd: Color, image: Int, onSignInClick: () -> Unit, onSkipClick: () -> Unit){
    val annotatedText = buildAnnotatedString {

        append("Already have an account? ")

        withLink(
            LinkAnnotation.Clickable(
                tag = "sign_in",
                linkInteractionListener = {
                    onSignInClick()
                },
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.None
                    )
                )
            )
        ) {
            withStyle(
                SpanStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("Sign in")
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        GradientBackground(backgroundStart, backgroundEnd)


        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
        ) {
            Text(
                text = "Skip",
                color = Color.White,
                modifier = Modifier
                    .padding(top = 40.dp, end = 24.dp)
                    .clickable { onSkipClick() },
                fontFamily = RobotoMedium,
                fontSize = 14.sp
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {

            Spacer(modifier = Modifier.height(120.dp))

            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape)
                )

                Image(
                    painter = painterResource(image),
                    contentDescription = null,
                    modifier = Modifier.width(153.dp).height(155.dp)
                )
            }


            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = title,
                fontSize = 26.sp,
                fontFamily = RobotoBold,
                lineHeight = 33.8.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp,
                fontFamily = RobotoRegular
            )

            Spacer(modifier = Modifier.height(40.dp))

            PageIndicator(page = page)

            Spacer(modifier = Modifier.height(30.dp))

            ButtonOutline(
                bgColor = Color.White.copy(alpha = 0.2f),
                outlineColor = Color.White.copy(alpha = 0.8f),
                textColor = Color.White,
                width = 326.dp,
                height = 58.dp,
                text = "Continue"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = annotatedText,
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
    
}


@Preview
@Composable
fun PreviewOnboarding(){
    PetCareTheme {

        OnBoardingScreen(
            page = 1,
            title = "All your pet's health, \n in one place",
            description = "Centralize vaccines, medications, records, and documents. Never miss a dose or appointment again.",
            backgroundStart = Color(0xFF006A60),
            backgroundEnd = Color(0xFF00897B),
            image = R.drawable.onboarding_dop,
            onSignInClick = {},
            onSkipClick = {}
        )


    }
}

@Preview
@Composable
fun PreviewOnboarding2(){
    PetCareTheme {

        OnBoardingScreen(
            page = 2,
            title = "Track vaccines & \n medications",
            description = "Timeline-based vaccine history, smart reminders for medications, and overdue alerts that keep you informed.",
            backgroundStart = Color(0xFF4B607A),
            backgroundEnd = Color(0xFF37505F),
            image = R.drawable.onboarding_vaccine,
            onSignInClick = {},
            onSkipClick = {}
        )

    }
}


@Preview
@Composable
fun PreviewOnboarding3(){
    PetCareTheme {

        OnBoardingScreen(
            page = 3,
            title = "NFC tag \n integration",
            description = "Write your pet's info to an NFC tag. Anyone who finds your pet can contact you instantly.",
            backgroundStart = Color(0xFF7B3DC4),
            backgroundEnd = Color(0xFF5E2A9D),
            image = R.drawable.onboarding_nfc,
            onSignInClick = {},
            onSkipClick = {}
        )

    }
}

