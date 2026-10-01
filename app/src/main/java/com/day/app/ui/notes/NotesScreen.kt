package com.day.app.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.domain.model.Note
import com.day.app.ui.components.AmbientGlassBackground
import com.day.app.ui.components.DayLogoMark
import com.day.app.ui.components.GlassButton
import com.day.app.ui.components.NeoInput
import com.day.app.ui.theme.DayDestructive
import com.day.app.ui.theme.DayGlassSurface
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * THE DAY — NOTES SCREEN
 * Reference-Driven Premium Knowledge Workspace
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onNavigateToNoteEditor: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by rememberSaveable { mutableStateOf("") }

    AmbientGlassBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // ── 1. MINIMAL HEADER ──────────────────────────────────────────────────
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DayLogoMark(size = 20.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NOTES",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 2.sp,
                                color = DayTextPrimary
                            )
                            Text(
                                text = if (state.isCategoriesViewActive) "CATEGORIES" else if (state.selectedCategory != null) "CATEGORY · ${state.selectedCategory}" else "YOUR WORKSPACE",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.5.sp,
                                letterSpacing = 1.sp,
                                color = DayTextTertiary
                            )
                        }
                    }

                    // Top Action Controls (Search, Categories, Sort)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Category View Toggle
                        IconButton(
                            onClick = { viewModel.toggleCategoriesView() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isCategoriesViewActive) Icons.Default.FolderOpen else Icons.Default.Folder,
                                contentDescription = "Categories",
                                tint = if (state.isCategoriesViewActive || state.selectedCategory != null) DayTextPrimary else DayTextTertiary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Search Toggle
                        IconButton(
                            onClick = { viewModel.toggleSearch(!state.isSearchExpanded) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (state.isSearchExpanded || state.searchQuery.isNotBlank()) DayTextPrimary else DayTextTertiary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // ── 2. EXPANDABLE SEARCH BAR ──────────────────────────────────────────
                AnimatedVisibility(
                    visible = state.isSearchExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        NeoInput(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = "Search notes, tags, ideas...",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = DayTextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = if (state.searchQuery.isNotBlank()) {
                                {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = DayTextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── 3. SEGMENTED TABS (NOTES | HIGHLIGHTS | FAVORITES) ────────────────
                if (!state.isCategoriesViewActive) {
                    Spacer(modifier = Modifier.height(14.dp))
                    NotesSegmentedTabs(
                        selectedTab = state.selectedTab,
                        onTabSelected = { viewModel.setTab(it) }
                    )
                }

                // Active Category Filter Indicator Pill
                if (state.selectedCategory != null && !state.isCategoriesViewActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141418))
                            .border(0.5.dp, Color(0xFF2A2A30), RoundedCornerShape(12.dp))
                            .clickable { viewModel.setCategory(null) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter: ${state.selectedCategory}",
                            color = DayTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear filter",
                            tint = DayTextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // ── SUBHEADER: "List Notes" & Sort Dropdown ("All Notes ▾") ───────────
                if (!state.isCategoriesViewActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "List Notes",
                            color = DayTextPrimary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        )

                        var sortExpanded by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { sortExpanded = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.sortOrder.label,
                                    color = DayTextTertiary,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.UnfoldMore,
                                    contentDescription = "Sort order",
                                    tint = DayTextTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = sortExpanded,
                                onDismissRequest = { sortExpanded = false },
                                modifier = Modifier
                                    .background(Color(0xFF141418))
                                    .border(0.5.dp, Color(0xFF2A2A30), RoundedCornerShape(8.dp))
                            ) {
                                NoteSortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.label,
                                                color = if (order == state.sortOrder) DayTextPrimary else DayTextSecondary,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (order == state.sortOrder) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            sortExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── 4. MAIN CONTENT AREA (CATEGORIES OR NOTES LIST) ───────────────────
                if (state.isCategoriesViewActive) {
                    // CATEGORIES BROWSER
                    CategoriesBrowserView(
                        categories = state.categories,
                        onSelectCategory = { cat -> viewModel.setCategory(cat) },
                        onCreateCategory = { showCreateCategoryDialog = true }
                    )
                } else {
                    // NOTES LIST
                    if (state.displayedNotes.isEmpty()) {
                        NotesEmptyState(
                            isSearching = state.searchQuery.isNotBlank(),
                            onCreateNote = { onNavigateToNoteEditor(0L) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // PINNED SECTION (if on NOTES tab and pinned notes exist)
                            if (state.selectedTab == NotesTab.NOTES && state.pinnedNotes.isNotEmpty() && state.sortOrder == NoteSortOrder.PINNED) {
                                item(key = "header_pinned") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = null,
                                            tint = DayTextTertiary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PINNED",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 1.5.sp,
                                            color = DayTextTertiary,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }
                                }

                                items(state.pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                                    PremiumNoteCard(
                                        note = note,
                                        onClick = { onNavigateToNoteEditor(note.id) },
                                        onPinToggle = { viewModel.togglePin(note.id) },
                                        onDelete = { noteToDelete = note }
                                    )
                                }

                                item(key = "header_all") {
                                    Text(
                                        text = "ALL NOTES",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.5.sp,
                                        color = DayTextTertiary,
                                        fontFamily = FontFamily.SansSerif,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                    )
                                }

                                items(state.regularNotes, key = { "regular_${it.id}" }) { note ->
                                    PremiumNoteCard(
                                        note = note,
                                        onClick = { onNavigateToNoteEditor(note.id) },
                                        onPinToggle = { viewModel.togglePin(note.id) },
                                        onDelete = { noteToDelete = note }
                                    )
                                }
                            } else {
                                items(state.displayedNotes, key = { "note_${it.id}" }) { note ->
                                    PremiumNoteCard(
                                        note = note,
                                        onClick = { onNavigateToNoteEditor(note.id) },
                                        onPinToggle = { viewModel.togglePin(note.id) },
                                        onDelete = { noteToDelete = note }
                                    )
                                }
                            }

                            // Spacer for bottom navigation
                            item {
                                Spacer(modifier = Modifier.height(130.dp))
                            }
                        }
                    }
                }
            }

            // ── 5. FLOATING GLASS "+" ADD BUTTON ──────────────────────────────────────
            FloatingGlassAddNoteButton(
                onClick = { onNavigateToNoteEditor(0L) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp)
            )
        }
    }

    // Delete Confirmation Dialog
    if (noteToDelete != null) {
        val note = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = {
                Text(
                    text = "Delete Note",
                    color = DayTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${note.title.ifBlank { "Untitled Note" }}\"?",
                    color = DayTextSecondary
                )
            },
            confirmButton = {
                GlassButton(
                    text = "Delete",
                    onClick = {
                        viewModel.deleteNote(note.id)
                        noteToDelete = null
                    },
                    isPrimary = true
                )
            },
            dismissButton = {
                GlassButton(
                    text = "Cancel",
                    onClick = { noteToDelete = null }
                )
            },
            containerColor = Color(0xFF141416),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Create Category Dialog
    if (showCreateCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            title = {
                Text(
                    text = "Create Category",
                    color = DayTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Organize your notes into a new topic or category:",
                        color = DayTextTertiary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NeoInput(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = "e.g. Robotics, Architecture, Ideas",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Create",
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.createCategory(newCategoryName)
                            newCategoryName = ""
                            showCreateCategoryDialog = false
                        }
                    },
                    isPrimary = true
                )
            },
            dismissButton = {
                GlassButton(
                    text = "Cancel",
                    onClick = { showCreateCategoryDialog = false }
                )
            },
            containerColor = Color(0xFF141416),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Clean segmented tabs for Notes (NOTES | RECENT | PINNED | FAVORITES)
 */
@Composable
private fun NotesSegmentedTabs(
    selectedTab: NotesTab,
    onTabSelected: (NotesTab) -> Unit
) {
    val barShape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(barShape)
            .background(Color(0xFF0A0A0C))
            .border(0.5.dp, Color(0xFF1C1C20), barShape)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NotesTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val interactionSource = remember { MutableInteractionSource() }

                val tabBg by animateColorAsState(
                    targetValue = if (isSelected) Color(0x30FFFFFF) else Color.Transparent,
                    animationSpec = tween(180),
                    label = "tab_bg"
                )

                val tabBorder = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                } else null

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) DayTextPrimary else DayTextTertiary,
                    animationSpec = tween(180),
                    label = "tab_text_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tabBg)
                        .then(
                            if (tabBorder != null) {
                                Modifier.border(0.5.dp, tabBorder, RoundedCornerShape(12.dp))
                            } else Modifier
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        color = textColor,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Premium Note Card Item with Material Levels & Metadata
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PremiumNoteCard(
    note: Note,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "note_card_scale"
    )

    val containerColor = when {
        note.pinned -> Color(0xFF141418)
        isPressed -> Color(0xFF18181E)
        else -> Color(0xFF0B0B0D)
    }

    val borderColor = when {
        note.pinned -> Color.White.copy(alpha = 0.20f)
        isPressed -> Color.White.copy(alpha = 0.25f)
        else -> Color(0xFF1E1E24)
    }

    val relativeTime = remember(note.updatedAt) {
        formatRelativeTime(note.updatedAt)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .shadow(
                elevation = if (note.pinned) 8.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x66000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(0.75.dp, borderColor, RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onDelete
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Title + Optional "New" Badge + Pin Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    color = DayTextPrimary,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isNew = remember(note.createdAt) {
                        (System.currentTimeMillis() - note.createdAt) < 24 * 3600 * 1000L
                    }
                    if (isNew) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF222228))
                                .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "New",
                                color = DayTextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (note.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.pinned) "Unpin" else "Pin",
                            tint = if (note.pinned) DayTextPrimary else DayTextDisabled,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Body preview (multi-line snippet with clean line height)
            if (note.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = note.snippet(130),
                    color = DayTextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Tags (with '|' separator like reference "Design | Wireframe") + Date (YYYY/MM/DD)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (note.tags.isNotEmpty()) {
                        note.tags.take(3).forEachIndexed { index, tag ->
                            if (index > 0) {
                                Text(
                                    text = "|",
                                    color = DayTextDisabled,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp)
                                )
                            }
                            Text(
                                text = tag.replaceFirstChar { it.uppercase() },
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                color = DayTextTertiary
                            )
                        }
                    } else {
                        Text(
                            text = "General",
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = DayTextDisabled
                        )
                    }
                }

                val formattedDate = remember(note.updatedAt) {
                    SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(note.updatedAt))
                }
                Text(
                    text = formattedDate,
                    fontSize = 11.5.sp,
                    color = DayTextTertiary,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

/**
 * Categories Browser View (Matching Reference 2x2 Grid)
 */
@Composable
private fun CategoriesBrowserView(
    categories: List<NoteCategoryInfo>,
    onSelectCategory: (String) -> Unit,
    onCreateCategory: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Subheader: "List Categories" & "New ▾"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "List Categories",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = DayTextPrimary,
                fontFamily = FontFamily.SansSerif
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCreateCategory() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "+ New",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DayTextTertiary,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories, key = { it.name }) { category ->
                CategoryCard(
                    category = category,
                    onClick = { onSelectCategory(category.name) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(130.dp))
            }
        }
    }
}

/**
 * Category Card with Centered Folder Icon & Typography (Matching Reference Structure)
 */
@Composable
private fun CategoryCard(
    category: NoteCategoryInfo,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "cat_scale"
    )

    val icon: ImageVector = when (category.iconType) {
        "code" -> Icons.Default.Code
        "lightbulb" -> Icons.Default.Lightbulb
        "design" -> Icons.Default.GridView
        else -> Icons.Default.Folder
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(138.dp)
            .scale(scale)
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x66000000))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0F12))
            .border(0.75.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Centered Monochrome Folder / Category Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF18181E))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DayTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = category.name,
                color = DayTextPrimary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${category.count} Notes",
                color = DayTextTertiary,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Floating Glass "+" Add Note Button
 */
@Composable
private fun FloatingGlassAddNoteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "fab_scale"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .scale(scale)
            .shadow(16.dp, CircleShape, spotColor = Color(0xCC000000))
            .clip(CircleShape)
            .background(Color(0xF0121216), CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                CircleShape
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "New Note",
            tint = DayTextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Minimal Empty State for Notes
 */
@Composable
private fun NotesEmptyState(
    isSearching: Boolean,
    onCreateNote: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Default.Search else Icons.Default.FolderOpen,
                contentDescription = null,
                tint = DayTextDisabled,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isSearching) "No matching notes" else "Nothing here yet.",
                color = DayTextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isSearching) "Try a different search term" else "Create your first minimal note.",
                color = DayTextTertiary,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif
            )
            if (!isSearching) {
                Spacer(modifier = Modifier.height(18.dp))
                GlassButton(
                    text = "+ Create Note",
                    onClick = onCreateNote,
                    isPrimary = true
                )
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
