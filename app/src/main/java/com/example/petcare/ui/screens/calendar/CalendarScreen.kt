package com.example.petcare.ui.screens.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.ExpandableFAB
import com.example.petcare.ui.components.Filters
import com.example.petcare.ui.components.NavBar
import com.example.petcare.ui.components.CalendarWidget
import com.example.petcare.ui.components.EmptyStateView
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.OffWhite
import com.example.petcare.ui.theme.PetCareTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarScreen(
    currentRoute: String = "calendar",
    onNavigationClick: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All") }
    // Initialize date range with today
    var startDate by remember { mutableStateOf<LocalDate>(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate>(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    Scaffold(
        containerColor = OffWhite,
        bottomBar = {
            NavBar(
                currentRoute = currentRoute,
                onItemClick = onNavigationClick
            )
        },
        floatingActionButton = {
            ExpandableFAB()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Calendar",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            CalendarWidget(
                currentMonth = currentMonth,
                startDate = startDate,
                endDate = endDate,
                onDateSelected = { date ->
                    when {
                        startDate != endDate && (date == startDate || date == endDate) -> {
                            // Tapped an extreme: Reset to just that day
                            startDate = date
                            endDate = date
                        }
                        startDate == endDate -> {
                            if (date < startDate) {
                                startDate = date
                            } else if (date > endDate) {
                                endDate = date
                            }
                        }
                        else -> {
                            // We have an existing range (startDate != endDate)
                            if (date < startDate) {
                                startDate = date // expand to the left
                            } else if (date > endDate) {
                                endDate = date // expand to the right
                            } else if (date > startDate && date < endDate) {
                                // "If the user taps a day D > startDate, D < EndDate, then endDate = D"
                                endDate = date
                            }
                        }
                    }
                },
                onMonthChanged = { currentMonth = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Filters(
                filters = listOf("All", "Vaccines", "Appointments"),
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            val dateText = if (startDate == endDate) {
                startDate.format(DateTimeFormatter.ofPattern("dd MMMM")).uppercase()
            } else {
                val startFormat = startDate.format(DateTimeFormatter.ofPattern("dd MMM"))
                val endFormat = endDate.format(DateTimeFormatter.ofPattern("dd MMM"))
                "$startFormat - $endFormat".uppercase()
            }

            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF4A6863),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            EmptyStateView(
                icon = Icons.Default.CalendarMonth,
                message = "No events on this day",
                buttonText = "Add Event"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    PetCareTheme {
        CalendarScreen()
    }
}

