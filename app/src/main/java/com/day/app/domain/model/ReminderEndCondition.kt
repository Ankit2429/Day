package com.day.app.domain.model

enum class ReminderEndCondition(val label: String) {
    COMPLETED("UNTIL COMPLETED"),
    DEADLINE("UNTIL DEADLINE"),
    CUSTOM("CUSTOM LIMIT");

    companion object {
        fun fromString(value: String?): ReminderEndCondition {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: COMPLETED
        }
    }
}
