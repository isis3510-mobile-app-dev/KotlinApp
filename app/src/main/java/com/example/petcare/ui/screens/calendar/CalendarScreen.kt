package com.example.petcare.ui.screens.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.ui.components.CalendarWidget
import com.example.petcare.ui.components.EmptyStateView
import com.example.petcare.ui.components.EventCard
import com.example.petcare.ui.components.Filters
import com.example.petcare.ui.theme.GreenTextDark
import com.example.petcare.ui.theme.PetCareTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onAddEvent: () -> Unit = {},
    onNavigateToEvent: (petId: String, eventId: String) -> Unit = { _, _ -> }
) {
    val viewModel: CalendarViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("All") }
    var startDate      by remember { mutableStateOf(LocalDate.now()) }
    var endDate        by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth   by remember { mutableStateOf(YearMonth.now()) }

    // Load data once
    LaunchedEffect(Unit) { viewModel.loadData() }

    // Events that fall on selected date(s)
    val eventsForRange = remember(uiState.events, startDate, endDate, selectedFilter) {
        uiState.events.filter { event ->
            try {
                val eventDate = LocalDate.parse(event.date.take(10))
                val inRange   = !eventDate.isBefore(startDate) && !eventDate.isAfter(endDate)
                val matchFilter = when (selectedFilter) {
                    "Vaccines"     -> event.eventType.name == "VACCINE"
                    "Appointments" -> event.eventType.name != "VACCINE"
                    else           -> true
                }
                inRange && matchFilter
            } catch (e: Exception) { false }
        }
    }

    // Vaccine due dates that fall on selected date(s)
    val vaccinesForRange = remember(uiState.vaccinations, startDate, endDate, selectedFilter) {
        if (selectedFilter == "Appointments") return@remember emptyList()
        uiState.vaccinations.filter { vacc ->
            try {
                val dueDate = LocalDate.parse(
                    (vacc.nextDueDate ?: vacc.dateGiven).take(10)
                )
                !dueDate.isBefore(startDate) && !dueDate.isAfter(endDate)
            } catch (e: Exception) { false }
        }
    }

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
            text       = "Calendar",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.Black
        )

        Spacer(modifier = Modifier.height(24.dp))

        CalendarWidget(
            currentMonth    = currentMonth,
            startDate       = startDate,
            endDate         = endDate,
            // Pass dates that have events so the widget can mark them
            markedDates     = uiState.allEventDates + uiState.allVaccineDates,
            onDateSelected  = { date ->
                when {
                    startDate != endDate && (date == startDate || date == endDate) -> {
                        startDate = date; endDate = date
                    }
                    startDate == endDate -> {
                        if (date < startDate) startDate = date
                        else if (date > endDate) endDate = date
                    }
                    else -> {
                        if (date < startDate) startDate = date
                        else if (date > endDate) endDate = date
                        else endDate = date
                    }
                }
            },
            onMonthChanged = { currentMonth = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Filters(
            filters          = listOf("All", "Vaccines", "Appointments"),
            selectedFilter   = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Date header
        val dateText = if (startDate == endDate) {
            startDate.format(DateTimeFormatter.ofPattern("dd MMMM")).uppercase()
        } else {
            "${startDate.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${endDate.format(DateTimeFormatter.ofPattern("dd MMM"))}".uppercase()
        }
        Text(
            text       = dateText,
            style      = MaterialTheme.typography.labelSmall,
            color      = GreenTextDark,
            fontWeight = FontWeight.Bold,
            fontSize   = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Loading
        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        // Vaccine due dates
        vaccinesForRange.forEach { vacc ->
            val dateLabel = vacc.nextDueDate?.take(10) ?: vacc.dateGiven.take(10)
            EventCard(
                eventName = "Vaccine due: ${vacc.vaccineId.take(8)}",
                pet       = vacc.petName,
                date      = dateLabel
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Events
        eventsForRange.forEach { event ->
            Box(modifier = Modifier.clickable {
                onNavigateToEvent(event.petId, event.id)
            }) {
                EventCard(
                    eventName = event.title,
                    pet       = uiState.petNames[event.petId] ?: "",
                    date      = event.date.take(10)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Empty state
        if (!uiState.isLoading && eventsForRange.isEmpty() && vaccinesForRange.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            EmptyStateView(
                icon          = Icons.Default.CalendarMonth,
                message       = "No events on this day",
                buttonText    = "Add Event",
                onButtonClick = onAddEvent
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    PetCareTheme { CalendarScreen() }
}