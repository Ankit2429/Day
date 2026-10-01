package com.day.app.ui.components

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
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DaySurface

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = DaySurface,
    shadowOffset: Dp = 0.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = DayBorderSubtle,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape: Shape = RoundedCornerShape(cornerRadius)

    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val animatedAlpha by animateFloatAsState(
            targetValue = if (isPressed) 0.8f else 1f,
            label = "card_press_alpha"
        )

        Box(
            modifier = modifier
                .alpha(animatedAlpha)
                .clip(shape)
                .background(color = backgroundColor, shape = shape)
                .border(width = borderWidth, color = borderColor, shape = shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(color = backgroundColor, shape = shape)
                .border(width = borderWidth, color = borderColor, shape = shape)
        ) {
            content()
        }
    }
}
