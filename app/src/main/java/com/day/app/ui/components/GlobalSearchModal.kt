package com.day.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.DayApplication
import com.day.app.domain.model.Document
import com.day.app.domain.model.Note
import com.day.app.domain.model.Task
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * THE DAY — GLOBAL SEARCH MODAL
 *
 * Triggered by the dedicated circular glass search button on the bottom nav.
 * Live real-time search across:
 * - Tasks (title, description)
 * - Notes (title, content)
 * - Files / Documents (title, tags, category)
 */
@Composable
fun GlobalSearchModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNavigateToTask: (Long) -> Unit,
    onNavigateToNote: (Long) -> Unit,
    onNavigateToDocument: (Long) -> Unit
) {
    if (!isVisible) return

    val context = LocalContext.current
    val app = context.applicationContext as DayApplication
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Collect data streams
    val allTasks by app.taskRepository.getAllTasks().collectAsState(initial = emptyList())
    val allNotes by app.noteRepository.getAllNotes().collectAsState(initial = emptyList())
    val allDocs by app.documentRepository.getAllDocuments().collectAsState(initial = emptyList())

    // Filter results reactively
    val matchedTasks = remember(allTasks, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allTasks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }.take(8)
    }

    val matchedNotes = remember(allNotes, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allNotes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.body.contains(searchQuery, ignoreCase = true)
        }.take(8)
    }

    val matchedDocs = remember(allDocs, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allDocs.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
        }.take(8)
    }

    val hasResults = matchedTasks.isNotEmpty() || matchedNotes.isNotEmpty() || matchedDocs.isNotEmpty()

    LaunchedEffect(isVisible) {
        if (isVisible) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Modal Sheet Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.92f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color(0xF00A0A0C))
                .border(
                    width = 0.75.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .clickable(enabled = false) {} // Prevent backdrop dismissal on click inside
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GLOBAL SEARCH",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                        color = DayTextTertiary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DayTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Field
                NeoInput(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search tasks, notes, files...",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = DayTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = DayTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Results or Empty State
                if (searchQuery.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = DayTextDisabled,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Search anything across THE DAY",
                                color = DayTextTertiary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                } else if (!hasResults) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results for \"$searchQuery\"",
                            color = DayTextTertiary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // 1. Tasks
                        if (matchedTasks.isNotEmpty()) {
                            item {
                                SectionLabel(icon = Icons.Default.Checklist, title = "TASKS (${matchedTasks.size})")
                            }
                            items(matchedTasks, key = { "task_${it.id}" }) { task ->
                                SearchTaskResultRow(
                                    task = task,
                                    onClick = {
                                        onDismiss()
                                        onNavigateToTask(task.id)
                                    },
                                    onToggle = { done ->
                                        coroutineScope.launch {
                                            app.taskRepository.completeTask(task.id, done)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        // 2. Notes
                        if (matchedNotes.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                SectionLabel(icon = Icons.Default.EditNote, title = "NOTES (${matchedNotes.size})")
                            }
                            items(matchedNotes, key = { "note_${it.id}" }) { note ->
                                SearchNoteResultRow(
                                    note = note,
                                    onClick = {
                                        onDismiss()
                                        onNavigateToNote(note.id)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        // 3. Documents / Files
                        if (matchedDocs.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                SectionLabel(icon = Icons.Default.Description, title = "FILES (${matchedDocs.size})")
                            }
                            items(matchedDocs, key = { "doc_${it.id}" }) { doc ->
                                SearchDocResultRow(
                                    doc = doc,
                                    onClick = {
                                        onDismiss()
                                        onNavigateToDocument(doc.id)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DayTextTertiary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = DayTextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun SearchTaskResultRow(
    task: Task,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101012))
            .border(0.5.dp, Color(0xFF222226), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            NeoCheckbox(
                checked = task.completed,
                onCheckedChange = onToggle,
                size = 18.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = if (task.completed) DayTextDisabled else DayTextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        color = DayTextTertiary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchNoteResultRow(
    note: Note,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101012))
            .border(0.5.dp, Color(0xFF222226), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = note.title.ifBlank { "Untitled Note" },
                color = DayTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val snippetText = note.snippet(80)
            if (snippetText.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = snippetText,
                    color = DayTextTertiary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchDocResultRow(
    doc: Document,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101012))
            .border(0.5.dp, Color(0xFF222226), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = DayTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = doc.displayName,
                color = DayTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1C1C20))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = doc.extension.uppercase(Locale.US).ifBlank { "FILE" },
                    color = DayTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
