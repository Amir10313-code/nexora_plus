package com.mahirex.nexoraplus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Colours that are not part of Material's scheme. */
@Immutable
data class NexoraColors(
    val headerStart: Color,
    val headerEnd: Color,
    val onHeader: Color,
    val holiday: Color,
    val success: Color,
    val warning: Color
)

// Every foreground/background pair below was checked for a contrast ratio of at least 4.5:1 (WCAG AA).
val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3E1FF), onPrimaryContainer = Color(0xFF1A1466),
    secondary = Color(0xFF0F766E), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDF5EF), onSecondaryContainer = Color(0xFF06352F),
    tertiary = Color(0xFFB4236C), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E8), onTertiaryContainer = Color(0xFF4A0A2A),
    background = Color(0xFFF6F6FB), onBackground = Color(0xFF1B1B26),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1B1B26),
    surfaceVariant = Color(0xFFE8E8F3), onSurfaceVariant = Color(0xFF4A4A5E),
    surfaceContainer = Color(0xFFEFEFF8), surfaceContainerHigh = Color(0xFFE9E9F4),
    surfaceContainerHighest = Color(0xFFE3E3F0), surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Color(0xFF767689), outlineVariant = Color(0xFFC9C9D9),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)

val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFB9B6FF), onPrimary = Color(0xFF1D1A66),
    primaryContainer = Color(0xFF3A33A8), onPrimaryContainer = Color(0xFFE3E0FF),
    secondary = Color(0xFF5FD8C7), onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF0F5A50), onSecondaryContainer = Color(0xFFBDF2E9),
    tertiary = Color(0xFFFFB0D2), onTertiary = Color(0xFF5B0B37),
    tertiaryContainer = Color(0xFF8A1E55), onTertiaryContainer = Color(0xFFFFD9E8),
    background = Color(0xFF11111A), onBackground = Color(0xFFECECF6),
    surface = Color(0xFF191924), onSurface = Color(0xFFECECF6),
    surfaceVariant = Color(0xFF2A2A3A), onSurfaceVariant = Color(0xFFC0C0D4),
    surfaceContainer = Color(0xFF20202D), surfaceContainerHigh = Color(0xFF262634),
    surfaceContainerHighest = Color(0xFF2D2D3C), surfaceContainerLow = Color(0xFF1C1C28),
    surfaceContainerLowest = Color(0xFF0D0D14),
    outline = Color(0xFF8B8BA1), outlineVariant = Color(0xFF3C3C4E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
)

val LightExtras = NexoraColors(
    headerStart = Color(0xFF4F46E5), headerEnd = Color(0xFF7C3AED), onHeader = Color(0xFFFFFFFF),
    holiday = Color(0xFFC62828), success = Color(0xFF1B7F3B), warning = Color(0xFF9A5B00)
)

val DarkExtras = NexoraColors(
    headerStart = Color(0xFF3B2FC9), headerEnd = Color(0xFF5B21B6), onHeader = Color(0xFFFFFFFF),
    holiday = Color(0xFFFF8A80), success = Color(0xFF6FD98B), warning = Color(0xFFF2B84B)
)
