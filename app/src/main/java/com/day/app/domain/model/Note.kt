package com.day.app.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Note(
    val id: Long = 0L,
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val attachedDocumentIds: List<Long> = emptyList()
) {
    fun formattedDate(): String {
        return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(updatedAt))
    }

    fun snippet(maxLength: Int = 120): String {
        val clean = body.trim()
        return if (clean.length > maxLength) {
            clean.substring(0, maxLength) + "..."
        } else {
            clean
        }
    }
}
