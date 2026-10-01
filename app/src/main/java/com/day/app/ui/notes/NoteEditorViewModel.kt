package com.day.app.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.day.app.DayApplication
import com.day.app.domain.model.Document
import com.day.app.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val noteId: Long = 0L,
    val title: String = "",
    val body: String = "",
    val tagsString: String = "",
    val pinned: Boolean = false,
    val attachedDocumentIds: List<Long> = emptyList(),
    val availableDocuments: List<Document> = emptyList(),
    val isEditMode: Boolean = false,
    val errorMessage: String? = null
)

class NoteEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayApplication
    private val noteRepository = app.noteRepository
    private val documentRepository = app.documentRepository

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val docs = documentRepository.getAllDocumentsSync()
            _uiState.value = _uiState.value.copy(availableDocuments = docs)
        }
    }

    fun loadNote(noteId: Long) {
        if (noteId <= 0L) return
        viewModelScope.launch {
            val note = noteRepository.getNoteByIdSync(noteId)
            if (note != null) {
                _uiState.value = _uiState.value.copy(
                    noteId = note.id,
                    title = note.title,
                    body = note.body,
                    tagsString = note.tags.joinToString(", "),
                    pinned = note.pinned,
                    attachedDocumentIds = note.attachedDocumentIds,
                    isEditMode = true
                )
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title, errorMessage = null)
    }

    fun updateBody(body: String) {
        _uiState.value = _uiState.value.copy(body = body)
    }

    fun updateTags(tags: String) {
        _uiState.value = _uiState.value.copy(tagsString = tags)
    }

    fun togglePin(pinned: Boolean) {
        _uiState.value = _uiState.value.copy(pinned = pinned)
    }

    fun toggleAttachDocument(docId: Long) {
        val currentIds = _uiState.value.attachedDocumentIds.toMutableList()
        if (currentIds.contains(docId)) {
            currentIds.remove(docId)
        } else {
            currentIds.add(docId)
        }
        _uiState.value = _uiState.value.copy(attachedDocumentIds = currentIds)
    }

    fun saveNote(onSuccess: () -> Unit) {
        val current = _uiState.value
        val titleTrimmed = current.title.trim()

        if (titleTrimmed.isEmpty() && current.body.trim().isEmpty()) {
            _uiState.value = current.copy(errorMessage = "Note cannot be empty")
            return
        }

        val tagsList = current.tagsString.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        viewModelScope.launch {
            val note = Note(
                id = current.noteId,
                title = if (titleTrimmed.isNotEmpty()) titleTrimmed else "UNTITLED NOTE",
                body = current.body.trim(),
                tags = tagsList,
                pinned = current.pinned,
                attachedDocumentIds = current.attachedDocumentIds
            )
            noteRepository.saveNote(note)
            onSuccess()
        }
    }

    fun deleteNote(onSuccess: () -> Unit) {
        val id = _uiState.value.noteId
        if (id > 0L) {
            viewModelScope.launch {
                noteRepository.deleteNote(id)
                onSuccess()
            }
        }
    }
}
