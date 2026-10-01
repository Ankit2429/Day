package com.day.app.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.domain.model.Task
import com.day.app.domain.model.Urgency
import com.day.app.ui.components.AmbientGlassBackground
import com.day.app.ui.components.DayLogoMark
import com.day.app.ui.components.GlassButton
import com.day.app.ui.components.NeoInput
import com.day.app.ui.components.NeoTabItem
import com.day.app.ui.components.NeoTabRow
import com.day.app.ui.components.SwipeableTaskRow
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * THE DAY — TODO SCREEN (WEEK-BASED CONTINUOUS DAY STACK UI)
 *
 * Implements the reference-exact visual geometry and interaction:
 * - 0dp corner radius on day sections (rectangular continuous blocks)
 * - Tonal variation across days (Monday lighter dark gray down to Sunday)
 * - 0dp gap between days (subtle 0.5dp divider rgba(255,255,255,0.06))
 * - Vertical week paging: ONE SWIPE = ONE WEEK (snapping week-to-week)
 * - Tap to expand day inline: tasks unfold inside rectangular section
 * - Subtle glass highlight on selected/expanded day
 * - Auto-scroll / snap to Today
 */

private const val TOTAL_PAGES = 521
private const val INITIAL_PAGE = 260

// Tonal stepping from Monday down to Sunday
private val DayTones = listOf(
    Color(0xFF141417), // Monday (slightly lighter)
    Color(0xFF121215), // Tuesday
    Color(0xFF101013), // Wednesday
    Color(0xFF0E0E11), // Thursday
    Color(0xFF0C0C0F), // Friday
    Color(0xFF0A0A0D), // Saturday
    Color(0xFF08080A)  // Sunday (deepest dark)
)

data class WeeklyStackDay(
    val timestamp: Long,
    val dayOfWeekName: String,
    val dayOfMonth: Int,
    val monthShortName: String,
    val monthFullName: String,
    val year: Int,
    val formattedDate: String,
    val isToday: Boolean,
    val isPast: Boolean,
    val tasks: List<Task>,
    val completedCount: Int,
    val totalCount: Int
)

