package com.day.app.domain.model

enum class Priority(val label: String, val level: Int) {
    LOW("LOW", 1),
    MEDIUM("MEDIUM", 2),
    HIGH("HIGH", 3);

    companion object {
        fun fromString(value: String?): Priority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
