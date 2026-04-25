package com.example.petcare.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GrayBorder
import java.util.*
import androidx.compose.material.icons.filled.AccessTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTextField(
    label: String = "dd/mm/yyyy",
    name: String,
    value: String = "",
    onDateSelected: (String) -> Unit,
    allowFutureDates: Boolean = true,    // NEW: true = allow any date by default
    allowPastDates: Boolean = true,      // NEW: false = restrict to today or future
    minDateAfter: String? = null,        // NEW: if set, minimum selectable date (strictly after)
    minDateInclusive: String? = null     // NEW: if set, minimum selectable date (inclusive)
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        selectedDate = value
    }

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            val formattedDate = "${d.toString().padStart(2, '0')}/${(m+1).toString().padStart(2, '0')}/$y"
            selectedDate = formattedDate
            onDateSelected(formattedDate)
        }, year, month, day
    ).also { dialog ->
        // Restrict future dates (e.g., birth date, date given for vaccine)
        if (!allowFutureDates) {
            dialog.datePicker.maxDate = calendar.timeInMillis
        }
        // Restrict past dates (e.g., if we only want future events — not currently used)
        if (!allowPastDates) {
            dialog.datePicker.minDate = calendar.timeInMillis
        }
        // Minimum date constraint (for next due date > date given)
        if (minDateAfter != null) {
            parseDatePickerInput(minDateAfter)?.let { minDate ->
                dialog.datePicker.minDate = minDate.plusDays(1).toPickerMillis()
            }
        }
        if (minDateInclusive != null) {
            parseDatePickerInput(minDateInclusive)?.let { minDate ->
                dialog.datePicker.minDate = maxOf(dialog.datePicker.minDate, minDate.toPickerMillis())
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = selectedDate,
            onValueChange = {
                selectedDate = it
                onDateSelected(it)
            },
            placeholder = {
                Text(
                    label,
                    color = GrayText
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Calendar",
                    modifier = Modifier.clickable { datePickerDialog.show() },
                    tint = Color.LightGray
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = GrayBorder,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .size(width = 342.dp, height = 53.17.dp)
                .clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(20.dp),
            readOnly = true
        )
    }
}

private fun parseDatePickerInput(raw: String): LocalDate? {
    val value = raw.trim()
    if (value.isBlank()) return null
    return runCatching {
        when {
            value.matches(Regex("""\d{2}/\d{2}/\d{4}""")) ->
                LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            value.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) ->
                LocalDate.parse(value.take(10))
            else -> null
        }
    }.getOrNull()
}

private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTextField(
    label: String = "hh:mm am/pm",
    name: String,
    value: String = "",
    onTimeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTime by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        selectedTime = value
    }

    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, h, m ->
            val amPmHour = if (h == 0) 12 else if (h > 12) h - 12 else h
            val formattedTime = "${amPmHour.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')} ${if(h >= 12) "PM" else "AM"}"
            selectedTime = formattedTime
            onTimeSelected(formattedTime)
        }, hour, minute, false
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = selectedTime,
            onValueChange = {
                selectedTime = it
                onTimeSelected(it)
            },
            placeholder = { Text(label, color = GrayText) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Time",
                    modifier = Modifier.clickable { timePickerDialog.show() },
                    tint = Color.LightGray
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = GrayBorder,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .size(height = 53.17.dp, width = 342.dp)
                .clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(20.dp),
            readOnly = true
        )
    }
}
