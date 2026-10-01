package com.day.app.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.components.AmbientGlassBackground
import com.day.app.ui.components.GlassButton
import com.day.app.ui.components.NeoInput
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * THE DAY — NOTE EDITOR & DETAIL SCREEN
 *
 * Designed as a clean, distraction-free document canvas:
 * - Direct writing surface on deep black background (no cluttered cards)
 * - Large architectural document title
 * - Floating glass formatting toolbar (B, I, U, list, quote, link)
 * - Save feedback pill ("✓ Saved")
 * - Tag / Category metadata pill
 */
@Composable
fun NoteEditorScreen(
    noteId: Long = 0L,
    viewModel: NoteEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showSavedFeedback by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId > 0L) {
            viewModel.loadNote(noteId)
        }
    }

    LaunchedEffect(showSavedFeedback) {
        if (showSavedFeedback) {
            delay(1600)
            showSavedFeedback = false
        }
    }

    AmbientGlassBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp)
            ) {
                // ── 1. PINNED TOP BAR (Always Accessible) ─────────────────────────────
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DayTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center Saved Feedback or Document Title Label
                    AnimatedVisibility(
                        visible = showSavedFeedback,
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut() + slideOutVertically { -it / 2 }
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF16161A))
                                .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = DayTextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Saved",
                                color = DayTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    // Actions Row: Pin toggle, Delete, Save Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pin Toggle Pill
                        IconButton(
                            onClick = { viewModel.togglePin(!state.pinned) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (state.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (state.pinned) "Unpin" else "Pin",
                                tint = if (state.pinned) DayTextPrimary else DayTextTertiary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        if (state.isEditMode) {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = DayTextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        GlassButton(
                            text = "Save",
                            onClick = {
                                viewModel.saveNote(onSuccess = {
                                    showSavedFeedback = true
                                    onNavigateBack()
                                })
                            },
                            isPrimary = true,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 2. SCROLLABLE DOCUMENT CANVAS ─────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                // ── 2. EDITABLE DOCUMENT TITLE ────────────────────────────────────────
                BasicTextField(
                    value = state.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = DayTextPrimary,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 32.sp
                    ),
                    cursorBrush = SolidColor(DayTextPrimary),
                    decorationBox = { innerTextField ->
                        if (state.title.isEmpty()) {
                            Text(
                                text = "Untitled Note",
                                color = DayTextDisabled,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                lineHeight = 32.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ── 3. METADATA & CATEGORIES ROW ──────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormatted = remember {
                        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
                    }
                    Text(
                        text = dateFormatted,
                        color = DayTextTertiary,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Tags / Category Input Field
                    NeoInput(
                        value = state.tagsString,
                        onValueChange = { viewModel.updateTags(it) },
                        placeholder = "Tags (e.g. Robotics, Ideas)...",
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── 4. DOCUMENT BODY (DISTRACTION-FREE CANVAS) ────────────────────────
                BasicTextField(
                    value = state.body,
                    onValueChange = { viewModel.updateBody(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 350.dp),
                    textStyle = TextStyle(
                        color = DayTextPrimary,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(DayTextPrimary),
                    decorationBox = { innerTextField ->
                        if (state.body.isEmpty()) {
                            Text(
                                text = "Start typing your thoughts, ideas, code notes, or meeting decisions...",
                                color = DayTextDisabled,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )
                        }
                        innerTextField()
                    }
                )

                // Bottom padding for floating formatting toolbar
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // ── 5. FLOATING GLASS FORMATTING TOOLBAR ──────────────────────────────────
            FloatingGlassFormattingToolbar(
                onBold = {
                    viewModel.updateBody("${state.body} **bold**")
                },
                onItalic = {
                    viewModel.updateBody("${state.body} *italic*")
                },
                onUnderline = {
                    viewModel.updateBody("${state.body} <u>underline</u>")
                },
                onList = {
                    viewModel.updateBody(if (state.body.isEmpty()) "- " else "${state.body}\n- ")
                },
                onQuote = {
                    viewModel.updateBody(if (state.body.isEmpty()) "> " else "${state.body}\n> ")
                },
                onLink = {
                    viewModel.updateBody("${state.body} [title](https://)")
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete Note",
                    color = DayTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this note?",
                    color = DayTextSecondary
                )
            },
            confirmButton = {
                GlassButton(
                    text = "Delete",
                    onClick = {
                        viewModel.deleteNote(onSuccess = onNavigateBack)
                        showDeleteConfirm = false
                    },
                    isPrimary = true
                )
            },
            dismissButton = {
                GlassButton(
                    text = "Cancel",
                    onClick = { showDeleteConfirm = false }
                )
            },
            containerColor = Color(0xFF141416),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Floating Glass Formatting Toolbar (Reference Structure)
 * ┌────────────────────────────────┐
 * │  B    I    U    ≡    ●    🔗   │
 * └────────────────────────────────┘
 */
@Composable
private fun FloatingGlassFormattingToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onList: () -> Unit,
    onQuote: () -> Unit,
    onLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = barShape,
                spotColor = Color(0xAA000000),
                ambientColor = Color(0x55000000)
            )
            .clip(barShape)
            .background(Color(0xF0101014), barShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                barShape
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = barShape
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FormatToolbarButton(
                icon = Icons.Default.FormatBold,
                label = "B",
                onClick = onBold
            )
            FormatToolbarButton(
                icon = Icons.Default.FormatItalic,
                label = "I",
                onClick = onItalic
            )
            FormatToolbarButton(
                icon = Icons.Default.FormatUnderlined,
                label = "U",
                onClick = onUnderline
            )
            FormatToolbarButton(
                icon = Icons.Default.FormatAlignLeft,
                label = "Align",
                onClick = onList
            )
            FormatToolbarButton(
                icon = Icons.Default.Circle,
                label = "Bullet",
                onClick = onQuote
            )
            FormatToolbarButton(
                icon = Icons.Default.Link,
                label = "Link",
                onClick = onLink
            )
        }
    }
}

@Composable
private fun FormatToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "btn_scale"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) Color(0x33FFFFFF) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = DayTextPrimary,
            modifier = Modifier.size(19.dp)
        )
    }
}
