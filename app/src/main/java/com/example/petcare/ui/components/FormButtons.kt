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

                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Gray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
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