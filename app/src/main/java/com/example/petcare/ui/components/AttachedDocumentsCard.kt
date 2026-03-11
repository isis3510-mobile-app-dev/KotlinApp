package com.example.petcare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.data.model.AttachedDocument
import com.example.petcare.ui.theme.GrayBorder
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.OffWhite

@Composable
fun AttachedDocumentsCard(
    documents: List<AttachedDocument>,
    onAddClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "ATTACHED DOCUMENTS",
                style = MaterialTheme.typography.labelMedium,
                color = GrayText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (documents.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    documents.forEach { doc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Document",
                                tint = GreenDark,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = doc.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.clickable { onAddClicked() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GreenDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Document", tint = Color.White)
                    }
                    Text(
                        text = "Attach Document.",
                        fontWeight = FontWeight.Bold,
                        color = GreenDark,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun AttachedDocumentsCardPreview() {
    PetCareTheme {
        Box(modifier = Modifier.background(OffWhite).padding(16.dp)) {
            AttachedDocumentsCard(
                documents = listOf(
                    AttachedDocument(id = "1", fileName = "Rabies_Certificate.pdf")
                ),
                onAddClicked = {}
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun AttachedDocumentsCardEmptyPreview() {
    PetCareTheme {
        Box(modifier = Modifier.background(OffWhite).padding(16.dp)) {
            AttachedDocumentsCard(
                documents = emptyList(),
                onAddClicked = {}
            )
        }
    }
}
