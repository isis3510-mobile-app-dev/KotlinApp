package com.example.petcare.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.example.petcare.ui.theme.PetCareTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.petcare.ui.theme.GreenDark
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarWidget(
    currentMonth: YearMonth,
    startDate: LocalDate,
    endDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            CalendarHeader(
                currentMonth = currentMonth,
                onMonthChanged = onMonthChanged
            )

            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            val firstDayOfMonth = currentMonth.atDay(1)
            val lastDayOfMonth = currentMonth.atEndOfMonth()
            
            // DayOfWeek enum: Monday = 1, Sunday = 7
            // We want Sunday to be index 0
            val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

            val daysInMonth = lastDayOfMonth.dayOfMonth
            val totalDays = startDayOfWeek + daysInMonth
            val totalWeeks = ceil(totalDays / 7.0).toInt()

            var currentDay = 1
            for (week in 0 until totalWeeks) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayOfWeek in 0..6) {
                        val isCurrentMonthDay = (week == 0 && dayOfWeek >= startDayOfWeek) || (week > 0 && currentDay <= daysInMonth)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrentMonthDay) {
                                val date = currentMonth.atDay(currentDay)
                                CalendarDay(
                                    date = date,
                                    startDate = startDate,
                                    endDate = endDate,
                                    onDateSelected = onDateSelected
                                )
                                currentDay++
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onMonthChanged: (YearMonth) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChanged(currentMonth.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = Color.Black)
        }
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleLarge,
            color = Color.Black
        )
        IconButton(onClick = { onMonthChanged(currentMonth.plusMonths(1)) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.Black)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarDay(
    date: LocalDate,
    startDate: LocalDate,
    endDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val isStart = date == startDate
    val isEnd = date == endDate
    val isInRange = date.isAfter(startDate) && date.isBefore(endDate)
    val isSingleSelection = startDate == endDate

    // Container for highlights spanning across cells
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when {
                    isSingleSelection -> Color.Transparent
                    isInRange -> Color(0xFFB2EBE0) // Light green for middle
                    isStart -> Color(0xFFB2EBE0).copy(alpha = 0f) // We'll handle this in the Box below to not overdraw if we want rounded edges
                    isEnd -> Color(0xFFB2EBE0).copy(alpha = 0f)
                    else -> Color.Transparent
                }
            )
    ) {
        // For the start node, add a partial background extending to the right if not the end
        if (isStart && !isSingleSelection) {
            Box(modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterEnd)
                .background(Color(0xFFB2EBE0)))
        }
        // For the end node, add a partial background extending to the left if not the start
        if (isEnd && !isSingleSelection) {
            Box(modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterStart)
                .background(Color(0xFFB2EBE0)))
        }
    }

    // The actual clickable circle
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (isStart || isEnd) GreenDark else Color.Transparent)
            .clickable { onDateSelected(date) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isStart || isEnd) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isStart || isEnd) Color.White else Color.Black
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun CalendarWidgetPreview() {
    PetCareTheme {
        var currentMonth by remember { mutableStateOf(YearMonth.now()) }
        var selectedDate by remember { mutableStateOf(LocalDate.now()) }
        
        CalendarWidget(
            currentMonth = currentMonth,
            startDate = selectedDate,
            endDate = selectedDate,
            onDateSelected = { selectedDate = it },
            onMonthChanged = { currentMonth = it }
        )
    }
}
