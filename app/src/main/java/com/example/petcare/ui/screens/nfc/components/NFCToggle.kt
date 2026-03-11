package com.example.petcare.ui.screens.nfc.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.petcare.ui.components.ButtonSwitch
import com.example.petcare.ui.theme.PetCareTheme

@Composable
fun NFCToggle(
    isReadMode: Boolean,
    onModeChanged: (Boolean) -> Unit
) {
    ButtonSwitch(
        option1 = "Read Tag",
        option2 = "Write Tag",
        selectedIndex = if (isReadMode) 0 else 1,
        onSelectionChange = { index ->
            onModeChanged(index == 0)
        }
    )
}

@Preview(showBackground = true)
@Composable
fun NFCTogglePreview() {
    PetCareTheme {
        NFCToggle(isReadMode = true, onModeChanged = {})
    }
}
