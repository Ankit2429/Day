package com.day.app.data.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class LocalStorageProvider(private val context: Context) : FileStorageProvider {

    private val storageDir: File
        get() = File(context.filesDir, "documents").apply {
            if (!exists()) mkdirs()
        }

    override suspend fun upload(
        file: File,
        mimeType: String,
        onProgress: (Float) -> Unit
    ): Result<StorageUploadResult> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(IOException("Source file does not exist"))
            }

            val targetFile = File(storageDir, "${System.currentTimeMillis()}_${file.name}")
            val totalBytes = file.length()
            var bytesCopied = 0L

            FileInputStream(file).use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead
                        if (totalBytes > 0) {
                            onProgress(bytesCopied.toFloat() / totalBytes.toFloat())
                        }
                    }
                }
            }

            onProgress(1.0f)
            Result.success(
                StorageUploadResult(
                    remoteReference = targetFile.absolutePath,
                    storageProvider = "LOCAL",
                    fileSizeBytes = targetFile.length()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun download(
        remoteReference: String,
        destinationFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(remoteReference)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(IOException("Local file not found"))
            }

            if (sourceFile.absolutePath == destinationFile.absolutePath) {
                onProgress(1f)
                return@withContext Result.success(destinationFile)
            }

            sourceFile.copyTo(destinationFile, overwrite = true)
            onProgress(1f)
            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(remoteReference: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(remoteReference)
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMetadata(remoteReference: String): Result<RemoteFileMetadata> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(remoteReference)
                if (!file.exists()) {
                    return@withContext Result.failure(IOException("File not found"))
                }
                Result.success(
                    RemoteFileMetadata(
                        remoteReference = remoteReference,
                        fileName = file.name,
                        sizeBytes = file.length(),
                        mimeType = "application/octet-stream"
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun exists(remoteReference: String): Boolean = withContext(Dispatchers.IO) {
        File(remoteReference).exists()
    }
}
