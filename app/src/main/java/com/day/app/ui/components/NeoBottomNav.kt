package com.day.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextTertiary

enum class BottomNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    TODAY("home", "TODAY", Icons.Default.Today),
    TASKS("tasks", "TASKS", Icons.Default.Checklist),
    NOTES("notes", "NOTES", Icons.Default.EditNote),
    FILES("documents", "FILES", Icons.Default.Description),
    SETTINGS("settings", "CONFIG", Icons.Default.Settings)
}

@Composable
fun NeoBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(28.dp)
    val pillShape = RoundedCornerShape(999.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val outerBorder = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.06f)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = barShape,
                    spotColor = Color(0x99000000),
                    ambientColor = Color(0x4D000000)
                )
                .clip(barShape)
                .background(Color(0xEB0A0A0D), barShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    ),
                    barShape
                )
                .border(0.5.dp, outerBorder, barShape)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavDestination.entries.forEach { destination ->
                val isSelected = currentRoute == destination.route
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val tapScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1.0f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.75f,
                        stiffness = 500f
                    ),
                    label = "nav_tap_scale"
                )

                val itemColor by animateColorAsState(
                    targetValue = if (isSelected) DayTextPrimary else DayTextTertiary,
                    animationSpec = androidx.compose.animation.core.tween(200),
                    label = "nav_color"
                )

                val iconSize by animateDpAsState(
                    targetValue = if (isSelected) 23.dp else 21.dp,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f),
                    label = "nav_icon_size"
                )

                val innerPillBorder = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                } else null

                val innerPillBg = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                } else null

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
                    Row(
                        modifier = Modifier
                            .scale(tapScale)
                            .clip(pillShape)
                            .then(
                                if (innerPillBg != null) {
                                    Modifier
                                        .background(innerPillBg, pillShape)
                                        .border(0.5.dp, innerPillBorder!!, pillShape)
                                } else Modifier
                            )
                            .padding(horizontal = if (isSelected) 8.dp else 6.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title,
                        tint = itemColor,
                        modifier = Modifier.size(if (isSelected) 20.dp else 21.dp)
                    )

                    if (isSelected) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = destination.title,
                            color = itemColor,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
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
}
