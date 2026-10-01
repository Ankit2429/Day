package com.day.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.day.app.ui.theme.DayGlassSurface
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.day.app.util.TimeFormatter

/**
 * THE DAY — Swipeable Task Row
 * Supports:
 * - Swipe right: Complete / Reopen task
 * - Swipe left: Delete / Snooze
 * - Subtle pressed scale animation (0.985f)
 * - Animated strikethrough & text fade upon completion
 * - Subtle monochrome priority dot
 * - Respects strict monochrome aesthetic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskRow(
    task: Task,
    onTaskClick: () -> Unit,
    onCompleteToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onCompleteToggle(!task.completed)
                    false // Return false so row snaps back smoothly into place
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF161616)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFF1F1212)
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = if (task.completed) "Reopen" else "Complete",
                            tint = DayTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (task.completed) "REOPEN" else "COMPLETE",
                            color = DayTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DELETE",
                            color = DayDestructive,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = DayDestructive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        val rowScale by animateFloatAsState(
            targetValue = if (isPressed) 0.982f else 1.0f,
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

        val isMidnight = remember(task.scheduledTime) {
            if (task.scheduledTime == null) false
            else {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = task.scheduledTime }
                cal.get(java.util.Calendar.HOUR_OF_DAY) == 0 && cal.get(java.util.Calendar.MINUTE) == 0
            }
        }

        val timeLabel = when {
            task.scheduledTime != null && !isMidnight -> TimeFormatter.formatUserTime(task.scheduledTime)
            task.deadline != null -> "Due ${TimeFormatter.formatUserTime(task.deadline)}"
            else -> "Anytime"
        }

        val isOverdue = task.urgency == Urgency.OVERDUE && !task.completed

        val accessibilityLabel = buildString {
            append(task.title)
            append(", ")
            append(timeLabel)
            if (task.completed) append(", completed") else append(", incomplete")
            if (isOverdue) append(", overdue")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(rowScale)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isPressed) DayGlassSurface else Color.Transparent)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onTaskClick
                )
                .semantics { contentDescription = accessibilityLabel }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Checkbox
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

                if (task.description.isNotBlank() || isOverdue) {
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
                        if (task.description.isNotBlank()) {
                            Text(
                                text = if (isOverdue) " · ${task.description}" else task.description,
                                color = (if (task.completed) DayTextDisabled else DayTextTertiary).copy(alpha = metaAlpha),
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. Time label on the right
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = timeLabel,
                color = (if (task.completed) DayTextDisabled else DayTextSecondary).copy(alpha = metaAlpha),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )

            // 4. Subtle Priority Indicator Dot
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
}
