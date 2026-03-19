package com.example.petcare.ui.screens.addEventForm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.ui.components.*
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.RobotoBold
import com.example.petcare.ui.theme.RobotoRegular




@Composable
fun AddEventFinalForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEventViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PetCareTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TransparentTopBar(title = "Add New Event", onBackClick = onBack)
                Stepper(currentStep = 3, stepLabels = listOf("Basic Info", "Details", "Overview"))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(width = 350.dp, height = 88.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Almost done!",
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            fontFamily = RobotoBold, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Confirm your event's information and set a reminder.",
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp),
                            fontFamily = RobotoRegular, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary, lineHeight = 16.sp
                        )
                    }
                }

                TextFieldComponent(
                    name = "Event Name", label = state.title,
                    value = state.title, onValueChange = viewModel::setTitle
                )
                TextFieldComponent(
                    name = "Date", label = state.date,
                    value = state.date, onValueChange = {}
                )
                TextFieldComponent(
                    name = "Description", label = state.description,
                    value = state.description, onValueChange = viewModel::setDescription
                )

                SettingsListItem(
                    icon = Icons.Default.Notifications,
                    iconBackgroundColor = Color(0x99FFECB3),
                    iconTintColor = Color(0xFFFBBC05),
                    title = "Set a reminder",
                    trailingContent = {
                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = viewModel::setReminderEnabled,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = com.example.petcare.ui.theme.GreenDark,
                                uncheckedTrackColor = com.example.petcare.ui.theme.GrayBorder,
                                checkedThumbColor = Color.White
                            ),
                            modifier = Modifier.semantics { contentDescription = "Set a reminder" }
                        )
                    }
                )

                state.error?.let {
                    Text(
                        text = it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row {
                ButtonOutline(
                    bgColor = MaterialTheme.colorScheme.background,
                    outlineColor = MaterialTheme.colorScheme.secondary,
                    textColor = MaterialTheme.colorScheme.secondary,
                    width = 169.dp, height = 50.57.dp, text = "Back", onclick = onBack
                )
                Spacer(modifier = Modifier.width(10.dp))
                ButtonDefault(
                    bgColor = com.example.petcare.ui.theme.GreenDark, textColor = Color.White,
                    width = 169.dp, height = 50.57.dp,
                    text = if (state.isLoading) "Saving…" else "Add Event",
                    onclick = { viewModel.submit { onclick() } }
                )
            }
        }
    }
}

@Preview
@Composable
fun AddEventFinalFormPreview(){
    AddEventFinalForm(
        onclick = {},
        onBack = {},
        viewModel = AddEventViewModel()
    )
}