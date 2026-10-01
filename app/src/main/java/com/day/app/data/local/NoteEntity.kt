package com.day.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.day.app.domain.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String>,
    val pinned: Boolean,
    val attachedDocumentIds: List<Long>
) {
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        body = body,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tags = tags,
        pinned = pinned,
        attachedDocumentIds = attachedDocumentIds
    )

    companion object {
        fun fromDomain(note: Note): NoteEntity = NoteEntity(
            id = note.id,
            title = note.title,
            body = note.body,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            tags = note.tags,
            pinned = note.pinned,
            attachedDocumentIds = note.attachedDocumentIds
        )
    }
}
