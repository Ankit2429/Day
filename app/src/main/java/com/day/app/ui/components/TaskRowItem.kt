package com.day.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.domain.model.Priority
import com.day.app.domain.model.Task
import com.day.app.domain.model.Urgency
import com.day.app.ui.theme.DayDestructive
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.day.app.util.TimeFormatter

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import com.day.app.ui.theme.DayGlassSurface

@Composable
fun TaskRowItem(
    task: Task,
    onTaskClick: () -> Unit,
    onCompleteToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val rowScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "task_row_scale"
    )

    val textColor by animateColorAsState(
        targetValue = if (task.completed) DayTextDisabled else DayTextPrimary,
        animationSpec = tween(220),
        label = "task_text_color"
    )

    val metaAlpha by animateFloatAsState(
        targetValue = if (task.completed) 0.35f else 1.0f,
        animationSpec = tween(220),
        label = "task_meta_alpha"
    )

    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    val formattedTime = when {
        task.scheduledTime != null -> {
            val dateStr = if (isToday(task.scheduledTime)) "Today" else dateFormat.format(Date(task.scheduledTime))
            "$dateStr · ${TimeFormatter.formatUserTime(task.scheduledTime)}"
        }
        task.deadline != null -> {
            val dateStr = if (isToday(task.deadline)) "Today" else dateFormat.format(Date(task.deadline))
            "Due $dateStr · ${TimeFormatter.formatUserTime(task.deadline)}"
        }
        else -> null
    }

    val timeOnly = when {
        task.scheduledTime != null -> TimeFormatter.formatUserTime(task.scheduledTime)
        task.deadline != null -> TimeFormatter.formatUserTime(task.deadline)
        else -> null
    }

    val subtitle = when {
        task.scheduledTime != null && !isToday(task.scheduledTime) -> dateFormat.format(Date(task.scheduledTime))
        task.deadline != null && !isToday(task.deadline) -> "Due ${dateFormat.format(Date(task.deadline))}"
        else -> null
    }

    val isOverdue = task.urgency == Urgency.OVERDUE && !task.completed

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(rowScale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) DayGlassSurface else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTaskClick
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Subtle Circular Checkbox
        NeoCheckbox(
            checked = task.completed,
            onCheckedChange = onCompleteToggle,
            size = 20.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        // 2. Title & Subtitle Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = task.title,
                color = textColor,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isOverdue || subtitle != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (isOverdue) {
                        Text(
                            text = "Overdue",
                            color = DayDestructive.copy(alpha = metaAlpha),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                    if (subtitle != null) {
                        Text(
                            text = if (isOverdue) " · $subtitle" else subtitle,
                            color = (if (task.completed) DayTextDisabled else DayTextTertiary).copy(alpha = metaAlpha),
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 3. Time label on the right
        if (timeOnly != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = timeOnly,
                color = (if (task.completed) DayTextDisabled else DayTextSecondary).copy(alpha = metaAlpha),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }

        // 4. Subtle Priority Indicator Dot (Non-intrusive)
        if (!task.completed && task.priority == Priority.HIGH) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(DayTextPrimary)
            )
        }
    }
}

private fun isToday(timestamp: Long): Boolean {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return fmt.format(Date(timestamp)) == fmt.format(Date())
}
