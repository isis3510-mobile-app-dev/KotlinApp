package com.example.petcare.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.example.petcare.data.preferences.AppThemeMode

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.SYSTEM }
