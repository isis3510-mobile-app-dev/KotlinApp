package com.example.petcare.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.Icon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.GrayBorder

@Composable
fun Stepper(currentStep: Int, totalSteps: Int = 3,
            stepLabels: List<String> = listOf("Step 1", "Step 2", "Step 3")) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)
    ) {
        for (step in 1..totalSteps) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = if (step <= currentStep) GreenDark else GrayBorder,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (step <= currentStep) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White
                        )
                    } else {
                        Text(
                            text = "$step",
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stepLabels.getOrNull(step - 1) ?: "",
                    fontSize = 10.sp,
                    color = if (step <= currentStep) GreenDark else GrayBorder
                )
            }


            if (step != totalSteps) {
                Canvas(modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .padding(horizontal = 6.dp, vertical = 12.dp)) {
                    val lineColor = if (step < currentStep) GreenDark else GrayBorder
                    val stroke = with(density) { 2.dp.toPx() }
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun StepperPreview() {
    PetCareTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Stepper(currentStep = 2)
        }
    }
}