package com.day.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.navigation.Screen
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextTertiary

/**
 * THE DAY — PREMIUM GLOSSY GLASS BOTTOM NAVIGATION
 *
 * Direct native Jetpack Compose implementation of reference design:
 * - One continuous horizontal smoked glass capsule for primary destinations:
 *   [ Today | Tasks | Notes | Files ]
 * - One physically separate circular glass Search button
 * - Smoked black glass material with realistic diffuse shadow & subtle top rim light-catch
 * - Fluid sliding inner glass capsule that glides between destinations with spring physics
 * - Respects safe area insets (navigationBarsPadding) floating 12dp above system gesture bar
 */

enum class GlassNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    TODAY(Screen.Home.route, "Today", Icons.Default.Today),
    TASKS(Screen.Tasks.route, "Tasks", Icons.Default.Checklist),
    NOTES(Screen.Notes.route, "Notes", Icons.Default.EditNote),
    FILES(Screen.Documents.route, "Files", Icons.Default.Description)
}

@Composable
fun PremiumGlassBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(34.dp)
    val destinations = GlassNavDestination.entries

    val selectedIndex = destinations.indexOfFirst { it.route == currentRoute }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // ── 1. MAIN HORIZONTAL GLASS CAPSULE ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = barShape,
                        spotColor = Color(0xAA000000),
                        ambientColor = Color(0x55000000)
                    )
                    .clip(barShape)
                    // Deep smoked black glass base
                    .background(Color(0xF008080A), barShape)
                    // Subtle vertical translucent gloss
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        ),
                        barShape
                    )
                    // Edge highlight (lighter on top edge, softer on bottom)
                    .border(
                        width = 0.75.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = barShape
                    )
            ) {
                // Subtle top-rim light reflection hairline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 28.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Responsive inner area for sliding indicator and tab items
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    val count = destinations.size
                    val slotWidth = maxWidth / count
                    val indicatorWidth = slotWidth - 4.dp
                    val indicatorHeight = 54.dp
                    val indicatorShape = RoundedCornerShape(26.dp)

                    // Target X offset for the sliding glass capsule
                    val targetX = if (selectedIndex >= 0) {
                        (slotWidth * selectedIndex) + 2.dp
                    } else {
                        2.dp
                    }

                    // Smooth spring animation for sliding capsule movement
                    val animatedX by animateDpAsState(
                        targetValue = targetX,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = 380f
                        ),
                        label = "sliding_capsule_x"
                    )

                    // ── INNER FLOATING GLASS CAPSULE (SLIDING INDICATOR) ───────────
                    if (selectedIndex >= 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = animatedX, y = 0.dp)
                                .width(indicatorWidth)
                                .height(indicatorHeight)
                                .clip(indicatorShape)
                                .background(Color(0x2EFFFFFF), indicatorShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.16f),
                                            Color.White.copy(alpha = 0.04f)
                                        )
                                    ),
                                    indicatorShape
                                )
                                .border(
                                    width = 0.75.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.10f)
                                        )
                                    ),
                                    shape = indicatorShape
                                )
                        ) {
                            // Inner highlight hairline inside the selected capsule
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .padding(horizontal = 12.dp)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.40f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // ── TAB ITEMS ROW ──────────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        destinations.forEachIndexed { index, destination ->
                            val isSelected = index == selectedIndex
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()

                            val tapScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.94f else if (isSelected) 1.0f else 0.97f,
                                animationSpec = spring(
                                    dampingRatio = 0.75f,
                                    stiffness = 500f
                                ),
                                label = "nav_item_scale"
                            )

                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) DayTextPrimary else DayTextTertiary,
                                animationSpec = tween(180),
                                label = "nav_item_color"
                            )

                            val contentAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 1.0f else 0.72f,
                                animationSpec = tween(180),
                                label = "nav_item_alpha"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { onNavigate(destination.route) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .scale(tapScale)
                                        .alpha(contentAlpha),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label,
                                        tint = contentColor,
                                        modifier = Modifier.size(22.dp)
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = destination.label,
                                        color = contentColor,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // ── 2. SEPARATE CIRCULAR GLASS SEARCH BUTTON ───────────────────────────
            CircularGlassSearchButton(
                onClick = onSearchClick
            )
        }
    }
}

/**
 * Separate Circular Glass Search Button
 * Matches the smoked black glass material and elevates the composition.
 */
@Composable
private fun CircularGlassSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 500f
        ),
        label = "search_button_scale"
    )

    val buttonSurfaceColor by animateColorAsState(
        targetValue = if (isPressed) Color(0x38FFFFFF) else Color(0xF008080A),
        animationSpec = tween(140),
        label = "search_button_bg"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .scale(buttonScale)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                spotColor = Color(0xAA000000),
                ambientColor = Color(0x55000000)
            )
            .clip(CircleShape)
            .background(buttonSurfaceColor, CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                CircleShape
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle top rim light catch inside circle
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 1.dp)
                .align(Alignment.TopCenter)
                .padding(top = 1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = DayTextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}
