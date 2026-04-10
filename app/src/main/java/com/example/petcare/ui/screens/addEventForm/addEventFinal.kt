
package com.example.petcare.ui.screens.addEventForm

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.ui.components.*
import com.example.petcare.data.analytics.FeatureClicksTracker
import com.example.petcare.ui.theme.GrayDark
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.RobotoBold
import com.example.petcare.ui.theme.RobotoRegular
import com.example.petcare.util.InputTextLimits

@Composable
fun AddEventFinalForm(
    onclick: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEventViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val bannerTextColor = if (isSystemInDarkTheme()) GrayDark else MaterialTheme.colorScheme.tertiary

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            val mimeType = context.contentResolver.getType(it)
                ?: "application/octet-stream"
            val fileName = com.example.petcare.util.FirebaseDocumentUploader
                .getFileName(context, it)
                ?: "document_${System.currentTimeMillis()}"

            viewModel.addDocument(context, it, mimeType, fileName)
        }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TransparentTopBar(title = "Add New Event", onBackClick = onBack)
            Stepper(currentStep = 3, stepLabels = listOf("Basic Info", "Details", "Overview"))

            // ── Banner informativo ────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(width = 350.dp, height = 88.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column {
                    Text(
                        text = "Almost done!",
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        fontFamily = RobotoBold,
                        fontSize = 14.sp,
                        color = bannerTextColor
                    )
                    Text(
                        text = "Confirm your event's information and optionally attach documents.",
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp),
                        fontFamily = RobotoRegular,
                        fontSize = 12.sp,
                        color = bannerTextColor,
                        lineHeight = 16.sp
                    )
                }
            }

            // ── Resumen read-only ────────────────────────────────────
            TextFieldComponent(
                name = "Event Name", label = state.title,
                value = state.title, onValueChange = viewModel::setTitle,
                maxLength = InputTextLimits.EVENT_TITLE
            )
            TextFieldComponent(
                name = "Date", label = state.date,
                value = state.date, onValueChange = {}
            )
            TextFieldComponent(
                name = "Description", label = state.description,
                value = state.description, onValueChange = viewModel::setDescription,
                maxLength = InputTextLimits.NOTES
            )

            // ── Reminder toggle ──────────────────────────────────────
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
                        modifier = Modifier.semantics {
                            contentDescription = "Set a reminder"
                        }
                    )
                }
            )

            // ── Documentos adjuntos ──────────────────────────────────
            AttachedDocumentsCard(
                documents = state.stagedDocuments.map { staged ->
                    com.example.petcare.data.model.AttachedDocument(
                        id = staged.uri.toString(),
                        fileName = staged.fileName,
                        fileUri = staged.downloadUrl
                    )
                },
                isUploading = state.stagedDocuments.any { it.isUploading },
                onDocumentPicked = { uri, mimeType, fileName ->
                    viewModel.addDocument(context, uri, mimeType, fileName)
                }
            )

            // ── Error ────────────────────────────────────────────────
            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ── Botones ──────────────────────────────────────────────────
        Row {
            ButtonOutline(
                bgColor = MaterialTheme.colorScheme.background,
                outlineColor = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.secondary,
                width = 169.dp, height = 50.57.dp,
                text = "Back", onclick = onBack
            )
            Spacer(modifier = Modifier.width(10.dp))
            ButtonDefault(
                bgColor = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.surface,
                width = 169.dp, height = 50.57.dp,
                text = if (state.isLoading) "Saving…" else "Add Event",
                onclick = {
                    viewModel.submit {
                        FeatureClicksTracker.endRoute()
                        onclick()
                    }
                }
            )
        }
    }
}
