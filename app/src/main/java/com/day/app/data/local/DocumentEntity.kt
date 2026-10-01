package com.day.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.day.app.domain.model.CacheStatus
import com.day.app.domain.model.Document
import com.day.app.domain.model.DocumentCategory

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fileName: String,
    val displayName: String,
    val mimeType: String,
    val extension: String,
    val size: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val storageProvider: String,
    val remoteFileReference: String?,
    val telegramMessageId: Long?,
    val telegramFileId: String?,
    val localCachedPath: String?,
    val thumbnailReference: String?,
    val tags: List<String>,
    val category: DocumentCategory,
    val subject: String?,
    val description: String,
    val localCacheStatus: CacheStatus
) {
    fun toDomain(): Document = Document(
        id = id,
        fileName = fileName,
        displayName = displayName,
        mimeType = mimeType,
        extension = extension,
        size = size,
        createdAt = createdAt,
        updatedAt = updatedAt,
        storageProvider = storageProvider,
        remoteFileReference = remoteFileReference,
        telegramMessageId = telegramMessageId,
        telegramFileId = telegramFileId,
        localCachedPath = localCachedPath,
        thumbnailReference = thumbnailReference,
        tags = tags,
        category = category,
        subject = subject,
        description = description,
        localCacheStatus = localCacheStatus
    )

    companion object {
        fun fromDomain(doc: Document): DocumentEntity = DocumentEntity(
            id = doc.id,
            fileName = doc.fileName,
            displayName = doc.displayName,
            mimeType = doc.mimeType,
            extension = doc.extension,
            size = doc.size,
            createdAt = doc.createdAt,
            updatedAt = doc.updatedAt,
            storageProvider = doc.storageProvider,
            remoteFileReference = doc.remoteFileReference,
            telegramMessageId = doc.telegramMessageId,
            telegramFileId = doc.telegramFileId,
            localCachedPath = doc.localCachedPath,
            thumbnailReference = doc.thumbnailReference,
            tags = doc.tags,
            category = doc.category,
            subject = doc.subject,
            description = doc.description,
            localCacheStatus = doc.localCacheStatus
        )
    }
}
