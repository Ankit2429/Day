package com.day.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.day.app.data.local.DocumentDao
import com.day.app.data.local.DocumentEntity
import com.day.app.data.storage.FileStorageProvider
import com.day.app.domain.model.CacheStatus
import com.day.app.domain.model.Document
import com.day.app.domain.model.DocumentCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentRepositoryImpl(
    private val documentDao: DocumentDao,
    private val storageProvider: FileStorageProvider,
    private val context: Context
) : DocumentRepository {

    override fun getAllDocuments(): Flow<List<Document>> {
        return documentDao.getAllDocuments().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllDocumentsSync(): List<Document> {
        return documentDao.getAllDocumentsSync().map { it.toDomain() }
    }

    override fun getDocumentsByCategory(category: DocumentCategory): Flow<List<Document>> {
        return documentDao.getDocumentsByCategory(category).map { list -> list.map { it.toDomain() } }
    }

    override fun searchDocuments(query: String): Flow<List<Document>> {
        return documentDao.searchDocuments(query).map { list -> list.map { it.toDomain() } }
    }

    override fun getDocumentById(id: Long): Flow<Document?> {
        return documentDao.getDocumentById(id).map { it?.toDomain() }
    }

    override suspend fun getDocumentByIdSync(id: Long): Document? {
        return documentDao.getDocumentByIdSync(id)?.toDomain()
    }

    override fun getDocumentCount(): Flow<Int> = documentDao.getDocumentCount()

    override suspend fun getDocumentCountSync(): Int = documentDao.getDocumentCountSync()

    override suspend fun importFile(
        uri: Uri,
        context: Context,
        category: DocumentCategory,
        tags: List<String>,
        subject: String?,
        description: String,
        onProgress: (Float) -> Unit
    ): Result<Document> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "document_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val extension = fileName.substringAfterLast('.', "").lowercase()

            // Copy to local temporary file
            val tempDir = File(context.cacheDir, "imports").apply { if (!exists()) mkdirs() }
            val tempFile = File(tempDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open content URI"))

            if (fileSize == 0L) {
                fileSize = tempFile.length()
            }

            // Upload via storage provider (caches locally and/or uploads)
            val uploadResult = storageProvider.upload(tempFile, mimeType, onProgress)
            if (uploadResult.isFailure) {
                tempFile.delete()
                return@withContext Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
            }

            val storageInfo = uploadResult.getOrThrow()

            val document = Document(
                id = 0L,
                fileName = fileName,
                displayName = fileName.substringBeforeLast('.'),
                mimeType = mimeType,
                extension = extension,
                size = fileSize,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                storageProvider = storageInfo.storageProvider,
                remoteFileReference = storageInfo.remoteReference,
                telegramMessageId = storageInfo.telegramMessageId,
                telegramFileId = storageInfo.telegramFileId,
                localCachedPath = if (storageInfo.remoteReference.startsWith("/")) storageInfo.remoteReference else tempFile.absolutePath,
                tags = tags,
                category = category,
                subject = subject,
                description = description,
                localCacheStatus = CacheStatus.CACHED
            )

            val insertedId = documentDao.insertDocument(DocumentEntity.fromDomain(document))
            Result.success(document.copy(id = insertedId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveDocument(document: Document): Long {
        val entity = DocumentEntity.fromDomain(document)
        return if (document.id == 0L) {
            documentDao.insertDocument(entity)
        } else {
            documentDao.updateDocument(entity)
            document.id
        }
    }

    override suspend fun deleteDocument(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentByIdSync(id) ?: return@withContext Result.success(Unit)
        doc.localCachedPath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        doc.remoteFileReference?.let { ref ->
            storageProvider.delete(ref)
        }
        documentDao.deleteDocumentById(id)
        Result.success(Unit)
    }

    override suspend fun getLocalFileForDocument(document: Document): File? = withContext(Dispatchers.IO) {
        // Check localCachedPath first
        document.localCachedPath?.let { path ->
            val file = File(path)
            if (file.exists() && file.length() > 0) return@withContext file
        }

        // If stored as local file reference
        document.remoteFileReference?.let { ref ->
            if (ref.startsWith("/")) {
                val file = File(ref)
                if (file.exists() && file.length() > 0) return@withContext file
            }
        }

        null
    }

    override suspend fun uploadToTelegram(id: Long, onProgress: (Float) -> Unit): Result<Document> =
        withContext(Dispatchers.IO) {
            val doc = documentDao.getDocumentByIdSync(id)?.toDomain()
                ?: return@withContext Result.failure(Exception("Document not found"))

            val localFile = getLocalFileForDocument(doc)
                ?: return@withContext Result.failure(Exception("Local cached file missing"))

            val result = storageProvider.upload(localFile, doc.mimeType, onProgress)
            if (result.isSuccess) {
                val uploadInfo = result.getOrThrow()
                val updated = doc.copy(
                    storageProvider = uploadInfo.storageProvider,
                    remoteFileReference = uploadInfo.remoteReference,
                    telegramMessageId = uploadInfo.telegramMessageId,
                    telegramFileId = uploadInfo.telegramFileId,
                    localCacheStatus = CacheStatus.CACHED,
                    updatedAt = System.currentTimeMillis()
                )
                documentDao.updateDocument(DocumentEntity.fromDomain(updated))
                Result.success(updated)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Upload to Telegram failed"))
            }
        }
}
