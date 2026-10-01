package com.day.app.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class Task(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val scheduledTime: Long? = null,
    val deadline: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatRule: String? = null,
    val reminderEnabled: Boolean = true,
    val reminderInterval: Long? = null, // in minutes
    val reminderEndCondition: ReminderEndCondition = ReminderEndCondition.COMPLETED,
    val linkedDocumentId: Long? = null,
    val linkedNoteId: Long? = null
) {
    val urgency: Urgency
        get() = Urgency.calculate(priority, scheduledTime, deadline, completed)

    /**
     * Percentage of time elapsed toward deadline (0.0 to 1.0)
     */
    fun deadlineProgress(now: Long = System.currentTimeMillis()): Float {
        if (completed) return 1f
        if (deadline == null) return 0f
        val start = scheduledTime ?: createdAt
        if (deadline <= start) return 1f
        if (now <= start) return 0f
        if (now >= deadline) return 1f
        return ((now - start).toFloat() / (deadline - start).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Visual 8-block representation of deadline urgency for Neo-Brutalist & MAX7219 mapping
     * Concept from specs:
     * 12 hours remaining: ██
     * 6 hours remaining:  ████
     * 2 hours remaining:  ██████
     * 30 mins remaining:  ████████
     * Overdue:            ALERT / 8 full blocks flashing
     */
    fun urgencyBlocks(now: Long = System.currentTimeMillis()): String {
        if (completed) return "COMPLETED"
        if (deadline == null) {
            return when (priority) {
                Priority.LOW -> "██"
                Priority.MEDIUM -> "████"
                Priority.HIGH -> "██████"
            }
        }
        val remainingMillis = deadline - now
        if (remainingMillis <= 0) return "ALERT"

        val hoursRemaining = remainingMillis / (1000 * 60 * 60.0)
        return when {
            hoursRemaining <= 0.5 -> "████████" // 8 blocks
            hoursRemaining <= 2.0 -> "██████"   // 6 blocks
            hoursRemaining <= 6.0 -> "████"     // 4 blocks
            hoursRemaining <= 12.0 -> "██"      // 2 blocks
            else -> "█"
        }
    }

    /**
     * Number of LEDs to light on an 8x8 matrix row (1 to 8)
     */
    fun matrixLedCount(now: Long = System.currentTimeMillis()): Int {
        if (completed) return 0
        if (deadline == null) {
            return when (priority) {
                Priority.LOW -> 2
                Priority.MEDIUM -> 4
                Priority.HIGH -> 7
            }
        }
        val remainingMillis = deadline - now
        if (remainingMillis <= 0) return 8
        val hoursRemaining = remainingMillis / (1000 * 60 * 60.0)
        return when {
            hoursRemaining <= 0.5 -> 8
            hoursRemaining <= 2.0 -> 6
            hoursRemaining <= 6.0 -> 5
            hoursRemaining <= 12.0 -> 3
            hoursRemaining <= 24.0 -> 2
            else -> 1
        }
    }

    fun formattedScheduledTime(): String? {
        return scheduledTime?.let {
            com.day.app.util.TimeFormatter.formatUserTime(it)
        }
    }

    fun formattedScheduledDate(): String? {
        return scheduledTime?.let {
            SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(it))
        }
    }

    fun formattedDeadline(): String? {
        return deadline?.let {
            com.day.app.util.TimeFormatter.formatDeadline(it)
        }
    }

    fun formattedRemainingTime(now: Long = System.currentTimeMillis()): String? {
        val target = deadline ?: scheduledTime ?: return null
        val diff = target - now
        val isOverdue = diff < 0
        val absDiff = kotlin.math.abs(diff)

        val hours = TimeUnit.MILLISECONDS.toHours(absDiff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(absDiff) % 60

        val timeString = when {
            hours > 24 -> "${hours / 24}d ${hours % 24}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }

        return if (isOverdue) "OVERDUE BY $timeString" else "$timeString REMAINING"
    }
}
