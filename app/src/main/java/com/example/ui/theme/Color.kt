package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Raw color definitions used in building the Color Scheme
val RawBluePrimary = Color(0xFF005FAC)
val RawBlueOnPrimary = Color(0xFFFFFFFF)
val RawBlueContainer = Color(0xFFD1E4FF)
val RawBlueOnContainer = Color(0xFF001D36)

val RawBackgroundGray = Color(0xFFF7F9FC)
val RawSurfaceWhite = Color(0xFFFFFFFF)
val RawBorderColor = Color(0xFFE1E2E6)
val RawTextDark = Color(0xFF1A1C1E)
val RawTextSubtle = Color(0xFF74777F)

val SoftGreen = Color(0xFF2E7D32)
val SoftRed = Color(0xFFC62828)

// Theme-responsive dynamic colors
val BluePrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val BlueOnPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onPrimary

val BlueContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primaryContainer

val BlueOnContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onPrimaryContainer

val BackgroundGray: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val SurfaceWhite: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val BorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

val TextDark: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onBackground

val TextSubtle: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.secondary

// Dynamic chat bubbles that adjust beautifully depending on light/dark mode
val ChatBubbleDad: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background.red < 0.5f) Color(0xFF24262A) else Color(0xFFF1F3F9)

val ChatBubbleMe: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background.red < 0.5f) Color(0xFF1E3A5F) else Color(0xFFD1E4FF)

