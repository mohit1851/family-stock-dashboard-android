package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProfessionalLightColorScheme = lightColorScheme(
    primary = RawBluePrimary,
    onPrimary = RawBlueOnPrimary,
    primaryContainer = RawBlueContainer,
    onPrimaryContainer = RawBlueOnContainer,
    background = RawBackgroundGray,
    onBackground = RawTextDark,
    surface = RawSurfaceWhite,
    onSurface = RawTextDark,
    outline = RawBorderColor,
    secondary = RawTextSubtle,
    error = SoftRed
)

private val ProfessionalDarkColorScheme = darkColorScheme(
    primary = RawBlueContainer,
    onPrimary = RawBlueOnContainer,
    primaryContainer = RawBluePrimary,
    onPrimaryContainer = RawBlueOnPrimary,
    background = Color(0xFF121316),
    onBackground = Color(0xFFE1E2E6),
    surface = Color(0xFF1E1F22),
    onSurface = Color(0xFFE1E2E6),
    outline = Color(0xFF43474E),
    secondary = Color(0xFF8C9199),
    error = Color(0xFFFFB4AB)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        ProfessionalDarkColorScheme
    } else {
        ProfessionalLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
