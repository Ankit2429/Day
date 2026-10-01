package com.day.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayBorder
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextTertiary

@Composable
fun NeoCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    shadowOffset: Dp = 0.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val boxScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.7f,
            stiffness = 500f
        ),
        label = "chk_scale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (checked) DayTextPrimary else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(220),
        label = "chk_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) DayTextPrimary else Color(0x38FFFFFF),
        animationSpec = androidx.compose.animation.core.tween(220),
        label = "chk_border"
    )

    val checkAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.0f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "chk_alpha"
    )

    Box(
        modifier = modifier
            .scale(boxScale)
            .size(size)
            .clip(CircleShape)
            .background(bgColor, CircleShape)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checkAlpha > 0.05f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = DayBackground.copy(alpha = checkAlpha),
                modifier = Modifier
                    .size(size * 0.65f)
                    .scale(checkAlpha)
            )
        }
    }
}
