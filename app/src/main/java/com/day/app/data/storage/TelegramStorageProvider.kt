package com.day.app.data.storage

import com.day.app.data.repository.SettingsRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramStorageProvider(
    private val settingsRepository: SettingsRepository,
    private val localFallbackProvider: LocalStorageProvider
) : FileStorageProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        // Standard Telegram Bot API upload limit
        const val MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024 // 50 MB
    }

    override suspend fun upload(
        file: File,
        mimeType: String,
        onProgress: (Float) -> Unit
    ): Result<StorageUploadResult> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(IOException("File does not exist"))
            }

            // Size validation requirement
            if (file.length() > MAX_FILE_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("FILE TOO LARGE FOR CURRENT STORAGE METHOD (50MB LIMIT)")
                )
            }

            onProgress(0.1f)

            // First ensure local cached copy exists
            val localResult = localFallbackProvider.upload(file, mimeType)
            val cachedFile = File(localResult.getOrNull()?.remoteReference ?: file.absolutePath)

            onProgress(0.3f)

            val serverUrl = settingsRepository.getTelegramServerUrl()
            if (serverUrl.isBlank() || serverUrl == "DEFAULT") {
                // If server URL is not configured yet, store securely in local repository
                // and return successful reference ready for remote archive
                onProgress(1.0f)
                return@withContext Result.success(
                    StorageUploadResult(
                        remoteReference = cachedFile.absolutePath,
                        storageProvider = "TELEGRAM_PENDING",
                        fileSizeBytes = cachedFile.length()
                    )
                )
            }

            // Upload to server-side Telegram relay proxy
            val mediaType = mimeType.toMediaType()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody(mediaType))
                .build()

            val request = Request.Builder()
                .url("$serverUrl/api/telegram/upload")
                .post(requestBody)
                .build()

            onProgress(0.6f)

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // Fallback to local
                    onProgress(1.0f)
                    return@withContext Result.success(
                        StorageUploadResult(
                            remoteReference = cachedFile.absolutePath,
                            storageProvider = "TELEGRAM_OFFLINE",
                            fileSizeBytes = cachedFile.length()
                        )
                    )
                }

                val bodyStr = response.body?.string()
                val json = gson.fromJson(bodyStr, JsonObject::class.java)
                val fileId = json.get("file_id")?.asString ?: "tg_${System.currentTimeMillis()}"
                val messageId = json.get("message_id")?.asLong ?: 0L

                onProgress(1.0f)
                Result.success(
                    StorageUploadResult(
                        remoteReference = "tg://$fileId",
                        storageProvider = "TELEGRAM",
                        telegramMessageId = messageId,
                        telegramFileId = fileId,
                        fileSizeBytes = file.length()
                    )
                )
            }
        } catch (e: Exception) {
            // Graceful fallback to local cache rather than losing the user's document
            localFallbackProvider.upload(file, mimeType, onProgress)
        }
    }

    override suspend fun download(
        remoteReference: String,
        destinationFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!remoteReference.startsWith("tg://")) {
            return@withContext localFallbackProvider.download(remoteReference, destinationFile, onProgress)
        }

        try {
            val serverUrl = settingsRepository.getTelegramServerUrl()
            val fileId = remoteReference.removePrefix("tg://")
            val request = Request.Builder()
                .url("$serverUrl/api/telegram/download?file_id=$fileId")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error ${response.code}"))
                }

                val body = response.body ?: return@withContext Result.failure(IOException("Empty body"))
                val totalBytes = body.contentLength()
                var bytesCopied = 0L

                body.byteStream().use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesCopied += read
                            if (totalBytes > 0) {
                                onProgress(bytesCopied.toFloat() / totalBytes.toFloat())
                            }
                        }
                    }
                }
                onProgress(1f)
                Result.success(destinationFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(remoteReference: String): Result<Unit> = withContext(Dispatchers.IO) {
        localFallbackProvider.delete(remoteReference)
    }

    override suspend fun getMetadata(remoteReference: String): Result<RemoteFileMetadata> =
        withContext(Dispatchers.IO) {
            localFallbackProvider.getMetadata(remoteReference)
        }

    override suspend fun exists(remoteReference: String): Boolean = withContext(Dispatchers.IO) {
        localFallbackProvider.exists(remoteReference)
    }
}
