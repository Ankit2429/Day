package com.day.app.domain.model

enum class RepeatType(val label: String) {
    NONE("DOESN'T REPEAT"),
    DAILY("DAILY"),
    WEEKDAYS("WEEKDAYS"),
    WEEKLY("WEEKLY"),
    CUSTOM("CUSTOM");

    companion object {
        fun fromString(value: String?): RepeatType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}
