package com.day.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.day.app.domain.model.Priority
import com.day.app.domain.model.Task
import com.day.app.domain.model.Urgency
import com.day.app.ui.components.GlassActiveTask
import com.day.app.ui.components.NeoCheckbox
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayDivider
import com.day.app.ui.theme.DayMotion
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary

@Composable
fun DailyTimelineView(
    entries: List<TimelineEntry>,
    onTaskClick: (Long) -> Unit,
    onCompleteToggle: (Long, Boolean) -> Unit,
    onTimelineSlotLongClick: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteTask: ((Long) -> Unit)? = null
) {
    if (entries.isEmpty() || entries.all { it is TimelineEntry.CurrentTimeMarker }) {
        TimelineEmptyState()
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            entries.forEachIndexed { index, entry ->
                when (entry) {
                    is TimelineEntry.CurrentTimeMarker -> {
                        CurrentTimeMarkerRow(entry = entry)
                    }
                    is TimelineEntry.TaskEntry -> {
                        TimelineTaskRow(
                            entry = entry,
                            isLast = index == entries.lastIndex,
                            onTaskClick = { onTaskClick(entry.task.id) },
                            onToggle = { checked -> onCompleteToggle(entry.task.id, checked) },
                            onDelete = { onDeleteTask?.invoke(entry.task.id) },
                            onLongClick = {
                                val hr = entry.minutesFromMidnight / 60
                                val min = entry.minutesFromMidnight % 60
                                onTimelineSlotLongClick(hr, min)
                            }
                        )
                    }
                    is TimelineEntry.FreeTimeSlot -> {
                        // Empty slot
                    }
                }
            }
        }
    }
}

/**
 * Real-time Current-Time Indicator Line (e.g. 17:35 ──────────)
 */
@Composable
private fun CurrentTimeMarkerRow(entry: TimelineEntry.CurrentTimeMarker) {
    val infiniteTransition = rememberInfiniteTransition(label = "now_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time label
        Text(
            text = entry.timeLabel,
            color = Color.White.copy(alpha = pulseAlpha),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            modifier = Modifier.width(68.dp)
        )

        // Pulsing white dot
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = pulseAlpha))
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Horizontal line extending across width
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.25f),
            thickness = 0.5.dp,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual Timeline Task Row with Vertical Timeline Rail Node
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineTaskRow(
    entry: TimelineEntry.TaskEntry,
    isLast: Boolean,
    onTaskClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit
) {
    val task = entry.task
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val coroutineScope = rememberCoroutineScope()
    val swipeOffsetX = remember { Animatable(0f) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        animationSpec = DayMotion.springTouch(),
        label = "row_scale"
    )

    val textColor by animateColorAsState(
        targetValue = if (task.completed) DayTextDisabled else DayTextPrimary,
        animationSpec = DayMotion.normalTween(),
        label = "row_text_color"
    )

    val alpha by animateFloatAsState(
        targetValue = if (task.completed) 0.40f else if (entry.isPast) 0.60f else 1.0f,
        animationSpec = DayMotion.normalTween(),
        label = "row_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTaskClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Time Label on the left
        Text(
            text = entry.timeLabel,
            color = if (task.completed) DayTextDisabled else DayTextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.width(68.dp)
        )

        // 2. Timeline Vertical Rail & Node
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Vertical continuous rail
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(DayDivider)
                )
            }

            // Node Circle
            if (entry.isCurrent) {
                // Active task glowing node
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (task.completed) DayTextDisabled else DayBackground)
                        .border(1.dp, if (task.completed) DayTextDisabled else DayTextTertiary, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 3. Task Content with Horizontal Swipe Actions:
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            // Background Action Revealed on Drag Right: Complete/Uncomplete
            if (swipeOffsetX.value > 12f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(start = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Complete",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (task.completed) "Undo" else "Done",
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Background Actions Revealed on Drag Left: Edit and Delete
            if (swipeOffsetX.value < -12f) {
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable {
                                coroutineScope.launch { swipeOffsetX.animateTo(0f) }
                                onTaskClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = DayTextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable {
                                coroutineScope.launch { swipeOffsetX.animateTo(0f) }
                                onDelete()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = DayTextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Foreground Task Row with Drag gestures:
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(swipeOffsetX.value.roundToInt(), 0) }
                    .pointerInput(task.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (swipeOffsetX.value > 75f) {
                                        onToggle(!task.completed)
                                        swipeOffsetX.animateTo(0f, spring(dampingRatio = 0.8f))
                                    } else if (swipeOffsetX.value < -65f) {
                                        swipeOffsetX.animateTo(-86f, spring(dampingRatio = 0.8f))
                                    } else {
                                        swipeOffsetX.animateTo(0f, spring(dampingRatio = 0.8f))
                                    }
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                coroutineScope.launch {
                                    val newOffset = (swipeOffsetX.value + dragAmount).coerceIn(-95f, 95f)
                                    swipeOffsetX.snapTo(newOffset)
                                }
                            }
                        )
                    }
            ) {
                if (entry.isCurrent) {
                    GlassActiveTask(
                        onClick = onTaskClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeoCheckbox(
                                checked = task.completed,
                                onCheckedChange = onToggle,
                                size = 20.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    color = textColor,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.5.sp,
                                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (task.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = task.description,
                                        color = DayTextTertiary,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Details",
                                tint = DayTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeoCheckbox(
                            checked = task.completed,
                            onCheckedChange = onToggle,
                            size = 19.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                color = textColor.copy(alpha = alpha),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.5.sp,
                                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (task.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = task.description,
                                    color = DayTextTertiary.copy(alpha = alpha),
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (task.priority == Priority.HIGH && !task.completed) {
                            Spacer(modifier = Modifier.width(6.dp))
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
        }
    }
}

/**
 * Calm Minimal Empty State for days with no tasks
 */
@Composable
private fun TimelineEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CLEAR DAY",
                color = DayTextSecondary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "No scheduled tasks.",
                color = DayTextTertiary,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp
            )
        }
    }
}
