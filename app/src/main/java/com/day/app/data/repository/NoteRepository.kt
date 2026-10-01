package com.day.app.data.repository

import com.day.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    suspend fun getAllNotesSync(): List<Note>
    fun searchNotes(query: String): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    suspend fun getNoteByIdSync(id: Long): Note?
    fun getNoteCount(): Flow<Int>
    suspend fun getNoteCountSync(): Int
    suspend fun saveNote(note: Note): Long
    suspend fun deleteNote(id: Long)
    suspend fun togglePin(id: Long)
}
