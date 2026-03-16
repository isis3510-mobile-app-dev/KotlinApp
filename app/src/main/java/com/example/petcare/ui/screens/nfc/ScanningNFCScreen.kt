package com.example.petcare.ui.screens.nfc

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.MainActivity
import com.example.petcare.ui.screens.nfc.components.NFCScanLoadingRing
import com.example.petcare.ui.theme.ErrorContent
import com.example.petcare.ui.theme.GrayDark
import com.example.petcare.ui.theme.GreenDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanningNFCScreen(
    onBack: () -> Unit = {},
    onWriteSuccess: () -> Unit = {},
    onReadSuccess: () -> Unit = {},
) {
    val activity     = LocalActivity.current as MainActivity
    val nfcViewModel = activity.nfcViewModel
    val uiState by nfcViewModel.uiState.collectAsStateWithLifecycle()

    // Navigate away only on success — errors stay on this screen
    // so the user can retry without going back to the previous screen.
    LaunchedEffect(uiState) {
        when (uiState) {
            is NfcUiState.WriteSuccess -> onWriteSuccess()
            is NfcUiState.ReadSuccess  -> onReadSuccess()
            else -> Unit
        }
    }

    val isWriteMode  = nfcViewModel.isPendingWrite()
    val isProcessing = uiState is NfcUiState.ProcessingTag
    val isError      = uiState is NfcUiState.Error
    val isWaiting    = uiState is NfcUiState.WaitingForTag

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC Tag", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        nfcViewModel.reset()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // ── Animated ring — shown unless there's an error ─────────────────
            if (!isError) {
                NFCScanLoadingRing()
                Spacer(modifier = Modifier.height(48.dp))
            } else {
                // Error icon placeholder to keep layout stable
                Spacer(modifier = Modifier.height(240.dp))
            }

            // ── Title ─────────────────────────────────────────────────────────
            when {
                isError -> {
                    val message = (uiState as NfcUiState.Error).message
                    Text(
                        text = "Something went wrong",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = ErrorContent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = GrayDark,
                        textAlign = TextAlign.Center
                    )
                }
                isProcessing -> {
                    Text(
                        text = if (isWriteMode) "Writing..." else "Reading...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hold still — processing tag",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GrayDark
                    )
                }
                else -> {
                    Text(
                        text = if (isWriteMode) "Writing..." else "Scanning...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hold your device near the NFC tag",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GrayDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Pulsing dots — shown only while waiting ───────────────────────
            if (isWaiting) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Box(modifier = Modifier.size(8.dp).background(GreenDark, CircleShape))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Action buttons — change depending on state ────────────────────
            when {
                isError -> {
                    // If write payload is still pending, offer a quick retry
                    if (nfcViewModel.isPendingWrite()) {
                        Button(
                            onClick = { nfcViewModel.retryWrite() },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text("Try again", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {
                            nfcViewModel.reset()
                            onBack()
                        }
                    )
                }
                else -> {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {
                            nfcViewModel.reset()
                            onBack()
                        }
                    )
                }
            }
        }
    }
}