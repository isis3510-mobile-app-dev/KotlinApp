package com.example.petcare.ui.screens.petprofile

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petcare.ui.components.DateTextField
import com.example.petcare.ui.theme.GrayBorder
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.containsOnlyWhitespace
import com.example.petcare.util.sanitizeForEditing
import com.example.petcare.util.validateLiveInput
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPetBottomSheet(
    petId: String,
    initialName: String,
    initialBreed: String,
    initialWeight: String,
    initialColor: String,
    initialBirthDate: String,
    initialKnownAllergies: String,
    initialDefaultVet: String,
    initialDefaultClinic: String,
    initialPhotoUrl: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val app = (LocalContext.current.applicationContext as android.app.Application)
    val editViewModel: EditPetViewModel = viewModel(
        factory = com.example.petcare.ApplicationViewModelFactory(app)
    )
    val state by editViewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        editViewModel.init(
            name           = initialName,
            breed          = initialBreed,
            weight         = initialWeight,
            color          = initialColor,
            birthDate      = initialBirthDate,
            knownAllergies = initialKnownAllergies,
            defaultVet     = initialDefaultVet,
            defaultClinic  = initialDefaultClinic,
            photoUrl       = initialPhotoUrl
        )
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            editViewModel.resetSaved()
            onSaved()
        }
    }

    var showPhotoOptions by remember { mutableStateOf(false) }

    val cameraUri = remember {
        val file = File(context.cacheDir, "photos/edit_pet_${System.currentTimeMillis()}.jpg")
            .also { it.parentFile?.mkdirs() }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { editViewModel.setPendingPhotoUri(cameraUri); showPhotoOptions = false }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { editViewModel.setPendingPhotoUri(it); showPhotoOptions = false }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { showPhotoOptions = false; cameraLauncher.launch(cameraUri) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        // The scrollable form can fight sheet dragging and make the sheet jitter during flings.
        sheetGesturesEnabled = false,
        dragHandle       = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Edit Pet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Column(
                modifier            = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── Photo ─────────────────────────────────────────────────
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    SectionLabel("PHOTO")
                    Spacer(Modifier.height(12.dp))

                    Box(contentAlignment = Alignment.BottomEnd) {
                        val imageData: Any? = state.pendingPhotoUri
                            ?: state.photoUrl?.takeIf { it.isNotBlank() }

                        if (imageData != null) {
                            AsyncImage(
                                model              = ImageRequest.Builder(context).data(imageData).crossfade(true).build(),
                                contentDescription = "Pet photo",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showPhotoOptions = true }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, GrayBorder, RoundedCornerShape(20.dp))
                                    .clickable { showPhotoOptions = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Add photo",
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .offset(x = 4.dp, y = 4.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(GreenDark)
                                .clickable { showPhotoOptions = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Change photo",
                                tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (showPhotoOptions) {
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick  = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Camera") }
                            OutlinedButton(
                                onClick  = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Gallery") }
                        }
                    }
                }

                // ── Basic info ────────────────────────────────────────────
                SectionLabel("BASIC INFO")

                EditField("Name *", "e.g. Buddy", state.name, editViewModel::setName, InputTextLimits.PET_NAME)
                EditField("Breed", "e.g. Golden Retriever", state.breed, editViewModel::setBreed, InputTextLimits.BREED)

                EditFieldDecimal(
                    label = "Weight (Kg)",
                    placeholder = "e.g. 4.5",
                    value = state.weight,
                    onValueChange = editViewModel::setWeight
                )

                EditField("Color / Markings", "e.g. Golden, White Chest", state.color, editViewModel::setColor, InputTextLimits.COLOR)

                // FIX: birth date cannot be in the future
                DateTextField(
                    name           = "Date of Birth",
                    label          = state.birthDate.ifBlank { "dd/mm/yyyy" },
                    onDateSelected = editViewModel::setBirthDate,
                    allowFutureDates = false
                )

                // ── Medical info ──────────────────────────────────────────
                SectionLabel("MEDICAL INFO")

                EditField("Veterinarian", "e.g. Dr. Smith", state.defaultVet, editViewModel::setDefaultVet, InputTextLimits.PROVIDER_OR_CLINIC)
                EditField("Clinic", "e.g. Happy Paws Clinic", state.defaultClinic, editViewModel::setDefaultClinic, InputTextLimits.PROVIDER_OR_CLINIC)
                EditField("Known Allergies", "e.g. Pollen, None", state.knownAllergies, editViewModel::setKnownAllergies, InputTextLimits.NOTES)

                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(20.dp))
            }

            // ── Save / Cancel ─────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(28.dp)
                ) { Text("Cancel") }

                Button(
                    onClick  = { editViewModel.save(petId) },
                    enabled  = !state.isSaving,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(28.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenDark)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        style         = MaterialTheme.typography.labelMedium,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun EditField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int? = null
) {
    var localError by remember(label, maxLength) { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value         = value,
            onValueChange = {
                val sanitized = sanitizeForEditing(
                    raw = it,
                    fieldPolicy = InputFieldPolicy.GENERAL_TEXT,
                    maxLength = maxLength
                )
                localError = sanitized.rejectionMessage ?: if (containsOnlyWhitespace(sanitized.value)) {
                    "Only spaces are not allowed."
                } else {
                    null
                }
                onValueChange(sanitized.value)
            },
            placeholder   = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(20.dp),
            isError       = localError != null,
            supportingText = localError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = GreenDark,
                unfocusedBorderColor = GrayBorder
            )
        )
    }
}

// FIX: dedicated decimal field for weight with max length
@Composable
private fun EditFieldDecimal(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var localError by remember(label) { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value         = value,
            onValueChange = {
                val sanitized = sanitizeForEditing(
                    raw = it,
                    fieldPolicy = InputFieldPolicy.DECIMAL,
                    maxLength = InputTextLimits.WEIGHT,
                    maxNumericValue = 199.0
                )
                localError = sanitized.rejectionMessage ?: validateLiveInput(
                    value = sanitized.value,
                    fieldPolicy = InputFieldPolicy.DECIMAL,
                    maxNumericValue = 199.0
                )
                onValueChange(sanitized.value)
            },
            placeholder   = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(20.dp),
            isError       = localError != null,
            supportingText = localError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = GreenDark,
                unfocusedBorderColor = GrayBorder
            )
        )
    }
}
