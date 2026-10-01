package com.day.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * THE DAY — Centralized 12-Hour User Time Formatter
 *
 * All user-facing clock and task times across the application must use
 * 12-hour format with AM / PM and NO leading zero for hours (h:mm a).
 *
 * Examples:
 * 1:05 AM
 * 8:30 AM
 * 12:00 PM
 * 3:45 PM
 * 11:59 PM
 *
 * NOTE: Internal scheduling, Room timestamps, sorting and AlarmManager
 * logic remain intact with standard epoch / 24-hour values.
 */
object TimeFormatter {

    /**
     * Format a millisecond timestamp to "h:mm a" (e.g. "6:30 PM", "9:05 AM", "12:00 PM").
     */
    fun formatUserTime(millis: Long): String {
        return SimpleDateFormat("h:mm a", Locale.US).format(Date(millis)).uppercase(Locale.US)
    }

    /**
     * Format a Date object to "h:mm a".
     */
    fun formatUserTime(date: Date): String {
        return SimpleDateFormat("h:mm a", Locale.US).format(date).uppercase(Locale.US)
    }

    /**
     * Format hour (0-23) and minute (0-59) to "h:mm a" with no leading zero for hours.
     * Boundary examples:
     * (0, 0)   -> "12:00 AM" (midnight)
     * (0, 5)   -> "12:05 AM"
     * (1, 30)  -> "1:30 AM"
     * (11, 59) -> "11:59 AM"
     * (12, 0)  -> "12:00 PM" (noon)
     * (12, 1)  -> "12:01 PM"
     * (13, 0)  -> "1:00 PM"
     * (18, 30) -> "6:30 PM"
     * (23, 59) -> "11:59 PM"
     */
    fun formatUserTime(hour: Int, minute: Int): String {
        val h12 = when (hour % 12) {
            0 -> 12
            else -> hour % 12
        }
        val ampm = if (hour < 12) "AM" else "PM"
        return String.format(Locale.US, "%d:%02d %s", h12, minute, ampm)
    }

    /**
     * Format timestamp with seconds for running technical/status clocks: "h:mm:ss a" (e.g. "6:42:15 PM").
     */
    fun formatUserTimeWithSeconds(millis: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("h:mm:ss a", Locale.US).format(Date(millis)).uppercase(Locale.US)
    }

    /**
     * Format timestamp to date with time: "MMM dd, h:mm a" (e.g. "Sep 28, 6:30 PM").
     */
    fun formatDateTime(millis: Long): String {
        return SimpleDateFormat("MMM dd, h:mm a", Locale.US).format(Date(millis)).uppercase(Locale.US)
    }

    /**
     * Format timestamp to full date with time: "MMM dd, yyyy · h:mm a".
     */
    fun formatDateWithTime(millis: Long): String {
        return SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.US).format(Date(millis)).uppercase(Locale.US)
    }

    /**
     * Format deadline for task items: "d MMM, h:mm a" (e.g. "28 Sep, 6:30 PM").
     */
    fun formatDeadline(millis: Long): String {
        return SimpleDateFormat("d MMM, h:mm a", Locale.US).format(Date(millis)).uppercase(Locale.US)
    }

    /**
     * Return separate components for high-polish running clocks:
     * first = "6:42" (primary time without leading zero)
     * second = "PM" (subordinate AM/PM label)
     */
    fun getClockComponents(date: Date = Date()): Pair<String, String> {
        val timeStr = SimpleDateFormat("h:mm", Locale.US).format(date)
        val ampmStr = SimpleDateFormat("a", Locale.US).format(date).uppercase(Locale.US)
        return Pair(timeStr, ampmStr)
    }
}
