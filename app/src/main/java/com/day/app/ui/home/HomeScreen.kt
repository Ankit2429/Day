package com.day.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.day.app.util.TimeFormatter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.R
import com.day.app.domain.model.Task
import com.day.app.esp8266.repository.ConnectionStatus
import com.day.app.ui.components.AmbientGlassBackground
import com.day.app.ui.components.DayLogoMark
import com.day.app.ui.components.GlassButton
import com.day.app.ui.components.GlassCard
import com.day.app.ui.components.GlassPill
import com.day.app.ui.theme.DayClockTextStyle
import com.day.app.ui.theme.DayDivider
import com.day.app.ui.theme.DayMotion
import com.day.app.ui.theme.DaySuccess
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddTask: (Long?) -> Unit,
    onNavigateToAddNote: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToTaskDetails: (Long) -> Unit,
    onNavigateToEsp8266: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    var isCalendarManuallyCollapsed by remember { androidx.compose.runtime.mutableStateOf(false) }
    val isScrolledPastHeader by remember {
        androidx.compose.runtime.derivedStateOf {
            lazyListState.firstVisibleItemIndex > 1 || (lazyListState.firstVisibleItemIndex == 1 && lazyListState.firstVisibleItemScrollOffset > 60)
        }
    }
    val isCalendarCollapsed = isCalendarManuallyCollapsed || isScrolledPastHeader

    AmbientGlassBackground {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp)
        ) {
            // 1. Top Header: Minimal Geometric Logo Mark + App Title & Device Status Indicator
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DayLogoMark(size = 22.dp)

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "DAY",
                                color = DayTextPrimary,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.todayDateFormatted.uppercase(Locale.getDefault()),
                                color = DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.5.sp,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quiet, compact ESP32 device indicator (32dp height)
                        val isConnected = state.espConnectionStatus == ConnectionStatus.CONNECTED
                        val isConnecting = state.espConnectionStatus == ConnectionStatus.CONNECTING
                        val isReconnecting = state.espConnectionStatus == ConnectionStatus.RECONNECTING
                        GlassPill(
                            onClick = onNavigateToEsp8266,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dotColor = when {
                                    isConnected -> DaySuccess
                                    isConnecting || isReconnecting -> Color(0xFFE2E8F0).copy(alpha = pulseAlpha)
                                    else -> DayTextTertiary.copy(alpha = 0.5f)
                                }
                                val pillText = when {
                                    isConnected -> "CONNECTED"
                                    isReconnecting -> "RECONNECTING"
                                    isConnecting -> "CONNECTING"
                                    else -> "DISCONNECTED"
                                }
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pillText,
                                    color = if (isConnected) DayTextSecondary else DayTextTertiary,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Compact settings button (32dp)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF141416), CircleShape)
                                .border(0.5.dp, Color(0xFF26262A), CircleShape)
                                .clickable(onClick = onNavigateToSettings),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = DayTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Dominant Running Digital Clock & Greeting
                RealtimeRunningClock(modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(4.dp))

                val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
                val greeting = when {
                    currentHour < 12 -> "Good morning."
                    currentHour < 17 -> "Good afternoon."
                    else -> "Good evening."
                }

                Text(
                    text = greeting,
                    color = DayTextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.5.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Quick Action Glass Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionPill(
                        text = "+ Task",
                        onClick = { onNavigateToAddTask(state.selectedDate) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionPill(
                        text = "+ Note",
                        onClick = onNavigateToAddNote,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionPill(
                        text = "Upload",
                        onClick = onNavigateToDocuments,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Monthly Calendar View with Collapse Support
                MonthCalendarView(
                    monthYearTitle = state.currentMonthYearFormatted,
                    calendarDays = state.calendarDays,
                    isViewingToday = state.isViewingToday,
                    onDateSelected = { timestamp -> viewModel.selectDate(timestamp) },
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onGoToToday = { viewModel.goToToday() },
                    isCollapsed = isCalendarCollapsed,
                    onToggleCollapse = { isCalendarManuallyCollapsed = !isCalendarManuallyCollapsed }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 5. UP NEXT Section (Connected directly to timeline)
                AnimatedContent(
                    targetState = state.upNextTask,
                    transitionSpec = {
                        (fadeIn(DayMotion.fastTween()) + slideInVertically { it / 3 })
                            .togetherWith(fadeOut(DayMotion.fastTween()) + slideOutVertically { -it / 3 })
                    },
                    label = "up_next"
                ) { task ->
                    if (task != null) {
                        Column {
                            Text(
                                text = "UP NEXT",
                                color = DayTextTertiary,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GlassCard(
                                cornerRadius = 14.dp,
                                onClick = { onNavigateToTaskDetails(task.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            color = DayTextPrimary,
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 14.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val dueText = when {
                                            task.scheduledTime != null -> "Scheduled · ${TimeFormatter.formatUserTime(task.scheduledTime)}"
                                            task.deadline != null -> "Due · ${TimeFormatter.formatUserTime(task.deadline)}"
                                            else -> "Today"
                                        }
                                        Text(
                                            text = dueText,
                                            color = DayTextTertiary,
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 11.5.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = DayTextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(22.dp))
                        }
                    }
                }

                // 6. Agenda / Timeline Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.selectedDateFormattedTitle,
                        color = DayTextTertiary,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp
                    )

                    GlassButton(
                        text = "+ Add",
                        onClick = { onNavigateToAddTask(state.selectedDate) },
                        isPrimary = false
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // 7. Daily Timeline Agenda View
            item {
                DailyTimelineView(
                    entries = state.timelineEntries,
                    onTaskClick = { taskId -> onNavigateToTaskDetails(taskId) },
                    onCompleteToggle = { taskId, checked -> viewModel.toggleTaskComplete(taskId, checked) },
                    onDeleteTask = { taskId -> viewModel.deleteTask(taskId) },
                    onTimelineSlotLongClick = { hour, minute ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = state.selectedDate
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                        }
                        onNavigateToAddTask(cal.timeInMillis)
                    }
                )
            }

            // Extra bottom spacing to scroll well above the floating navigation bar
            item {
                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPill(
        onClick = onClick,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = DayTextSecondary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun RealtimeRunningClock(modifier: Modifier = Modifier) {
    var clockComponents by remember {
        mutableStateOf(TimeFormatter.getClockComponents(java.util.Date()))
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            val updated = TimeFormatter.getClockComponents(java.util.Date())
            if (updated != clockComponents) {
                clockComponents = updated
            }
            delay(1000)
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            clockComponents.first.forEachIndexed { index, char ->
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        (slideInVertically { height -> height / 3 } + fadeIn(tween(250)))
                            .togetherWith(slideOutVertically { height -> -height / 3 } + fadeOut(tween(250)))
                    },
                    label = "clockDigit_$index"
                ) { targetChar ->
                    Text(
                        text = targetChar.toString(),
                        style = DayClockTextStyle.copy(fontSize = 48.sp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = clockComponents.second,
            color = DayTextTertiary,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}
