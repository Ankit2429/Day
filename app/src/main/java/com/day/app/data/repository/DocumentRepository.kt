package com.day.app.data.repository

import android.content.Context
import android.net.Uri
import com.day.app.domain.model.Document
import com.day.app.domain.model.DocumentCategory
import kotlinx.coroutines.flow.Flow
import java.io.File

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<Document>>
    suspend fun getAllDocumentsSync(): List<Document>
    fun getDocumentsByCategory(category: DocumentCategory): Flow<List<Document>>
    fun searchDocuments(query: String): Flow<List<Document>>
    fun getDocumentById(id: Long): Flow<Document?>
    suspend fun getDocumentByIdSync(id: Long): Document?
    fun getDocumentCount(): Flow<Int>
    suspend fun getDocumentCountSync(): Int

    suspend fun importFile(
        uri: Uri,
        context: Context,
        category: DocumentCategory = DocumentCategory.DOCUMENTS,
        tags: List<String> = emptyList(),
        subject: String? = null,
        description: String = "",
        onProgress: (Float) -> Unit = {}
    ): Result<Document>

    suspend fun saveDocument(document: Document): Long
    suspend fun deleteDocument(id: Long): Result<Unit>
    suspend fun getLocalFileForDocument(document: Document): File?
    suspend fun uploadToTelegram(id: Long, onProgress: (Float) -> Unit): Result<Document>
}
