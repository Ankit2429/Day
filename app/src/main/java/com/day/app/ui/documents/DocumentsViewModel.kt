package com.day.app.ui.documents

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.day.app.DayApplication
import com.day.app.domain.model.Document
import com.day.app.domain.model.DocumentCategory
import com.day.app.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DocumentTab {
    FILES,
    NOTES
}

data class DocumentsUiState(
    val currentTab: DocumentTab = DocumentTab.FILES,
    val documents: List<Document> = emptyList(),
    val notes: List<Note> = emptyList(),
    val selectedCategory: DocumentCategory? = null,
    val searchQuery: String = "",
    val fileCount: Int = 0,
    val noteCount: Int = 0,
    val uploadProgress: Float? = null, // null when idle, 0.0 to 1.0 when uploading
    val uploadFileName: String? = null,
    val errorMessage: String? = null
)

class DocumentsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayApplication
    private val documentRepository = app.documentRepository
    private val noteRepository = app.noteRepository

    private val _currentTab = MutableStateFlow(DocumentTab.FILES)
    private val _selectedCategory = MutableStateFlow<DocumentCategory?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _uploadProgress = MutableStateFlow<Float?>(null)
    private val _uploadFileName = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DocumentsUiState> = combine(
        documentRepository.getAllDocuments(),
        noteRepository.getAllNotes(),
        _currentTab,
        _selectedCategory,
        _searchQuery,
        _uploadProgress,
        _uploadFileName,
        _errorMessage
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val docs = args[0] as List<Document>
        @Suppress("UNCHECKED_CAST")
        val notes = args[1] as List<Note>
        val tab = args[2] as DocumentTab
        val category = args[3] as DocumentCategory?
        val query = args[4] as String
        val progress = args[5] as Float?
        val uploadName = args[6] as String?
        val error = args[7] as String?

        val filteredDocs = docs.filter { doc ->
            (category == null || doc.category == category) &&
                    (query.isBlank() || doc.fileName.contains(query, ignoreCase = true) ||
                            doc.displayName.contains(query, ignoreCase = true) ||
                            doc.subject?.contains(query, ignoreCase = true) == true ||
                            doc.tags.any { it.contains(query, ignoreCase = true) })
        }

        val filteredNotes = notes.filter { note ->
            query.isBlank() || note.title.contains(query, ignoreCase = true) ||
                    note.body.contains(query, ignoreCase = true) ||
                    note.tags.any { it.contains(query, ignoreCase = true) }
        }

        DocumentsUiState(
            currentTab = tab,
            documents = filteredDocs,
            notes = filteredNotes,
            selectedCategory = category,
            searchQuery = query,
            fileCount = docs.size,
            noteCount = notes.size,
            uploadProgress = progress,
            uploadFileName = uploadName,
            errorMessage = error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DocumentsUiState()
    )

    fun setTab(tab: DocumentTab) {
        _currentTab.value = tab
    }

    fun setCategory(category: DocumentCategory?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun importFileUri(uri: Uri, context: Context, category: DocumentCategory = DocumentCategory.DOCUMENTS) {
        viewModelScope.launch {
            _uploadProgress.value = 0.05f
            _uploadFileName.value = "Importing file..."
            _errorMessage.value = null

            val result = documentRepository.importFile(
                uri = uri,
                context = context,
                category = category,
                onProgress = { progress ->
                    _uploadProgress.value = progress
                }
            )

            if (result.isSuccess) {
                _uploadProgress.value = null
                _uploadFileName.value = null
            } else {
                _uploadProgress.value = null
                _uploadFileName.value = null
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Import failed"
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun togglePinNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.togglePin(noteId)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }

    fun deleteDocument(docId: Long) {
        viewModelScope.launch {
            documentRepository.deleteDocument(docId)
        }
    }
}
