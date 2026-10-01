package com.day.app.domain.model

enum class CacheStatus(val label: String) {
    CACHED("CACHED LOCALLY"),
    REMOTE_ONLY("REMOTE (TELEGRAM)"),
    DOWNLOADING("DOWNLOADING..."),
    UPLOADING("UPLOADING..."),
    ERROR("SYNC ERROR");

    companion object {
        fun fromString(value: String?): CacheStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CACHED
        }
    }
}
