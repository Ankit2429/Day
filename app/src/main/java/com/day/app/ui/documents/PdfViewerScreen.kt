package com.day.app.ui.documents

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.day.app.ui.components.NeoAlertBanner
import com.day.app.ui.components.NeoAlertVariant
import com.day.app.ui.components.NeoButton
import com.day.app.ui.components.NeoButtonVariant
import com.day.app.ui.components.NeoHeader
import com.day.app.ui.theme.DayBackgroundDark
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary

@Composable
fun PdfViewerScreen(
    documentId: Long,
    viewModel: PdfViewerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var zoomLevel by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }

    LaunchedEffect(documentId) {
        viewModel.loadPdf(documentId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DayBackgroundDark)
    ) {
        // Minimal Top Header
        NeoHeader(
            title = state.document?.displayName ?: "PDF Preview",
            subtitle = if (state.totalPages > 0) "${state.currentPageIndex + 1} of ${state.totalPages}" else null,
            onBackClick = onNavigateBack,
            actionButton = {
                NeoButton(
                    text = "Info",
                    onClick = { onNavigateToDetails(documentId) },
                    variant = NeoButtonVariant.OUTLINE_SECONDARY,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    )
                )
            }
        )

        // Main Document Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> {
                    Text(
                        text = "Loading document...",
                        color = DayTextSecondary,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp
                    )
                }
                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NeoAlertBanner(
                            title = "Preview Error",
                            message = state.errorMessage ?: "Failed to render PDF page",
                            variant = NeoAlertVariant.ERROR,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        state.localFile?.let { file ->
                            NeoButton(
                                text = "Open in External App",
                                onClick = {
                                    try {
                                        val uri = FileProvider.getUriForFile(context, "com.day.app.fileprovider", file)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                variant = NeoButtonVariant.FILLED_PRIMARY
                            )
                        }
                    }
                }
                state.currentPageBitmap != null -> {
                    val bitmap = state.currentPageBitmap!!
                    val pageShape = RoundedCornerShape(8.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF Page ${state.currentPageIndex + 1}",
                            modifier = Modifier
                                .fillMaxWidth(zoomLevel.coerceIn(0.5f, 2.5f))
                                .clip(pageShape)
                                .border(1.dp, DayBorderSubtle, pageShape),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }

        // Minimal Bottom Page Controls Toolbar
        if (state.totalPages > 1) {
            val pillShape = RoundedCornerShape(999.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(DaySurfaceElevated, pillShape)
                        .border(1.dp, DayBorderSubtle, pillShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousPage() },
                        enabled = state.currentPageIndex > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Page",
                            tint = if (state.currentPageIndex > 0) DayTextPrimary else DayTextTertiary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${state.currentPageIndex + 1} / ${state.totalPages}",
                        color = DayTextPrimary,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.nextPage() },
                        enabled = state.currentPageIndex < state.totalPages - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Page",
                            tint = if (state.currentPageIndex < state.totalPages - 1) DayTextPrimary else DayTextTertiary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = { zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.5f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Zoom Out",
                            tint = DayTextSecondary
                        )
                    }

                    IconButton(
                        onClick = { zoomLevel = (zoomLevel + 0.25f).coerceAtMost(2.5f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom In",
                            tint = DayTextSecondary
                        )
                    }
                }
            }
        }
    }
}
