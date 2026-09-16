package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val PurpleDarkColorScheme = darkColorScheme(
    primary = DeepPurple500,
    onPrimary = Color.White,
    primaryContainer = PurpleContainerDark,
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = ElectricBlue400,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = BlueContainerDark,
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = AmberAccent,
    onTertiary = Color.Black,
    background = DarkCanvas,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkCardBorder,
    error = ErrorRed,
    onError = Color.White
)

private val PurpleLightColorScheme = lightColorScheme(
    primary = DeepPurple700,
    onPrimary = Color.White,
    primaryContainer = PurpleContainerLight,
    onPrimaryContainer = DeepPurple900,
    secondary = ElectricBlue600,
    onSecondary = Color.White,
    secondaryContainer = BlueContainerLight,
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = AmberAccent,
    onTertiary = Color.White,
    background = CleanWhiteCanvas,
    onBackground = TextPrimaryLight,
    surface = CleanWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF5F3FF),
    onSurfaceVariant = TextSecondaryLight,
    outline = CardBorderLight,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyanmarVoiceTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) PurpleDarkColorScheme else PurpleLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyanmarVoiceTheme(
        themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
        content = content
    )
}
