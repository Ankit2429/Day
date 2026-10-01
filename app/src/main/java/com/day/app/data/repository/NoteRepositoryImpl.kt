package com.day.app.data.repository

import com.day.app.data.local.NoteDao
import com.day.app.data.local.NoteEntity
import com.day.app.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllNotesSync(): List<Note> {
        return noteDao.getAllNotesSync().map { it.toDomain() }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query).map { list -> list.map { it.toDomain() } }
    }

    override fun getNoteById(id: Long): Flow<Note?> {
        return noteDao.getNoteById(id).map { it?.toDomain() }
    }

    override suspend fun getNoteByIdSync(id: Long): Note? {
        return noteDao.getNoteByIdSync(id)?.toDomain()
    }

    override fun getNoteCount(): Flow<Int> = noteDao.getNoteCount()

    override suspend fun getNoteCountSync(): Int = noteDao.getNoteCountSync()

    override suspend fun saveNote(note: Note): Long {
        val entity = NoteEntity.fromDomain(note.copy(updatedAt = System.currentTimeMillis()))
        return if (note.id == 0L) {
            noteDao.insertNote(entity)
        } else {
            noteDao.updateNote(entity)
            note.id
        }
    }

    override suspend fun deleteNote(id: Long) {
        noteDao.deleteNoteById(id)
    }

    override suspend fun togglePin(id: Long) {
        val existing = noteDao.getNoteByIdSync(id) ?: return
        val updated = existing.copy(pinned = !existing.pinned, updatedAt = System.currentTimeMillis())
        noteDao.updateNote(updated)
    }
}
