package com.example.petcare.ui.theme

//import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.petcare.data.preferences.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = GreenDark,
    secondary = GreenDark,
    tertiary = Black,
    background = OffWhite,
    surface =  White,
    primaryContainer = GreenLight,
    secondaryContainer = GreenDark,
    surfaceVariant = OffWhite,
    error = ErrorContent,
    errorContainer = ErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = GreenDark,
    secondary = GreenDark,
    tertiary = Black,
    background = OffWhite,
    surface =  White,
    primaryContainer = GreenLight,
    secondaryContainer = GreenDark,
    surfaceVariant = OffWhite,
    error = ErrorContent,
    errorContainer = ErrorContainer
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
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}