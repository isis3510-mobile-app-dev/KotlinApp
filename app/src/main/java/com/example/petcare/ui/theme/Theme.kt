package com.example.petcare.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.petcare.data.preferences.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    secondary = GreenAccentLight,
    tertiary = White,
    background = DarkBackground,
    surface = DarkSurface,
    primaryContainer = GreenDark,
    secondaryContainer = GreenAccentDark,
    surfaceVariant = Color(0xFF323636),
    onBackground = White,
    onSurface = White,
    error = ErrorContent,
    errorContainer = ErrorContainer,
    onPrimary = GrayDark,
    onSecondary = GrayDark,
    onTertiary = Color(0xDCBBBBBB)
)

private val LightColorScheme = lightColorScheme(
    primary = GreenLight,
    secondary = GreenDark,
    tertiary = Black,
    background = OffWhite,
    surface =  White,
    primaryContainer = GreenLight,
    secondaryContainer = GreenDark,
    surfaceVariant = OffWhite,
    error = ErrorContent,
    errorContainer = ErrorContainer,
    onPrimary = GrayBackground,
    onSecondary = White,
    onTertiary = Color(0xDC737373),
    onBackground = Black
    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)
@Composable
fun PetCareTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.ADAPTIVE -> {
            val hour = java.time.LocalTime.now().hour
            hour < 6 || hour >= 18
        }
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}