package com.day.app.ui.home

import com.day.app.domain.model.Task

data class CalendarDay(
    val dayNumber: Int,
    val timestamp: Long,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean,
    val isToday: Boolean,
    val taskCount: Int
)

sealed interface TimelineEntry {
    data class TaskEntry(
        val task: Task,
        val timeLabel: String,
        val minutesFromMidnight: Int,
        val isCurrent: Boolean,
        val isPast: Boolean,
        val isUpcoming: Boolean
    ) : TimelineEntry

    data class CurrentTimeMarker(
        val timeLabel: String,
        val minutesFromMidnight: Int
    ) : TimelineEntry

    data class FreeTimeSlot(
        val timeLabel: String,
        val hour: Int,
        val minute: Int,
        val isPast: Boolean
    ) : TimelineEntry
}
