package com.day.app.esp8266.model

enum class EspDisplayMode(val code: String, val displayName: String) {
    CLOCK("CLOCK", "LIVE CLOCK"),
    TASK_PRIORITY("TASK_PRIORITY", "TASK MATRIX"),
    REMINDER("REMINDER", "URGENT ALERT"),
    COMPLETED("COMPLETED", "TASK COMPLETE"),
    IDLE("IDLE", "STANDBY IDLE"),
    CLEAR("CLEAR", "CLEAR DISPLAY");

    companion object {
        fun fromCode(code: String): EspDisplayMode {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: CLOCK
        }
    }
}
