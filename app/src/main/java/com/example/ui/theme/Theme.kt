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
    primary = SosRed,
    onPrimary = Color.White,
    primaryContainer = SosRedContainer,
    onPrimaryContainer = SosRedGlow,
    secondary = AmberLocating,
    onSecondary = Color.Black,
    secondaryContainer = AmberLocatingContainer,
    onSecondaryContainer = AmberLocating,
    tertiary = SuccessGreen,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

private val LightColorScheme = darkColorScheme(
    primary = SosRed,
    onPrimary = Color.White,
    primaryContainer = SosRedContainer,
    onPrimaryContainer = SosRedDark,
    secondary = AmberLocating,
    onSecondary = Color.Black,
    tertiary = SuccessGreen,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Emergency app looks clearest and highest contrast in deep tactical dark mode
    dynamicColor: Boolean = false, // Keep consistent high-visibility emergency red branding
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

