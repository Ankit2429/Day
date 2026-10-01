package com.day.app.esp8266.model

enum class EspTaskUrgency(val wireValue: String) {
    NORMAL("NORMAL"),
    APPROACHING("APPROACHING"),
    DUE_NOW("DUE_NOW"),
    OVERDUE("OVERDUE"),
    IGNORED("IGNORED");

    companion object {
        fun calculate(
            scheduledTime: Long?,
            deadline: Long?,
            reminderFiredTime: Long? = null,
            isCompleted: Boolean = false,
            now: Long = System.currentTimeMillis()
        ): EspTaskUrgency {
            if (isCompleted) return NORMAL

            val targetTime = deadline ?: scheduledTime
            if (targetTime != null && targetTime > 0L) {
                val diff = targetTime - now
                if (diff < -15 * 60 * 1000L) {
                    return if (reminderFiredTime != null && now - reminderFiredTime > 15 * 60 * 1000L) {
                        IGNORED
                    } else {
                        OVERDUE
                    }
                }
                if (diff < 0L) {
                    return OVERDUE
                }
                if (diff <= 5 * 60 * 1000L) {
                    return DUE_NOW
                }
                if (diff <= 30 * 60 * 1000L) {
                    return APPROACHING
                }
            }
            return NORMAL
        }
    }
}
