package com.day.app.domain.model

enum class Urgency(
    val label: String,
    val iconSymbol: String,
    val rank: Int
) {
    NORMAL("NORMAL", "○", 1),
    IMPORTANT("IMPORTANT", "◈", 2),
    URGENT("URGENT", "▲", 3),
    OVERDUE("OVERDUE", "⚠", 4);

    companion object {
        /**
         * Calculates the derived urgency based on priority, scheduled time, deadline, and completion state.
         *
         * @param priority Task priority (LOW, MEDIUM, HIGH)
         * @param scheduledTime Time task is scheduled for
         * @param deadline Optional deadline timestamp
         * @param isCompleted True if task is completed
         * @param now Current epoch timestamp
         */
        fun calculate(
            priority: Priority,
            scheduledTime: Long?,
            deadline: Long?,
            isCompleted: Boolean,
            now: Long = System.currentTimeMillis()
        ): Urgency {
            if (isCompleted) return NORMAL

            // 1. Check if overdue
            if (deadline != null && now > deadline) {
                return OVERDUE
            }
            if (deadline == null && scheduledTime != null && now > (scheduledTime + 3600000L)) {
                // If scheduled time passed by more than an hour without completion
                return OVERDUE
            }

            // 2. Deadline approaching checks
            if (deadline != null) {
                val millisRemaining = deadline - now
                val hoursRemaining = millisRemaining / (1000 * 60 * 60.0)

                if (hoursRemaining <= 2.0) {
                    return URGENT
                }
                if (hoursRemaining <= 8.0) {
                    return if (priority == Priority.HIGH) URGENT else IMPORTANT
                }
                if (hoursRemaining <= 24.0) {
                    return when (priority) {
                        Priority.HIGH -> URGENT
                        Priority.MEDIUM -> IMPORTANT
                        Priority.LOW -> NORMAL
                    }
                }
            }

            // 3. Fallback to priority and scheduled time proximity
            if (scheduledTime != null) {
                val timeToSchedule = scheduledTime - now
                if (timeToSchedule in 0..1800000L) { // within 30 mins
                    return if (priority == Priority.LOW) IMPORTANT else URGENT
                }
            }

            return when (priority) {
                Priority.HIGH -> IMPORTANT
                Priority.MEDIUM -> NORMAL
                Priority.LOW -> NORMAL
            }
        }
    }
}
