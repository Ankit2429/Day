package com.day.app.ui.documents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.day.app.DayApplication
import com.day.app.domain.model.Document
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class DocumentDetailsUiState(
    val document: Document? = null,
    val localFile: File? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val uploadProgress: Float? = null,
    val feedbackMessage: String? = null
)

class DocumentDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayApplication
    private val documentRepository = app.documentRepository

    private val _uiState = MutableStateFlow(DocumentDetailsUiState())
    val uiState: StateFlow<DocumentDetailsUiState> = _uiState.asStateFlow()

    fun loadDocument(id: Long) {
        viewModelScope.launch {
            val doc = documentRepository.getDocumentByIdSync(id)
            val file = doc?.let { documentRepository.getLocalFileForDocument(it) }
            _uiState.value = DocumentDetailsUiState(
                document = doc,
                localFile = file,
                isLoading = false
            )
        }
    }

    fun uploadToTelegram() {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(uploadProgress = 0.1f)
            val result = documentRepository.uploadToTelegram(doc.id) { progress ->
                _uiState.value = _uiState.value.copy(uploadProgress = progress)
            }
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    document = result.getOrNull(),
                    uploadProgress = null,
                    feedbackMessage = "ARCHIVED TO TELEGRAM PRIVATE STORAGE"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    uploadProgress = null,
                    feedbackMessage = result.exceptionOrNull()?.message ?: "Upload failed"
                )
            }
        }
    }

    fun deleteDocument(onSuccess: () -> Unit) {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            documentRepository.deleteDocument(doc.id)
            _uiState.value = _uiState.value.copy(isDeleted = true)
            onSuccess()
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }
}
