package com.example.petcare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun PageIndicator(page: Int) {
    var size1 = 28.dp
    var a1 = 1f
    var size2 = 8.dp
    var a2 = 0.5f
    var size3 = 8.dp
    var a3 = 0.5f

    if (page == 2){
        size1 = 8.dp
        a1 = 0.5f
        size2 = 28.dp
        a2 = 1f
        size3 = 8.dp
        a3 = 0.5f
    } else if (page ==3) {
        size1 = 8.dp
        a1 = 0.5f
        size2 = 8.dp
        a2 = 0.5f
        size3 = 28.dp
        a3 = 1f
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Box(
            modifier = Modifier
                .width(size1)
                .height(8.dp)
                .background(Color.White.copy(alpha = a1), RoundedCornerShape(10.dp))
        )

        Box(
            modifier = Modifier
                .width(size2)
                .height(8.dp)
                .background(Color.White.copy(alpha = a2), CircleShape)
        )

        Box(
            modifier = Modifier
                .width(size3)
                .height(8.dp)
                .background(Color.White.copy(alpha = a3), CircleShape)
        )
    }
}


@Preview
@Composable
fun PreviewPageIndicator(){
    Column {
        PageIndicator(page = 1)
        PageIndicator(page = 2)
        PageIndicator(page = 3)

    }
}

