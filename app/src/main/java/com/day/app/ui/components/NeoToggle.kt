package com.day.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DaySurfaceHighlight
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextTertiary
import kotlin.math.roundToInt

@Composable
fun NeoToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackWidth = 46.dp
    val trackHeight = 26.dp
    val thumbSize = 20.dp
    val maxOffset = trackWidth - thumbSize - 6.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else 0.dp,
        label = "toggle_thumb_offset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) DayTextPrimary else DaySurfaceElevated,
        label = "toggle_track_color"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) DayBackground else DayTextTertiary,
        label = "toggle_thumb_color"
    )

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
            .border(1.dp, DayBorderSubtle, RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.toPx().roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}
