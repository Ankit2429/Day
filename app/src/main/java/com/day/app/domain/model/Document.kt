package com.day.app.domain.model

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Document(
    val id: Long = 0L,
    val fileName: String,
    val displayName: String,
    val mimeType: String,
    val extension: String,
    val size: Long, // in bytes
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val storageProvider: String = "LOCAL", // "TELEGRAM" or "LOCAL"
    val remoteFileReference: String? = null,
    val telegramMessageId: Long? = null,
    val telegramFileId: String? = null,
    val localCachedPath: String? = null,
    val thumbnailReference: String? = null,
    val tags: List<String> = emptyList(),
    val category: DocumentCategory = DocumentCategory.DOCUMENTS,
    val subject: String? = null,
    val description: String = "",
    val localCacheStatus: CacheStatus = CacheStatus.CACHED
) {
    fun isPdf(): Boolean = extension.equals("pdf", ignoreCase = true) || mimeType.contains("pdf", ignoreCase = true)

    fun isImage(): Boolean = extension in listOf("jpg", "jpeg", "png", "webp", "svg") || mimeType.startsWith("image/")

    fun isOfficeDoc(): Boolean = extension in listOf("doc", "docx", "ppt", "pptx", "xls", "xlsx", "csv", "rtf", "txt", "md")

    fun extensionUpper(): String = extension.uppercase().ifBlank { "FILE" }

    fun formattedSize(): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val format = DecimalFormat("#,##0.#")
        return "${format.format(size / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    fun formattedDate(): String {
        return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(createdAt))
    }
}
