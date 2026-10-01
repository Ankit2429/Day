package com.day.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DayDestructive
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DaySurfaceHighlight
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary

enum class NeoBadgeVariant {
    TEAL,
    YELLOW,
    MAGENTA,
    BLACK,
    RED,
    WHITE
}

@Composable
fun NeoBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: NeoBadgeVariant = NeoBadgeVariant.WHITE,
    cornerRadius: Dp = 999.dp
) {
    val (bgColor, textColor, borderColor) = when (variant) {
        NeoBadgeVariant.TEAL,
        NeoBadgeVariant.WHITE,
        NeoBadgeVariant.YELLOW -> Triple(
            DaySurfaceElevated,
            DayTextPrimary,
            DayBorderSubtle
        )
        NeoBadgeVariant.BLACK,
        NeoBadgeVariant.MAGENTA -> Triple(
            DaySurfaceHighlight,
            DayTextSecondary,
            DayBorderSubtle
        )
        NeoBadgeVariant.RED -> Triple(
            Color(0xFF261214),
            DayDestructive,
            Color(0x33FF453A)
        )
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(color = bgColor, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.sp
        )
    }
}
