package com.day.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary

@Composable
fun NeoProgressBar(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    fillColor: Color = DayTextPrimary,
    trackColor: Color = DaySurfaceElevated,
    height: Dp = 6.dp,
    showPercentage: Boolean = false
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        label = "progress_anim"
    )
    val percentageInt = (clampedProgress * 100).toInt()
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track
        Box(
            modifier = Modifier
                .weight(1f)
                .height(height)
                .clip(shape)
                .background(trackColor, shape)
        ) {
            // Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(shape)
                    .background(fillColor, shape)
            )
        }

        if (showPercentage) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "$percentageInt%",
                color = DayTextSecondary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}
