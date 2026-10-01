package com.day.app.ui.taskeditor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.domain.model.Priority
import com.day.app.domain.model.RepeatType
import com.day.app.ui.components.NeoAlertBanner
import com.day.app.ui.components.NeoAlertVariant
import com.day.app.ui.components.NeoButton
import com.day.app.ui.components.NeoButtonVariant
import com.day.app.ui.components.NeoCard
import com.day.app.ui.components.NeoHeader
import com.day.app.ui.components.NeoInput
import com.day.app.ui.components.NeoToggle
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayBackgroundDark
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DayDestructive
import com.day.app.ui.theme.DaySurface
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DaySurfaceHighlight
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.day.app.util.TimeFormatter

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.text.style.TextAlign
import com.day.app.ui.components.AmbientGlassBackground
import com.day.app.ui.components.GlassButton
import com.day.app.ui.components.GlassPill

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(
    taskId: Long = 0L,
    viewModel: TaskEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(taskId) {
        if (taskId > 0L) {
            viewModel.loadTask(taskId)
        }
    }

    AmbientGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header: Cancel, Title, Create/Save Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cancel",
                    color = DayTextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigateBack
                    )
                )

                Text(
                    text = if (state.isEditMode) "EDIT TASK" else "NEW TASK",
                    color = DayTextPrimary,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.2.sp
                )

                GlassButton(
                    text = if (state.isEditMode) "Save" else "Create",
                    onClick = { viewModel.saveTask(onSuccess = onNavigateBack) },
                    isPrimary = true,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 7.dp
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                state.errorMessage?.let { error ->
                    NeoAlertBanner(
                        title = "Error",
                        message = error,
                        variant = NeoAlertVariant.ERROR,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 1. Title Input
                NeoInput(
                    value = state.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = "WHAT DO YOU NEED TO DO?",
                    placeholder = "Task title or action...",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Description Input
                NeoInput(
                    value = state.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = "DESCRIPTION",
                    placeholder = "Add notes, subtasks, or context...",
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 3. Quick Time Suggestions & When Picker
                Text(
                    text = "WHEN?",
                    color = DayTextTertiary,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickTimes = listOf(
                        "NOW" to { System.currentTimeMillis() },
                        "+30 MIN" to { System.currentTimeMillis() + 30 * 60 * 1000 },
                        "+1 HR" to { System.currentTimeMillis() + 60 * 60 * 1000 },
                        "TONIGHT" to {
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 20)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }.timeInMillis
                        }
                    )

                    quickTimes.forEach { (label, timeSupplier) ->
                        GlassPill(
                            onClick = {
                                viewModel.updateScheduledTime(timeSupplier())
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                color = DayTextPrimary,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val scheduledFormatted = state.scheduledTime?.let {
                        TimeFormatter.formatDateTime(it)
                    } ?: "Choose date & time..."

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DaySurfaceElevated)
                            .border(1.dp, DayBorderSubtle, RoundedCornerShape(14.dp))
                            .clickable {
                                val c = Calendar.getInstance().apply {
                                    state.scheduledTime?.let { timeInMillis = it }
                                }
                                DatePickerDialog(context, { _, y, m, d ->
                                    TimePickerDialog(context, { _, hour, min ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(y, m, d, hour, min, 0)
                                        }
                                        viewModel.updateScheduledTime(newCal.timeInMillis)
                                    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Event,
                                contentDescription = null,
                                tint = if (state.scheduledTime != null) DayTextPrimary else DayTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = scheduledFormatted,
                                color = if (state.scheduledTime != null) DayTextPrimary else DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (state.scheduledTime != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(DaySurfaceElevated, CircleShape)
                                .border(1.dp, DayBorderSubtle, CircleShape)
                                .clickable { viewModel.updateScheduledTime(null) }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = DayTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Priority Selector (Sleek pill segmented control)
                Text(
                    text = "PRIORITY",
                    color = DayTextTertiary,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.entries.forEach { p ->
                        val isSelected = state.priority == p
                        val pillShape = RoundedCornerShape(999.dp)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(pillShape)
                                .background(if (isSelected) DayTextPrimary else DaySurface, pillShape)
                                .border(0.5.dp, if (isSelected) DayTextPrimary else DayBorderSubtle, pillShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { viewModel.updatePriority(p) }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) DayBackgroundDark else DayTextSecondary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Deadline Section
            Text(
                text = "DEADLINE",
                color = DayTextTertiary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val deadlineFormatted = state.deadline?.let {
                    TimeFormatter.formatDateTime(it)
                } ?: "No deadline"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DaySurface)
                        .border(0.5.dp, DayBorderSubtle, RoundedCornerShape(12.dp))
                        .clickable {
                            val c = Calendar.getInstance().apply {
                                state.deadline?.let { timeInMillis = it }
                            }
                            DatePickerDialog(context, { _, y, m, d ->
                                TimePickerDialog(context, { _, hour, min ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, hour, min, 0)
                                    }
                                    viewModel.updateDeadline(newCal.timeInMillis)
                                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = if (state.deadline != null) DayTextPrimary else DayTextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = deadlineFormatted,
                            color = if (state.deadline != null) DayTextPrimary else DayTextTertiary,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp
                        )
                    }
                }

                if (state.deadline != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DaySurface, CircleShape)
                            .border(0.5.dp, DayBorderSubtle, CircleShape)
                            .clickable { viewModel.updateDeadline(null) }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = DayTextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Reminder Switch
            Text(
                text = "REMINDER",
                color = DayTextTertiary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DaySurface)
                    .border(0.5.dp, DayBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Notification Alert",
                        color = DayTextPrimary,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Push notification when due",
                        color = DayTextTertiary,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp
                    )
                }

                NeoToggle(
                    checked = state.reminderEnabled,
                    onCheckedChange = { viewModel.updateReminderEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 7. Large Primary Action: Save Task
            NeoButton(
                text = if (state.isEditMode) "Save Changes" else "Create Task",
                onClick = { viewModel.saveTask(onSuccess = onNavigateBack) },
                modifier = Modifier.fillMaxWidth(),
                variant = NeoButtonVariant.FILLED_PRIMARY,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
}
