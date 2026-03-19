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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTextField(
    label: String = "dd/mm/yyyy",
    name: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf("") }

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
            value = selectedDate,
            onValueChange = { selectedDate = it },
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



@Preview(showBackground = true)
@Composable
fun DateTextFieldPreview() {
    PetCareTheme {
        DateTextField(
            name = "Date *",
            onDateSelected = { date ->
                println("Selected date: $date")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTextField(
    label: String = "hh:mm am/pm",
    name: String,
    onTimeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTime by remember { mutableStateOf("") }

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
        }, hour, minute, false // false for 12 hour view
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
            onValueChange = { selectedTime = it },
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