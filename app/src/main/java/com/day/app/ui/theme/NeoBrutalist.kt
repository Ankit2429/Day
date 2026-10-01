package com.day.app.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// =====================================================================
// THE DAY — Spacing, Radii, and Border Design Tokens
// =====================================================================

object DaySpacing {
    val micro = 4.dp
    val small = 8.dp
    val compact = 12.dp
    val standard = 16.dp
    val section = 24.dp
    val major = 32.dp
    val pageHorizontal = 20.dp
}

object DayRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val pill = 999.dp
}

// Backward-compatible spacing mapping
object NeoSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

// Minimal elevation tokens (Soft/flat, zero hard offset shadows)
object NeoElevation {
    val None = 0.dp
    val Subtle = 0.dp
    val Prominent = 2.dp
    val Giant = 4.dp
}

// Subtle border widths (Thin, low-contrast 1dp lines)
object NeoStroke {
    val thin = 0.5.dp
    val standard = 1.dp
    val thick = 1.5.dp
    val giant = 2.dp
}

/**
 * Minimal shadow replacement: Clean zero-blur subtle treatment
 */
fun Modifier.neoHardShadow(
    offset: Dp = NeoElevation.None,
    shadowColor: Color = Color.Transparent,
    cornerRadius: Dp = 16.dp
): Modifier = this

/**
 * Subtle low-contrast border (Thin 1dp border with rounded corners)
 */
fun Modifier.neoBorder(
    width: Dp = NeoStroke.standard,
    color: Color = DayBorderSubtle,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier = this.border(width = width, color = color, shape = shape)

/**
 * Clean interactive card/button wrapper with soft press feedback
 */
@Composable
fun NeoInteractiveBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shadowOffset: Dp = 0.dp,
    backgroundColor: Color = DaySurface,
    borderWidth: Dp = NeoStroke.standard,
    borderColor: Color = DayBorderSubtle,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.75f else 1f,
        label = "press_alpha"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .alpha(animatedAlpha)
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        content()
    }
}

/**
 * Clickable feedback modifier for clean list items and pills
 */
fun Modifier.neoClickable(
    enabled: Boolean = true,
    shadowOffset: Dp = 0.dp,
    onClick: () -> Unit
): Modifier = this.clickable(enabled = enabled, onClick = onClick)
