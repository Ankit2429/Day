package com.day.app.data.storage

import java.io.File

data class StorageUploadResult(
    val remoteReference: String,
    val storageProvider: String,
    val telegramMessageId: Long? = null,
    val telegramFileId: String? = null,
    val fileSizeBytes: Long
)

data class RemoteFileMetadata(
    val remoteReference: String,
    val fileName: String,
    val sizeBytes: Long,
    val mimeType: String
)

interface FileStorageProvider {
    suspend fun upload(
        file: File,
        mimeType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<StorageUploadResult>

    suspend fun download(
        remoteReference: String,
        destinationFile: File,
        onProgress: (Float) -> Unit = {}
    ): Result<File>

    suspend fun delete(remoteReference: String): Result<Unit>

    suspend fun getMetadata(remoteReference: String): Result<RemoteFileMetadata>

    suspend fun exists(remoteReference: String): Boolean
}
