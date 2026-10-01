package com.day.app.ui.documents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.domain.model.DocumentCategory
import com.day.app.ui.components.FileCardItem
import com.day.app.ui.components.NeoAlertBanner
import com.day.app.ui.components.NeoAlertVariant
import com.day.app.ui.components.NeoButton
import com.day.app.ui.components.NeoButtonVariant
import com.day.app.ui.components.NeoCard
import com.day.app.ui.components.NeoEmptyState
import com.day.app.ui.components.NeoHeader
import com.day.app.ui.components.NeoInput
import com.day.app.ui.components.NeoProgressBar
import com.day.app.ui.components.NeoTabItem
import com.day.app.ui.components.NeoTabRow
import com.day.app.ui.components.NoteCardItem
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayDivider
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DayTextTertiary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.day.app.ui.components.AmbientGlassBackground
import com.day.app.ui.components.GlassButton
import com.day.app.ui.components.GlassCard
import com.day.app.ui.components.GlassFloatingUpload
import com.day.app.ui.components.GlassPill
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    viewModel: DocumentsViewModel,
    onNavigateToDocumentDetails: (Long) -> Unit,
    onNavigateToPdfViewer: (Long) -> Unit,
    onNavigateToNoteEditor: (Long) -> Unit,
    initialTab: DocumentTab = DocumentTab.FILES
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showUploadSheet by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFileUri(it, context, state.selectedCategory ?: DocumentCategory.DOCUMENTS)
        }
    }

    AmbientGlassBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                NeoHeader(
                    title = if (state.currentTab == DocumentTab.FILES) "FILES" else "NOTES",
                    actionButton = {
                        if (state.currentTab == DocumentTab.FILES) {
                            GlassButton(
                                text = "+ Upload",
                                onClick = { showUploadSheet = true },
                                isPrimary = true,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 14.dp,
                                    vertical = 6.dp
                                )
                            )
                        } else {
                            GlassButton(
                                text = "+ Note",
                                onClick = { onNavigateToNoteEditor(0L) },
                                isPrimary = true,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 14.dp,
                                    vertical = 6.dp
                                )
                            )
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Primary FILES / NOTES Tab Selector
                    NeoTabRow(
                        tabs = listOf(
                            NeoTabItem("FILES", state.documents.size),
                            NeoTabItem("NOTES", state.notes.size)
                        ),
                        selectedIndex = state.currentTab.ordinal,
                        onTabSelected = { viewModel.setTab(DocumentTab.entries[it]) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search Bar
                    NeoInput(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = if (state.currentTab == DocumentTab.FILES) "Search files..." else "Search notes...",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = DayTextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Error banner if any
                    state.errorMessage?.let { error ->
                        NeoAlertBanner(
                            title = "File Import Error",
                            message = error,
                            variant = NeoAlertVariant.ERROR,
                            onDismiss = { viewModel.dismissError() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Upload Progress Bar with Telegram Archive Status Indicator
                    state.uploadProgress?.let { progress ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Syncing",
                                        tint = DayTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = state.uploadFileName ?: "Uploading to Telegram Archive...",
                                        color = DayTextPrimary,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val percent = (progress * 100).toInt()
                                    Text(
                                        text = "$percent%",
                                        color = DayTextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                NeoProgressBar(
                                    progress = progress,
                                    height = 5.dp,
                                    showPercentage = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

            // Sub-category filters for files
            if (state.currentTab == DocumentTab.FILES) {
                val fileFilters = listOf(
                    NeoTabItem("ALL"),
                    NeoTabItem("PDF"),
                    NeoTabItem("DOCS"),
                    NeoTabItem("IMAGES")
                )
                val selectedFilterIdx = when (state.selectedCategory) {
                    null -> 0
                    DocumentCategory.DOCUMENTS -> 1
                    DocumentCategory.PROJECTS -> 2
                    DocumentCategory.IMAGES -> 3
                    else -> 0
                }

                NeoTabRow(
                    tabs = fileFilters,
                    selectedIndex = selectedFilterIdx,
                    onTabSelected = { idx ->
                        when (idx) {
                            0 -> viewModel.setCategory(null)
                            1 -> viewModel.setCategory(DocumentCategory.DOCUMENTS)
                            2 -> viewModel.setCategory(DocumentCategory.PROJECTS)
                            3 -> viewModel.setCategory(DocumentCategory.IMAGES)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Content List
            if (state.currentTab == DocumentTab.FILES) {
                if (state.documents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        NeoEmptyState(
                            title = if (state.searchQuery.isNotBlank()) "No matching files" else "No files in library",
                            description = if (state.searchQuery.isNotBlank()) "No files match '${state.searchQuery}'"
                            else "Import PDFs, documents, or images from your device.",
                            actionText = "+ Upload File",
                            onActionClick = { showUploadSheet = true }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
                    ) {
                        items(state.documents, key = { it.id }) { doc ->
                            FileCardItem(
                                document = doc,
                                onClick = {
                                    if (doc.isPdf()) {
                                        onNavigateToPdfViewer(doc.id)
                                    } else {
                                        onNavigateToDocumentDetails(doc.id)
                                    }
                                }
                            )
                            HorizontalDivider(color = DayDivider, thickness = 0.5.dp)
                        }
                    }
                }
            } else {
                if (state.notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        NeoEmptyState(
                            title = if (state.searchQuery.isNotBlank()) "No matching notes" else "No notes created",
                            description = if (state.searchQuery.isNotBlank()) "No notes match '${state.searchQuery}'"
                            else "Create your first minimal note or memo.",
                            actionText = "+ Add Note",
                            onActionClick = { onNavigateToNoteEditor(0L) }
                        )
                    }
                } else {
                    val pinnedNotes = state.notes.filter { it.pinned }
                    val otherNotes = state.notes.filter { !it.pinned }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
                    ) {
                        if (pinnedNotes.isNotEmpty()) {
                            item {
                                Text(
                                    text = "PINNED",
                                    color = DayTextTertiary,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                                )
                            }
                            items(pinnedNotes, key = { it.id }) { note ->
                                NoteCardItem(
                                    note = note,
                                    onClick = { onNavigateToNoteEditor(note.id) },
                                    onPinToggle = { viewModel.togglePinNote(note.id) }
                                )
                                HorizontalDivider(color = DayDivider, thickness = 0.5.dp)
                            }
                        }

                        if (otherNotes.isNotEmpty()) {
                            if (pinnedNotes.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "ALL",
                                        color = DayTextTertiary,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.2.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            items(otherNotes, key = { it.id }) { note ->
                                NoteCardItem(
                                    note = note,
                                    onClick = { onNavigateToNoteEditor(note.id) },
                                    onPinToggle = { viewModel.togglePinNote(note.id) }
                                )
                                HorizontalDivider(color = DayDivider, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }

            // Floating Upload / Note Glass Button (aligned to bottom-end above bottom nav dock)
            GlassFloatingUpload(
                onClick = {
                    if (state.currentTab == DocumentTab.FILES) {
                        showUploadSheet = true
                    } else {
                        onNavigateToNoteEditor(0L)
                    }
                },
                label = if (state.currentTab == DocumentTab.FILES) "UPLOAD" else "NOTE",
                icon = if (state.currentTab == DocumentTab.FILES) Icons.Default.CloudUpload else Icons.Default.Add,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(bottom = 92.dp, end = 20.dp)
            )

            // Upload Options Modal Bottom Sheet
            if (showUploadSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showUploadSheet = false },
                    containerColor = Color(0xF2121216),
                    scrimColor = Color(0x99000000),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "UPLOAD ARCHIVE",
                            color = DayTextTertiary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val options = listOf(
                            Triple("Upload Document", "PDF, Docs, spreadsheets", arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/*")),
                            Triple("Upload Image", "Photos, diagrams, scans", arrayOf("image/*")),
                            Triple("Upload Any File", "Archives, code, raw assets", arrayOf("*/*"))
                        )

                        options.forEach { (title, subtitle, mimeTypes) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showUploadSheet = false
                                        filePickerLauncher.launch(mimeTypes)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x18FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = DayTextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = title,
                                        color = DayTextPrimary,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.5.sp
                                    )
                                    Text(
                                        text = subtitle,
                                        color = DayTextTertiary,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
