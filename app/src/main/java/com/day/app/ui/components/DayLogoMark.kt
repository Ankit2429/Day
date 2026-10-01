package com.day.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * THE DAY — Minimal Monochrome Geometric Logo Mark
 * Concept: Day Cycle, Time Horizon & Progress.
 * A precision geometric 'D' cycle with timeline axis and subtle progress node.
 */
@Composable
fun DayLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val strokeWidth = (s * 0.09f).coerceIn(1.5f, 2.5f)
        val pad = strokeWidth / 2f

        val spineX = s * 0.28f
        val topY = pad
        val botY = s - pad
        val arcRadius = (botY - topY) / 2f
        val centerY = (topY + botY) / 2f

        // 1. Timeline Vertical Spine
        drawLine(
            color = Color.White.copy(alpha = 0.45f),
            start = Offset(spineX, topY),
            end = Offset(spineX, botY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // 2. Geometric Sun-Cycle Arc (D-shaped progress curve)
        val arcPath = Path().apply {
            moveTo(spineX, topY)
            arcTo(
                rect = Rect(
                    left = spineX - arcRadius,
                    top = topY,
                    right = spineX + arcRadius,
                    bottom = botY
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(spineX, botY)
        }

        drawPath(
            path = arcPath,
            color = Color.White.copy(alpha = 0.95f),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 3. Tiny Core Progress Accent Dot (Zenith of the cycle)
        drawCircle(
            color = Color.White.copy(alpha = 0.90f),
            radius = strokeWidth * 0.85f,
            center = Offset(spineX + arcRadius * 0.48f, centerY)
        )
    }
}
