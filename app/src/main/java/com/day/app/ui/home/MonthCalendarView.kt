package com.day.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.components.GlassCalendarSelection
import com.day.app.ui.components.GlassPill
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayMotion
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary

@Composable
fun MonthCalendarView(
    monthYearTitle: String,
    calendarDays: List<CalendarDay>,
    isViewingToday: Boolean,
    onDateSelected: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
    modifier: Modifier = Modifier,
    isCollapsed: Boolean = false,
    onToggleCollapse: (() -> Unit)? = null
) {
    var totalDragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (totalDragOffset > 50f) {
                            onPreviousMonth()
                        } else if (totalDragOffset < -50f) {
                            onNextMonth()
                        }
                        totalDragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragOffset += dragAmount
                    }
                )
            }
    ) {
        // Month / Year Header Row with Today Return Button, Collapse Toggle, and Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggleCollapse?.invoke() }
            ) {
                Text(
                    text = monthYearTitle,
                    color = DayTextPrimary,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    letterSpacing = 0.8.sp
                )

                // Return to Today button if viewing another date
                AnimatedVisibility(
                    visible = !isViewingToday,
                    enter = fadeIn(DayMotion.fastTween()),
                    exit = fadeOut(DayMotion.fastTween())
                ) {
                    Row {
                        Spacer(modifier = Modifier.width(10.dp))
                        GlassPill(onClick = onGoToToday) {
                            Text(
                                text = "TODAY",
                                color = DayTextPrimary,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = DayTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = DayTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // When collapsed: smoothly hide the multi-row calendar grid
        androidx.compose.animation.AnimatedVisibility(
            visible = !isCollapsed,
            enter = fadeIn(DayMotion.normalTween()) + androidx.compose.animation.expandVertically(DayMotion.normalTween()),
            exit = fadeOut(DayMotion.fastTween()) + androidx.compose.animation.shrinkVertically(DayMotion.fastTween())
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(4.dp))

                // Weekday header: M T W T F S S
                val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    weekDays.forEach { dayName ->
                        Text(
                            text = dayName,
                            color = DayTextTertiary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Calendar Date Cells Grid (rows of 7)
                val rows = calendarDays.chunked(7)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    rows.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            week.forEach { day ->
                                CalendarDayCell(
                                    day = day,
                                    onSelect = { onDateSelected(day.timestamp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = DayMotion.springTouch(),
        label = "cell_scale"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            day.isSelected -> DayTextPrimary
            day.isToday -> DayTextPrimary
            day.isCurrentMonth -> DayTextSecondary
            else -> DayTextDisabled
        },
        label = "cell_text_color"
    )

    Box(
        modifier = modifier
            .height(38.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelect
            ),
        contentAlignment = Alignment.Center
    ) {
        // Selection capsule or circle background
        if (day.isSelected) {
            GlassCalendarSelection(
                isSelected = true,
                isToday = day.isToday,
                modifier = Modifier.size(32.dp)
            ) {}
        } else if (day.isToday) {
            // Subtle indicator for today when not selected
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${day.dayNumber}",
                color = textColor,
                fontFamily = FontFamily.SansSerif,
                fontWeight = if (day.isSelected || day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            // Event Dots (up to 3 tiny monochrome dots)
            if (day.taskCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotCount = day.taskCount.coerceAtMost(3)
                    for (i in 0 until dotCount) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(
                                    if (day.isSelected) DayTextPrimary else DayTextTertiary
                                )
                        )
                    }
                }
            }
        }
    }
}