data class WeeklyStackWeek(
    val weekNumber: Int,
    val year: Int,
    val rangeLabel: String,
    val monthYearLabel: String,
    val days: List<WeeklyStackDay>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onNavigateToAddTask: (Long?) -> Unit,
    onNavigateToTaskDetails: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var isFilterVisible by rememberSaveable { mutableStateOf(false) }
    var isUnscheduledExpanded by rememberSaveable { mutableStateOf(false) }

    // Today's start of day timestamp
    val todayStartOfDay = remember { getStartOfDay(System.currentTimeMillis()) }

    // Start of Monday for current week (offset = 0)
    val currentMondayCal = remember {
        val todayCal = Calendar.getInstance()
        val dayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // Only one day expanded at a time (defaults to Today)
    var expandedDayTimestamp by rememberSaveable { mutableStateOf<Long?>(todayStartOfDay) }

    // Pager state for vertical snapping week navigation
    val pagerState = rememberPagerState(
        initialPage = INITIAL_PAGE,
        pageCount = { TOTAL_PAGES }
    )

    // When week page settles, reset expanded day appropriately (collapse previous week's expansion)
    var lastSettledPage by remember { mutableStateOf(INITIAL_PAGE) }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != lastSettledPage) {
            lastSettledPage = pagerState.settledPage
            expandedDayTimestamp = if (pagerState.settledPage == INITIAL_PAGE) {
                todayStartOfDay
            } else {
                null
            }
        }
    }

    // Unscheduled tasks (without scheduled date/deadline)
    val unscheduledTasks = remember(state.allTasks, state.filter, state.searchQuery) {
        state.allTasks.filter { it.scheduledTime == null && it.deadline == null }
            .filterTasks(state.filter, state.searchQuery)
    }

    AmbientGlassBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // ── TOP HEADER ─────────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DayLogoMark(size = 20.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DAY",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp,
                            color = DayTextPrimary
                        )
                        Text(
                            text = " · YOUR WEEK",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = DayTextTertiary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // "TODAY" Quick Snap Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF141414))
                                .border(0.5.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {
                                        expandedDayTimestamp = todayStartOfDay
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                page = INITIAL_PAGE,
                                                animationSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioNoBouncy
                                                )
                                            )
                                        }
                                    }
                                )
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "TODAY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DayTextPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Filter Toggle Button
                        IconButton(
                            onClick = { isFilterVisible = !isFilterVisible },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (state.filter != TaskFilter.ALL || isFilterVisible) DayTextPrimary else DayTextTertiary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Search Toggle Button
                        IconButton(
                            onClick = {
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible) viewModel.setSearchQuery("")
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchVisible || state.searchQuery.isNotBlank()) DayTextPrimary else DayTextTertiary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // ── OPTIONAL SEARCH BAR ──────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        NeoInput(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = "Search across weeks...",
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
                    }
                }

                // ── OPTIONAL FILTER PILLS ────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = isFilterVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val tabs = listOf(
                        NeoTabItem("ALL", state.allCount),
                        NeoTabItem("TODAY", state.todayCount),
                        NeoTabItem("URGENT", state.urgentCount),
                        NeoTabItem("DONE", state.completedCount)
                    )
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        NeoTabRow(
                            tabs = tabs,
                            selectedIndex = state.filter.ordinal,
                            onTabSelected = { viewModel.setFilter(TaskFilter.entries[it]) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── OPTIONAL UNSCHEDULED TASKS SECTION ───────────────────────────────────
                if (unscheduledTasks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        UnscheduledTasksStrip(
                            tasks = unscheduledTasks,
                            isExpanded = isUnscheduledExpanded,
                            onToggleExpand = { isUnscheduledExpanded = !isUnscheduledExpanded },
                            onTaskClick = onNavigateToTaskDetails,
                            onCompleteToggle = { id, done -> viewModel.toggleTaskComplete(id, done) },
                            onDelete = { id -> viewModel.deleteTask(id) },
                            onSnooze = { id -> viewModel.snoozeTask(id) },
                            onAddTask = { onNavigateToAddTask(null) }
                        )
                    }
                }

                // ── VERTICAL WEEK PAGER (ONE SWIPE = ONE WEEK) ───────────────────────────
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    beyondBoundsPageCount = 1
                ) { pageIndex ->
                    val weekOffset = pageIndex - INITIAL_PAGE
                    val week = remember(weekOffset, state.allTasks, state.filter, state.searchQuery) {
                        getWeekForOffset(
                            weekOffset = weekOffset,
                            currentMondayCal = currentMondayCal,
                            allTasks = state.allTasks,
                            filter = state.filter,
                            searchQuery = state.searchQuery
                        )
                    }

                    WeekPageView(
                        week = week,
                        expandedDayTimestamp = expandedDayTimestamp,
                        onDayClick = { day ->
                            expandedDayTimestamp = if (expandedDayTimestamp == day.timestamp) null else day.timestamp
                        },
                        onDayLongClick = { day ->
                            onNavigateToAddTask(day.timestamp)
                        },
                        onTaskClick = onNavigateToTaskDetails,
                        onCompleteToggle = { id, done -> viewModel.toggleTaskComplete(id, done) },
                        onDelete = { id -> viewModel.deleteTask(id) },
                        onSnooze = { id -> viewModel.snoozeTask(id) },
                        onAddTask = { timestamp -> onNavigateToAddTask(timestamp) }
                    )
                }
            }

            // ── TEMPORARY WEEK TRANSITION INDICATOR (Requirement 31) ─────────────────
            AnimatedVisibility(
                visible = pagerState.isScrollInProgress,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
            ) {
                val targetOffset = pagerState.targetPage - INITIAL_PAGE
                val targetWeek = remember(targetOffset, state.allTasks, state.filter, state.searchQuery) {
                    getWeekForOffset(targetOffset, currentMondayCal, state.allTasks, state.filter, state.searchQuery)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141416).copy(alpha = 0.94f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "WEEK ${targetWeek.weekNumber} · ${targetWeek.rangeLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DayTextPrimary,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Renders one complete week as a vertical continuous stack of 7 rectangular day sections.
 */
@Composable
private fun WeekPageView(
    week: WeeklyStackWeek,
    expandedDayTimestamp: Long?,
    onDayClick: (WeeklyStackDay) -> Unit,
    onDayLongClick: (WeeklyStackDay) -> Unit,
    onTaskClick: (Long) -> Unit,
    onCompleteToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onAddTask: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── WEEK HEADER ────────────────────────────────────────────────────────
        WeekHeader(week = week)

        // ── 7 CONTINUOUS RECTANGULAR DAYS (0dp corner radius) ─────────────────
        week.days.forEachIndexed { index, day ->
            val isExpanded = expandedDayTimestamp == day.timestamp

            DayRectangularSection(
                day = day,
                dayIndex = index,
                isExpanded = isExpanded,
                onHeaderClick = { onDayClick(day) },
                onHeaderLongClick = { onDayLongClick(day) },
                onTaskClick = onTaskClick,
                onCompleteToggle = onCompleteToggle,
                onDelete = onDelete,
                onSnooze = onSnooze,
                onAddTask = { onAddTask(day.timestamp) }
            )

            // Subtle divider between day sections
            if (index < week.days.size - 1) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.06f),
                    thickness = 0.5.dp
                )
            }
        }

        // Bottom spacing so content never collides with the floating bottom navigation
        Spacer(modifier = Modifier.height(130.dp))
    }
}

