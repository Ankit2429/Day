package com.day.app.domain.model

enum class DocumentCategory(val label: String) {
    STUDY("STUDY"),
    COLLEGE("COLLEGE"),
    ROBOTICS("ROBOTICS"),
    PROJECTS("PROJECTS"),
    PERSONAL("PERSONAL"),
    DOCUMENTS("DOCUMENTS"),
    IMAGES("IMAGES"),
    OTHER("OTHER");

    companion object {
        fun fromString(value: String?): DocumentCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
