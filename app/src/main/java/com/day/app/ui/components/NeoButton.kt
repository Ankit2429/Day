package com.day.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DayDestructive
import com.day.app.ui.theme.DaySurface
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DaySurfaceHighlight
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary

enum class NeoButtonVariant {
    FILLED_PRIMARY,     // Pure white fill, black text (Primary action)
    FILLED_YELLOW,      // Subtle elevated surface, white text
    FILLED_MAGENTA,     // Subtle elevated surface, white text
    OUTLINE_SECONDARY,  // Dark surface with hairline border
    DESTRUCTIVE,        // Dark red tint, red text
    DARK                // Elevated dark surface
}

@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NeoButtonVariant = NeoButtonVariant.FILLED_PRIMARY,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    shadowOffset: Dp = 0.dp,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 999.dp, // Default to sleek pill shape
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.7f else 1f,
        label = "btn_press_alpha"
    )

    val (bgColor, textColor, borderColor) = when (variant) {
        NeoButtonVariant.FILLED_PRIMARY -> Triple(
            DayTextPrimary,           // Pure white
            DayBackground,            // Deep black text
            Color.Transparent
        )
        NeoButtonVariant.FILLED_YELLOW,
        NeoButtonVariant.FILLED_MAGENTA,
        NeoButtonVariant.DARK -> Triple(
            DaySurfaceElevated,
            DayTextPrimary,
            DayBorderSubtle
        )
        NeoButtonVariant.OUTLINE_SECONDARY -> Triple(
            DaySurface,
            DayTextPrimary,
            DayBorderSubtle
        )
        NeoButtonVariant.DESTRUCTIVE -> Triple(
            Color(0xFF261214),
            DayDestructive,
            Color(0x33FF453A)
        )
    }

    val finalBgColor = if (enabled) bgColor else DaySurface
    val finalTextColor = if (enabled) textColor else DayTextDisabled
    val finalBorderColor = if (enabled) borderColor else DayBorderSubtle
    val shape: Shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .alpha(animatedAlpha)
            .clip(shape)
            .background(finalBgColor, shape)
            .border(borderWidth, finalBorderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = finalTextColor,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.sp
            )
        }
    }
}