/**
 * Lightweight, non-card architectural week header
 */
@Composable
private fun WeekHeader(week: WeeklyStackWeek) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "WEEK ${week.weekNumber} · ${week.monthYearLabel}",
            color = DayTextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.8.sp
        )
        Text(
            text = week.rangeLabel,
            color = DayTextDisabled,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Rectangular Day Section in the Continuous Week Stack
 *
 * Rules:
 * - Corner radius = 0dp
 * - Full width
 * - Tonal variation down the week
 * - Selected/expanded day: subtle glass lifted state with translucent highlight
 * - Day Name: 28–32sp, clean modern sans-serif
 * - Date Subtitle: 13–14sp, soft gray
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayRectangularSection(
    day: WeeklyStackDay,
    dayIndex: Int,
    isExpanded: Boolean,
    onHeaderClick: () -> Unit,
    onHeaderLongClick: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onCompleteToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onAddTask: () -> Unit
) {
    val dayBaseColor = DayTones.getOrElse(dayIndex) { Color(0xFF0C0C0F) }

    // Subtle glass treatment for selected/expanded day
    val backgroundColor = if (isExpanded) {
        Color(0xFF18181D)
    } else {
        dayBaseColor
    }

    val dayNameColor = if (isExpanded) {
        Color.White
    } else if (day.isToday) {
        DayTextPrimary
    } else if (day.isPast && day.tasks.isEmpty()) {
        DayTextTertiary
    } else {
        Color(0xFFDCDCDC)
    }

    val accessibilityDesc = "${day.dayOfWeekName}, ${day.formattedDate}, ${day.tasks.size} tasks, ${if (isExpanded) "expanded" else "collapsed"}"

    val sectionModifier = Modifier
        .fillMaxWidth()
        .background(backgroundColor)
        .then(
            if (isExpanded) {
                Modifier.border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.14f)
                )
            } else {
                Modifier
            }
        )
        .combinedClickable(
            onClick = onHeaderClick,
            onLongClick = onHeaderLongClick
        )

    Box(
        modifier = sectionModifier
            .animateContentSize(
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
            .semantics { contentDescription = accessibilityDesc }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 22.dp,
                    end = 22.dp,
                    top = if (isExpanded) 20.dp else 16.dp,
                    bottom = if (isExpanded) 20.dp else 16.dp
                )
        ) {
            // ── DAY HEADER ROW ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = day.dayOfWeekName,
                        fontSize = 28.sp,
                        fontWeight = if (isExpanded) FontWeight.Medium else FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp,
                        color = dayNameColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Date & Task count subtitle
                    val subtitleText = buildString {
                        if (day.isToday) append("● TODAY · ")
                        append(day.formattedDate)
                        append(" · ")
                        if (day.totalCount == 0) {
                            append("NO TASKS")
                        } else if (day.totalCount == 1) {
                            append("1 TASK")
                        } else {
                            append("${day.totalCount} TASKS")
                        }
                        if (day.completedCount > 0) {
                            append(" · ${day.completedCount} DONE")
                        }
                    }

                    Text(
                        text = subtitleText,
                        fontSize = 13.sp,
                        color = if (day.isToday) Color(0xFFD0D0D0) else DayTextTertiary,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.5.sp
                    )
                }

                // Subtle expand/collapse indicator
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = if (isExpanded) DayTextPrimary else DayTextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // ── TASKS UNFOLD INSIDE RECTANGULAR DAY ───────────────────────────────
            if (isExpanded) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                )

                if (day.tasks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tasks for ${day.dayOfWeekName.lowercase().replaceFirstChar { it.uppercase() }}",
                            color = DayTextTertiary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .clickable { onAddTask() }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "+ Add Task",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DayTextPrimary
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        day.tasks.forEachIndexed { index, task ->
                            SwipeableTaskRow(
                                task = task,
                                onTaskClick = { onTaskClick(task.id) },
                                onCompleteToggle = { checked -> onCompleteToggle(task.id, checked) },
                                onDelete = { onDelete(task.id) },
                                onSnooze = { onSnooze(task.id) }
                            )

                            if (index < day.tasks.size - 1) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.05f),
                                    thickness = 0.5.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // + ADD TASK BUTTON
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                                .clickable { onAddTask() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ Add Task",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DayTextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact expandable strip for unscheduled tasks (tasks without due date)
 */
@Composable
private fun UnscheduledTasksStrip(
    tasks: List<Task>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onCompleteToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onAddTask: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F0F12))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .clickable { onToggleExpand() }
            .animateContentSize(
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "UNSCHEDULED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp,
                        color = DayTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "· ${tasks.size} ${if (tasks.size == 1) "TASK" else "TASKS"}",
                        fontSize = 12.sp,
                        color = DayTextTertiary
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = DayTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isExpanded) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.06f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
                )

                tasks.forEachIndexed { index, task ->
                    SwipeableTaskRow(
                        task = task,
                        onTaskClick = { onTaskClick(task.id) },
                        onCompleteToggle = { checked -> onCompleteToggle(task.id, checked) },
                        onDelete = { onDelete(task.id) },
                        onSnooze = { onSnooze(task.id) }
                    )

                    if (index < tasks.size - 1) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.05f),
                            thickness = 0.5.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                        .clickable { onAddTask() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Add Unscheduled Task",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DayTextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Builds the rolling weekly stack data for a given week offset from current week Monday.
 * Uses ISO-8601 Monday-first week calculations.
 */
private fun getWeekForOffset(
    weekOffset: Int,
    currentMondayCal: Calendar,
    allTasks: List<Task>,
    filter: TaskFilter,
    searchQuery: String
): WeeklyStackWeek {
    val dayFormat = SimpleDateFormat("EEEE", Locale.US)
    val monthShortFormat = SimpleDateFormat("MMM", Locale.US)
    val monthFullFormat = SimpleDateFormat("MMMM", Locale.US)
    val dateDisplayFormat = SimpleDateFormat("dd MMMM", Locale.US)
    val rangeDateFormat = SimpleDateFormat("dd MMM", Locale.US)

    val todayCal = Calendar.getInstance()
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)

    val weekMondayCal = (currentMondayCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, weekOffset * 7)
    }

    val weekNum = weekMondayCal.get(Calendar.WEEK_OF_YEAR)
    val weekYear = weekMondayCal.get(Calendar.YEAR)

    val days = ArrayList<WeeklyStackDay>(7)
    var mondayRangeStr = ""
    var sundayRangeStr = ""
    var monthYearLabel = ""

    for (dayIndex in 0 until 7) {
        val dayCal = (weekMondayCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, dayIndex)
        }

        val dayTimestamp = dayCal.timeInMillis
        val dayOfWeekName = dayFormat.format(dayCal.time).uppercase(Locale.US)
        val dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH)
        val monthShort = monthShortFormat.format(dayCal.time).uppercase(Locale.US)
        val monthFull = monthFullFormat.format(dayCal.time).uppercase(Locale.US)
        val year = dayCal.get(Calendar.YEAR)
        val formattedDate = dateDisplayFormat.format(dayCal.time).uppercase(Locale.US)

        val isToday = (dayCal.get(Calendar.YEAR) == todayYear && dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear)
        val isPast = dayCal.before(todayCal) && !isToday

        if (dayIndex == 0) {
            mondayRangeStr = rangeDateFormat.format(dayCal.time).uppercase(Locale.US)
            monthYearLabel = "$monthFull $year"
        }
        if (dayIndex == 6) {
            sundayRangeStr = rangeDateFormat.format(dayCal.time).uppercase(Locale.US)
        }

        // Matching tasks for this day
        val dayTasks = allTasks.filter { task ->
            val time = task.scheduledTime ?: task.deadline
            if (time != null) {
                val taskCal = Calendar.getInstance().apply { timeInMillis = time }
                taskCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                        taskCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
            } else {
                false
            }
        }.filterTasks(filter, searchQuery)
            .sortedWith(
                compareBy<Task> { it.completed }
                    .thenBy { it.scheduledTime ?: it.deadline ?: Long.MAX_VALUE }
            )

        days.add(
            WeeklyStackDay(
                timestamp = dayTimestamp,
                dayOfWeekName = dayOfWeekName,
                dayOfMonth = dayOfMonth,
                monthShortName = monthShort,
                monthFullName = monthFull,
                year = year,
                formattedDate = formattedDate,
                isToday = isToday,
                isPast = isPast,
                tasks = dayTasks,
                completedCount = dayTasks.count { it.completed },
                totalCount = dayTasks.size
            )
        )
    }

    val rangeLabel = "$mondayRangeStr — $sundayRangeStr"
    return WeeklyStackWeek(
        weekNumber = weekNum,
        year = weekYear,
        rangeLabel = rangeLabel,
        monthYearLabel = monthYearLabel,
        days = days
    )
}

/**
 * Filter and search helper for tasks
 */
private fun List<Task>.filterTasks(filter: TaskFilter, query: String): List<Task> {
    val filtered = when (filter) {
        TaskFilter.ALL -> this
        TaskFilter.TODAY -> this.filter { !it.completed }
        TaskFilter.URGENT -> this.filter { !it.completed && (it.urgency == Urgency.URGENT || it.urgency == Urgency.OVERDUE) }
        TaskFilter.COMPLETED -> this.filter { it.completed }
    }

    return if (query.isNotBlank()) {
        filtered.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
        }
    } else {
        filtered
    }
}

private fun getStartOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
