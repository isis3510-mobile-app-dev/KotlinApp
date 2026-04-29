package com.example.petcare.ui.components

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petcare.data.model.AttachedDocument
import com.example.petcare.ui.theme.*
import java.io.File

@Composable
fun AttachedDocumentsCard(
    documents: List<AttachedDocument>,
    onDocumentPicked: (uri: Uri, mimeType: String, fileName: String) -> Unit,
    onDeleteDocument: ((documentId: String) -> Unit)? = null,
    isUploading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context   = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var showSourceOptions by remember { mutableStateOf(false) }
    var cameraUri         by remember { mutableStateOf<Uri?>(null) }

    // ── Launchers ──────────────────────────────────────────────────────────

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri ->
                onDocumentPicked(uri, "image/jpeg", "photo_${System.currentTimeMillis()}.jpg")
            }
        }
        showSourceOptions = false
    }


    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it) ?: "image/*"
            val fileName = com.example.petcare.util.FirebaseDocumentUploader
                .getFileName(context, it) ?: "image_${System.currentTimeMillis()}"
            onDocumentPicked(it, mimeType, fileName)
        }
        showSourceOptions = false
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
            val fileName = com.example.petcare.util.FirebaseDocumentUploader
                .getFileName(context, it) ?: "document_${System.currentTimeMillis()}"
            onDocumentPicked(it, mimeType, fileName)
        }
        showSourceOptions = false
    }

    // ── UI ─────────────────────────────────────────────────────────────────

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text          = "ATTACHED DOCUMENTS",
                style         = MaterialTheme.typography.labelMedium,
                color         = GrayText,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(16.dp))

            // Lista de documentos existentes
            if (documents.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    documents.forEach { doc ->
                        DocumentRow(
                            doc             = doc,
                            onDeleteDocument = onDeleteDocument,
                            onClick         = {
                                doc.fileUri?.let { url ->
                                    try { uriHandler.openUri(url) } catch (_: Exception) {}
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Botón principal de adjuntar
            OutlinedButton(
                onClick  = { showSourceOptions = !showSourceOptions },
                enabled  = !isUploading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(28.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Uploading...", color = MaterialTheme.colorScheme.secondary)
                } else {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Attach Document", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }

            // Opciones de fuente (cámara / galería / archivo)
            if (showSourceOptions && !isUploading) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    SourceButton(
                        label    = "Gallery",
                        icon     = Icons.Default.Image,
                        modifier = Modifier.weight(1f),
                        onClick  = { galleryLauncher.launch("image/*") }
                    )
                    SourceButton(
                        label    = "File",
                        icon     = Icons.Default.Description,
                        modifier = Modifier.weight(1f),
                        onClick  = {
                            fileLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "image/*",
                                    "text/plain"
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.height(44.dp),
        shape    = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun DocumentRow(
    doc: AttachedDocument,
    onClick: () -> Unit,
    onDeleteDocument: ((documentId: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val uri     = doc.fileUri
    val isImage = uri != null && (
            uri.contains("image") || uri.endsWith(".jpg") ||
                    uri.endsWith(".jpeg") || uri.endsWith(".png") || uri.endsWith(".webp")
            )
    val isPdf = doc.fileName.endsWith(".pdf", ignoreCase = true)

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && doc.id != null && onDeleteDocument != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title            = { Text("Delete document") },
            text             = { Text("Remove \"${doc.fileName}\"? This cannot be undone.") },
            confirmButton    = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteDocument(doc.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = uri != null, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail o ícono
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isPdf    -> ErrorContainer
                        isImage  -> InfoContainer
                        else     -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isImage && uri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(uri))
                        .crossfade(true)
                        .build(),
                    contentDescription = doc.fileName,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            } else if (isPdf) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint     = ErrorContent,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = doc.fileName,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (uri != null) {
                Text(
                    text  = "Tap to open",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (uri != null) {
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = "Open",
                    tint     = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (doc.id != null && onDeleteDocument != null) {
                IconButton(
                    onClick  = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete document",
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}