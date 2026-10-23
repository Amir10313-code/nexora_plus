package com.mahirex.nexoraplus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mahirex.nexoraplus.ui.designsystem.Elevation
import com.mahirex.nexoraplus.ui.designsystem.Radius
import com.mahirex.nexoraplus.ui.designsystem.Space
import com.mahirex.nexoraplus.ui.theme.NexoraTheme

/**
 * Layered card: a faint tinted layer peeks out below a bordered surface.
 * The bottom padding reserves room for the peeking layer.
 */
@Composable
fun LayeredCard(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    Box(modifier.padding(bottom = Space.s1)) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = Space.s1 - Space.half)
                .background(accent.copy(alpha = 0.16f), shape)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = Elevation.low,
            tonalElevation = Elevation.low,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            val inner = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            Column(inner.padding(Space.s2), content = content)
        }
    }
}

/** Gradient header used on the home screen. Draws under the status bar. */
@Composable
fun GradientHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = NexoraTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = Radius.r3, bottomEnd = Radius.r3))
            .background(Brush.linearGradient(listOf(c.headerStart, c.headerEnd)))
            .statusBarsPadding()
            .padding(horizontal = Space.s2, vertical = Space.s2),
        content = content
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = Space.s1, bottom = Space.half),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(Space.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s1)
    ) {
        Box(
            Modifier.size(Space.s6 + Space.s4).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(Space.s4))
        }
        Spacer(Modifier.height(Space.half))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.s1))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Shimmering placeholder block shown while data loads. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha = transition.animateFloat(
        initialValue = 0.35f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "skeletonAlpha"
    )
    Box(
        modifier.clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha.value))
    )
}

@Composable
fun SkeletonList(count: Int = 3, modifier: Modifier = Modifier) {
    Column(modifier.padding(Space.s2), verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        repeat(count) { SkeletonBlock(Modifier.fillMaxWidth().height(Space.s6 + Space.s5)) }
    }
}

@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = Space.s2) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun RowSpacer() { Spacer(Modifier.height(Space.s1)) }

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
