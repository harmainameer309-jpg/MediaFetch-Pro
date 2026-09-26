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

private val DarkColorScheme = darkColorScheme(
    primary = EmberOrange,
    onPrimary = Color.White,
    primaryContainer = CoralRed,
    onPrimaryContainer = Color.White,
    secondary = CoralRed,
    onSecondary = Color.White,
    secondaryContainer = SlateDark700,
    onSecondaryContainer = SlateTextPrimary,
    tertiary = AmberGlow,
    onTertiary = SlateDark900,
    background = SlateDark900,
    onBackground = SlateTextPrimary,
    surface = SlateDark800,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateDark700,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorder
)

private val LightColorScheme = lightColorScheme(
    primary = EmberOrange,
    onPrimary = Color.White,
    primaryContainer = CoralRed,
    onPrimaryContainer = Color.White,
    secondary = CoralRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECE5),
    onSecondaryContainer = Color(0xFF6B1D00),
    tertiary = AmberGlow,
    onTertiary = Color.Black,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0)
)

@Composable
fun MediaFetchProTheme(
    darkTheme: Boolean = true, // Default to sleek media-style dark theme
    dynamicColor: Boolean = false, // Keep high contrast brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
