package com.mahirex.nexoraplus.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.mahirex.nexoraplus.ui.designsystem.Motion
import com.mahirex.nexoraplus.ui.designsystem.Radius

val LocalNexoraColors = staticCompositionLocalOf { LightExtras }

object NexoraTheme {
    val colors: NexoraColors
        @Composable @ReadOnlyComposable get() = LocalNexoraColors.current
}

private val NexoraShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.r1 / 2),
    small = RoundedCornerShape(Radius.r1),
    medium = RoundedCornerShape(Radius.r2),
    large = RoundedCornerShape(Radius.r3),
    extraLarge = RoundedCornerShape(Radius.r4)
)

@Composable
private fun animated(color: Color): Color =
    animateColorAsState(color, tween(Motion.ThemeMs), label = "themeColor").value

@Composable
private fun animatedScheme(t: ColorScheme): ColorScheme = t.copy(
    primary = animated(t.primary), onPrimary = animated(t.onPrimary),
    primaryContainer = animated(t.primaryContainer), onPrimaryContainer = animated(t.onPrimaryContainer),
    secondary = animated(t.secondary), onSecondary = animated(t.onSecondary),
    secondaryContainer = animated(t.secondaryContainer), onSecondaryContainer = animated(t.onSecondaryContainer),
    tertiary = animated(t.tertiary), onTertiary = animated(t.onTertiary),
    tertiaryContainer = animated(t.tertiaryContainer), onTertiaryContainer = animated(t.onTertiaryContainer),
    background = animated(t.background), onBackground = animated(t.onBackground),
    surface = animated(t.surface), onSurface = animated(t.onSurface),
    surfaceVariant = animated(t.surfaceVariant), onSurfaceVariant = animated(t.onSurfaceVariant),
    surfaceContainer = animated(t.surfaceContainer), surfaceContainerHigh = animated(t.surfaceContainerHigh),
    surfaceContainerHighest = animated(t.surfaceContainerHighest), surfaceContainerLow = animated(t.surfaceContainerLow),
    surfaceContainerLowest = animated(t.surfaceContainerLowest),
    outline = animated(t.outline), outlineVariant = animated(t.outlineVariant),
    error = animated(t.error), onError = animated(t.onError),
    errorContainer = animated(t.errorContainer), onErrorContainer = animated(t.onErrorContainer)
)

@Composable
fun NexoraTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val scheme = animatedScheme(if (darkTheme) DarkScheme else LightScheme)
    val target = if (darkTheme) DarkExtras else LightExtras
    val extras = NexoraColors(
        headerStart = animated(target.headerStart),
        headerEnd = animated(target.headerEnd),
        onHeader = animated(target.onHeader),
        holiday = animated(target.holiday),
        success = animated(target.success),
        warning = animated(target.warning)
    )
    CompositionLocalProvider(LocalNexoraColors provides extras) {
        // Typography() keeps the system font on purpose.
        MaterialTheme(colorScheme = scheme, typography = Typography(), shapes = NexoraShapes, content = content)
    }
}
