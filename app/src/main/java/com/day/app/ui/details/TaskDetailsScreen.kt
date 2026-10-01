package com.day.app.ui.details

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.domain.model.Priority
import com.day.app.domain.model.Urgency
import com.day.app.ui.components.NeoAlertBanner
import com.day.app.ui.components.NeoAlertVariant
import com.day.app.ui.components.NeoBadge
import com.day.app.ui.components.NeoBadgeVariant
import com.day.app.ui.components.NeoButton
import com.day.app.ui.components.NeoButtonVariant
import com.day.app.ui.components.NeoCard
import com.day.app.ui.components.NeoHeader
import com.day.app.ui.components.NeoProgressBar
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayDestructive
import com.day.app.ui.theme.DayDivider
import com.day.app.ui.theme.DaySurface
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary
import com.day.app.ui.theme.DayWarning

@Composable
fun TaskDetailsScreen(
    taskId: Long,
    viewModel: TaskDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val highlightAlpha = remember { Animatable(0.5f) }
    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
        highlightAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1800)
        )
    }

    val task = state.task

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DayBackground)
    ) {
        NeoHeader(
            title = "Task Details",
            onBackClick = onNavigateBack,
            actionButton = {
                if (task != null) {
                    NeoButton(
                        text = "Edit",
                        onClick = { onNavigateToEdit(task.id) },
                        variant = NeoButtonVariant.OUTLINE_SECONDARY,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 6.dp
                        )
                    )
                }
            }
        )

        if (task == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Task not found",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = DayTextSecondary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                state.hardwareSyncMessage?.let { msg ->
                    NeoAlertBanner(
                        title = "ESP8266 Sync",
                        message = msg,
                        variant = if (msg.contains("FLASHED")) NeoAlertVariant.SUCCESS else NeoAlertVariant.WARNING,
                        onDismiss = { viewModel.clearHardwareMessage() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Core Info Card
                NeoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            androidx.compose.ui.graphics.Color.White.copy(alpha = highlightAlpha.value),
                            RoundedCornerShape(16.dp)
                        ),
                    backgroundColor = DaySurfaceElevated,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NeoBadge(
                                text = task.priority.name,
                                variant = when (task.priority) {
                                    Priority.HIGH -> NeoBadgeVariant.WHITE
                                    Priority.MEDIUM -> NeoBadgeVariant.BLACK
                                    Priority.LOW -> NeoBadgeVariant.BLACK
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            NeoBadge(
                                text = if (task.completed) "Completed" else task.urgency.name,
                                variant = if (task.urgency == Urgency.OVERDUE) NeoBadgeVariant.RED else NeoBadgeVariant.BLACK
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = task.title,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = DayTextPrimary
                        )

                        if (task.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = task.description,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = DayTextSecondary
                            )
                        }

                        if (task.deadline != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "DEADLINE PROGRESS",
                                color = DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            NeoProgressBar(
                                progress = task.deadlineProgress(),
                                height = 6.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = DayDivider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow("Scheduled", task.formattedScheduledDate()?.let { "$it at ${task.formattedScheduledTime()}" } ?: "None")
                        DetailRow("Deadline", task.formattedDeadline() ?: "None")
                        DetailRow("Repeat", task.repeatType.label)
                        DetailRow("Reminder", task.reminderInterval?.let { "Every $it mins" } ?: if (task.reminderEnabled) "Active" else "Off")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                NeoButton(
                    text = if (task.completed) "Mark as Incomplete" else "Mark as Completed",
                    onClick = { viewModel.toggleComplete(!task.completed) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = if (task.completed) NeoButtonVariant.OUTLINE_SECONDARY else NeoButtonVariant.FILLED_PRIMARY
                )

                Spacer(modifier = Modifier.height(10.dp))

                NeoButton(
                    text = "Push to ESP8266 Matrix",
                    onClick = { viewModel.testPushToEspMatrix() },
                    modifier = Modifier.fillMaxWidth(),
                    variant = NeoButtonVariant.OUTLINE_SECONDARY
                )

                Spacer(modifier = Modifier.height(10.dp))

                NeoButton(
                    text = "Delete Task",
                    onClick = { viewModel.deleteTask(onSuccess = onNavigateBack) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = NeoButtonVariant.DESTRUCTIVE
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = DayTextTertiary,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = DayTextPrimary
        )
    }
}
