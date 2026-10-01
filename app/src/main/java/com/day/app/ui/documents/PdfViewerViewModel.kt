package com.day.app.ui.documents

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.day.app.DayApplication
import com.day.app.domain.model.Document
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PdfViewerUiState(
    val document: Document? = null,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val localFile: File? = null
)

class PdfViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayApplication
    private val documentRepository = app.documentRepository
    private var renderer: PdfRendererHelper? = null

    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    fun loadPdf(documentId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val doc = documentRepository.getDocumentByIdSync(documentId)

            if (doc == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Document not found")
                return@launch
            }

            val file = documentRepository.getLocalFileForDocument(doc)
            if (file == null || !file.exists()) {
                _uiState.value = _uiState.value.copy(
                    document = doc,
                    isLoading = false,
                    errorMessage = "File not cached locally"
                )
                return@launch
            }

            try {
                renderer?.close()
                val pdfHelper = PdfRendererHelper(file)
                renderer = pdfHelper

                val pageCount = pdfHelper.pageCount
                val firstPageBitmap = pdfHelper.renderPage(0)

                _uiState.value = PdfViewerUiState(
                    document = doc,
                    currentPageIndex = 0,
                    totalPages = pageCount,
                    currentPageBitmap = firstPageBitmap,
                    isLoading = false,
                    localFile = file
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    document = doc,
                    isLoading = false,
                    errorMessage = "Cannot render PDF: ${e.message}"
                )
            }
        }
    }

    fun nextPage() {
        val current = _uiState.value
        if (current.currentPageIndex < current.totalPages - 1) {
            goToPage(current.currentPageIndex + 1)
        }
    }

    fun previousPage() {
        val current = _uiState.value
        if (current.currentPageIndex > 0) {
            goToPage(current.currentPageIndex - 1)
        }
    }

    fun goToPage(page: Int) {
        viewModelScope.launch {
            val bitmap = renderer?.renderPage(page)
            _uiState.value = _uiState.value.copy(
                currentPageIndex = page,
                currentPageBitmap = bitmap
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        renderer?.close()
    }
}
