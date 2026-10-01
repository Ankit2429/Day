package com.day.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary

/**
 * THE DAY — Centralized Reusable Frosted Glass Surface
 * Implements layered glass with subtle specular edge gradient,
 * soft ambient shadow, and fine luminance variation.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    surfaceAlpha: Float = 0.055f, // rgba(255,255,255, 0.055)
    borderAlpha: Float = 0.12f,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.975f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 500f
        ),
        label = "glass_scale"
    )

    val currentSurfaceAlpha by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) (surfaceAlpha * 1.6f).coerceAtMost(0.40f) else surfaceAlpha,
        animationSpec = spring(stiffness = 600f),
        label = "glass_surface_alpha"
    )

    val currentBorderAlpha by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) (borderAlpha * 1.5f).coerceAtMost(0.50f) else borderAlpha,
        animationSpec = spring(stiffness = 600f),
        label = "glass_border_alpha"
    )

    val surfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = currentSurfaceAlpha * 1.4f),
            Color.White.copy(alpha = currentSurfaceAlpha * 0.7f)
        )
    )

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = currentBorderAlpha),
            Color.White.copy(alpha = currentBorderAlpha * 0.25f)
        )
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = if (isPressed) elevation * 0.6f else elevation,
                        shape = shape,
                        spotColor = Color(0x40000000),
                        ambientColor = Color(0x20000000)
                    )
                } else Modifier
            )
            .clip(shape)
            .background(Color(0xCC08080A), shape) // Deep dark base layer
            .background(surfaceBrush, shape)     // Frosted glass overlay
            .border(BorderStroke(0.5.dp, borderBrush), shape)
            .then(clickableModifier)
    ) {
        content()
    }
}

/**
 * Pill-shaped glass container (for tabs, quick action badges, chips)
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val pillShape = RoundedCornerShape(999.dp)
    val surfaceAlpha = if (isSelected) 0.14f else 0.055f
    val borderAlpha = if (isSelected) 0.24f else 0.09f

    GlassSurface(
        modifier = modifier,
        shape = pillShape,
        surfaceAlpha = surfaceAlpha,
        borderAlpha = borderAlpha,
        onClick = onClick,
        content = content
    )
}

/**
 * Frosted Glass Card for featured items (UP NEXT, Dialogs, etc.)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    isElevated: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        surfaceAlpha = if (isElevated) 0.085f else 0.055f,
        borderAlpha = if (isElevated) 0.15f else 0.10f,
        elevation = if (isElevated) 8.dp else 0.dp,
        onClick = onClick,
        content = content
    )
}

/**
 * Frosted Glass Active/Current Task Card along the timeline
 */
@Composable
fun GlassActiveTask(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        surfaceAlpha = 0.095f,
        borderAlpha = 0.22f,
        elevation = 6.dp,
        onClick = onClick,
        content = content
    )
}

/**
 * Subtle Frosted Glass selection capsule for Calendar Date
 */
@Composable
fun GlassCalendarSelection(
    modifier: Modifier = Modifier,
    isSelected: Boolean = true,
    isToday: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    val surfaceAlpha = if (isSelected) 0.20f else if (isToday) 0.08f else 0.0f
    val borderAlpha = if (isSelected) 0.35f else if (isToday) 0.16f else 0.0f

    GlassSurface(
        modifier = modifier,
        shape = shape,
        surfaceAlpha = surfaceAlpha,
        borderAlpha = borderAlpha,
        elevation = if (isSelected) 4.dp else 0.dp,
        content = content
    )
}

/**
 * Frosted Glass Button with spring-scale interaction
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
) {
    val shape = RoundedCornerShape(999.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 500f
        ),
        label = "btn_spring"
    )

    val surfaceBrush = if (isPrimary) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.88f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.09f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    val borderBrush = if (isPrimary) {
        Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.7f)))
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    val textColor = if (isPrimary) DayBackground else DayTextPrimary

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(if (isPrimary) Color.White else Color(0xCC0D0D10), shape)
            .background(surfaceBrush, shape)
            .border(BorderStroke(0.5.dp, borderBrush), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            letterSpacing = 0.2.sp
        )
    }
}

/**
 * Ambient Glass Background:
 * Adds a soft, subtle radial illumination at the top center of screens and
 * a gentle bottom falloff, matching the luxury dark depth without neon or color noise.
 */
@Composable
fun AmbientGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val ambientTopGlow = Brush.radialGradient(
        colors = listOf(
            Color(0x0EFFFFFF), // 5.5% white diffuse center
            Color(0x04FFFFFF), // 1.5% white falloff
            Color.Transparent
        ),
        center = androidx.compose.ui.geometry.Offset(x = 540f, y = 80f),
        radius = 850f
    )

    val ambientBottomGlow = Brush.radialGradient(
        colors = listOf(
            Color(0x06FFFFFF),
            Color.Transparent
        ),
        center = androidx.compose.ui.geometry.Offset(x = 540f, y = 2000f),
        radius = 900f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DayBackground)
            .background(ambientTopGlow)
            .background(ambientBottomGlow)
    ) {
        content()
    }
}

/**
 * Premium Floating Glass Upload / Add Button
 * A tactile circular glass action control with soft glow and spring scale.
 */
@Composable
fun GlassFloatingUpload(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "UPLOAD",
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Add
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 500f
        ),
        label = "fab_scale"
    )

    val shape = RoundedCornerShape(999.dp)
    val outerBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isPressed) 0.35f else 0.22f),
            Color.White.copy(alpha = 0.08f)
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                spotColor = Color(0x66000000),
                ambientColor = Color(0x33000000)
            )
            .clip(shape)
            .background(Color(0xF0101014), shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isPressed) 0.16f else 0.10f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape
            )
            .border(0.5.dp, outerBorder, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DayTextPrimary,
                modifier = Modifier.size(18.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = DayTextPrimary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}
