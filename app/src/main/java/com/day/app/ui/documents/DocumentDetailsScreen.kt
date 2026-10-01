package com.day.app.ui.documents

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.day.app.ui.components.NeoAlertBanner
import com.day.app.ui.components.NeoAlertVariant
import com.day.app.ui.components.NeoBadge
import com.day.app.ui.components.NeoBadgeVariant
import com.day.app.ui.components.NeoButton
import com.day.app.ui.components.NeoButtonVariant
import com.day.app.ui.components.NeoCard
import com.day.app.ui.components.NeoHeader
import com.day.app.ui.components.NeoProgressBar
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayDivider
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentDetailsScreen(
    documentId: Long,
    viewModel: DocumentDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPdfViewer: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    val doc = state.document

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DayBackground)
    ) {
        NeoHeader(
            title = "File Info",
            onBackClick = onNavigateBack
        )

        if (doc == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "File not found",
                    color = DayTextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 15.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                state.feedbackMessage?.let { msg ->
                    NeoAlertBanner(
                        title = "Notice",
                        message = msg,
                        variant = NeoAlertVariant.INFO,
                        onDismiss = { viewModel.clearFeedback() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                state.uploadProgress?.let { progress ->
                    NeoCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = DaySurfaceElevated,
                        cornerRadius = 14.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            NeoProgressBar(progress = progress, height = 6.dp, showPercentage = true)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // File Info Card
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DaySurfaceElevated,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NeoBadge(
                                text = doc.extensionUpper(),
                                variant = NeoBadgeVariant.WHITE
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            NeoBadge(
                                text = doc.storageProvider,
                                variant = NeoBadgeVariant.BLACK
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = doc.displayName.ifBlank { doc.fileName },
                            color = DayTextPrimary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = DayDivider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow("Size", doc.formattedSize())
                        DetailRow("Type", doc.mimeType)
                        DetailRow("Created", com.day.app.util.TimeFormatter.formatDateWithTime(doc.createdAt))
                        DetailRow("Storage", doc.storageProvider)
                        doc.subject?.let { DetailRow("Subject", it) }
                        if (doc.tags.isNotEmpty()) {
                            DetailRow("Tags", doc.tags.joinToString(", "))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Preview or Open Action
                if (doc.isPdf()) {
                    NeoButton(
                        text = "Preview PDF",
                        onClick = { onNavigateToPdfViewer(doc.id) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = NeoButtonVariant.FILLED_PRIMARY
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                state.localFile?.let { file ->
                    NeoButton(
                        text = "Open with External App",
                        onClick = {
                            try {
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(context, "com.day.app.fileprovider", file)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, doc.mimeType)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open with"))
                                }
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = if (doc.isPdf()) NeoButtonVariant.OUTLINE_SECONDARY else NeoButtonVariant.FILLED_PRIMARY
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    NeoButton(
                        text = "Share File",
                        onClick = {
                            try {
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(context, "com.day.app.fileprovider", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = doc.mimeType
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share File"))
                                }
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = NeoButtonVariant.OUTLINE_SECONDARY
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (doc.storageProvider == "LOCAL") {
                    NeoButton(
                        text = "Upload to Telegram Storage",
                        onClick = { viewModel.uploadToTelegram() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = NeoButtonVariant.OUTLINE_SECONDARY
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                NeoButton(
                    text = "Delete File",
                    onClick = { viewModel.deleteDocument(onSuccess = onNavigateBack) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = NeoButtonVariant.DESTRUCTIVE
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = DayTextTertiary,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = DayTextPrimary
        )
    }
}
