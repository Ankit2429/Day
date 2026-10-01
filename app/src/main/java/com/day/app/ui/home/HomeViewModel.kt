package com.day.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.day.app.DayApplication
import com.day.app.alarms.AlarmScheduler
import com.day.app.domain.model.Task
import com.day.app.domain.model.Urgency
import com.day.app.esp8266.repository.ConnectionStatus
import com.day.app.widget.WidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.day.app.util.TimeFormatter

data class HomeUiState(
    val todayDateFormatted: String = "",
    val currentTimeFormatted: String = "",
    val selectedDate: Long = System.currentTimeMillis(),
    val isViewingToday: Boolean = true,
    val selectedDateFormattedTitle: String = "TODAY",
    val currentMonthYearFormatted: String = "",
    val calendarDays: List<CalendarDay> = emptyList(),
    val timelineEntries: List<TimelineEntry> = emptyList(),
    val activeTasks: List<Task> = emptyList(),
    val urgentTasks: List<Task> = emptyList(),
    val upNextTasks: List<Task> = emptyList(),
    val upNextTask: Task? = null,
    val nextDeadlineTask: Task? = null,
    val documentCount: Int = 0,
    val noteCount: Int = 0,
    val espConnectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val isLoading: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayApplication
    private val taskRepository = app.taskRepository
    private val documentRepository = app.documentRepository
    private val noteRepository = app.noteRepository
    private val espRepository = app.esp8266Repository

    private val _currentTime = MutableStateFlow(getCurrentTime())
    private val _selectedDate = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    private val _currentYearMonth = MutableStateFlow(getCurrentYearMonth())

    init {
        // Periodic time refresh for home calendar/schedule calculation
        viewModelScope.launch {
            while (true) {
                _currentTime.value = getCurrentTime()
                delay(30000)
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        taskRepository.getAllTasks(),
        documentRepository.getDocumentCount(),
        noteRepository.getNoteCount(),
        espRepository.connectionStatus,
        _currentTime,
        _selectedDate,
        _currentYearMonth
    ) { args: Array<Any> ->
        @Suppress("UNCHECKED_CAST")
        val allTasks = args[0] as List<Task>
        val docCount = args[1] as Int
        val noteCount = args[2] as Int
        val espStatus = args[3] as ConnectionStatus
        val timeStr = args[4] as String
        val selectedDateMs = args[5] as Long
        @Suppress("UNCHECKED_CAST")
        val yearMonthPair = args[6] as Pair<Int, Int>
        val (year, month) = yearMonthPair

        val now = System.currentTimeMillis()
        val todayStart = getStartOfDay(now)
        val isViewingToday = isSameDay(selectedDateMs, now)

        val todayDateStr = SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(now)).uppercase()

        // Format selected date title: "TODAY · SAT 27" or "MON · 29 SEP"
        val selectedDateTitle = if (isViewingToday) {
            "TODAY · " + SimpleDateFormat("EEE d", Locale.getDefault()).format(Date(now)).uppercase()
        } else {
            SimpleDateFormat("EEE · d MMM", Locale.getDefault()).format(Date(selectedDateMs)).uppercase()
        }

        // Format calendar month/year
        val calMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val monthYearStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calMonth.time).uppercase()

        // 1. Build calendar days with event density dots
        val calendarDays = buildCalendarDays(year, month, selectedDateMs, now, allTasks)

        // 2. Build daily timeline entries
        val active = allTasks.filter { !it.completed }
        val urgent = active.filter { it.urgency == Urgency.URGENT || it.urgency == Urgency.OVERDUE }
        val upNext = active.filter { !urgent.contains(it) }.sortedBy { it.scheduledTime ?: it.deadline ?: Long.MAX_VALUE }

        val timelineEntries = buildTimelineEntries(selectedDateMs, isViewingToday, now, allTasks)

        // Next upcoming task for "UP NEXT" card
        val currentCal = Calendar.getInstance().apply { timeInMillis = now }
        val currentMinutes = currentCal.get(Calendar.HOUR_OF_DAY) * 60 + currentCal.get(Calendar.MINUTE)
        val upNextTask = if (isViewingToday) {
            active.filter {
                val t = it.scheduledTime ?: it.deadline
                if (t != null && isSameDay(t, now)) {
                    val taskCal = Calendar.getInstance().apply { timeInMillis = t }
                    val taskMins = taskCal.get(Calendar.HOUR_OF_DAY) * 60 + taskCal.get(Calendar.MINUTE)
                    taskMins >= currentMinutes - 15
                } else false
            }.minByOrNull { it.scheduledTime ?: it.deadline ?: Long.MAX_VALUE } ?: upNext.firstOrNull()
        } else {
            allTasks.filter {
                val t = it.scheduledTime ?: it.deadline
                t != null && isSameDay(t, selectedDateMs) && !it.completed
            }.minByOrNull { it.scheduledTime ?: it.deadline ?: Long.MAX_VALUE }
        }

        val nextDeadline = active.filter { it.deadline != null && it.deadline > now }
            .minByOrNull { it.deadline!! }

        HomeUiState(
            todayDateFormatted = todayDateStr,
            currentTimeFormatted = timeStr,
            selectedDate = selectedDateMs,
            isViewingToday = isViewingToday,
            selectedDateFormattedTitle = selectedDateTitle,
            currentMonthYearFormatted = monthYearStr,
            calendarDays = calendarDays,
            timelineEntries = timelineEntries,
            activeTasks = active,
            urgentTasks = urgent,
            upNextTasks = upNext.take(3),
            upNextTask = upNextTask,
            nextDeadlineTask = nextDeadline,
            documentCount = docCount,
            noteCount = noteCount,
            espConnectionStatus = espStatus,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState(
            todayDateFormatted = SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date()).uppercase(),
            currentTimeFormatted = getCurrentTime()
        )
    )

    fun selectDate(timestamp: Long) {
        _selectedDate.value = getStartOfDay(timestamp)
        // Automatically sync month if selected day belongs to another month
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val yr = cal.get(Calendar.YEAR)
        val mo = cal.get(Calendar.MONTH)
        if (_currentYearMonth.value != Pair(yr, mo)) {
            _currentYearMonth.value = Pair(yr, mo)
        }
    }

    fun nextMonth() {
        val (year, month) = _currentYearMonth.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, 1)
        }
        _currentYearMonth.value = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun previousMonth() {
        val (year, month) = _currentYearMonth.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, -1)
        }
        _currentYearMonth.value = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun goToToday() {
        val now = System.currentTimeMillis()
        selectDate(now)
    }

    fun toggleTaskComplete(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            val updated = taskRepository.completeTask(taskId, completed)
            if (updated != null) {
                if (completed) {
                    AlarmScheduler.cancelAlarm(getApplication(), taskId)
                    espRepository.sendTaskCompletion(taskId, updated.title)
                } else {
                    AlarmScheduler.scheduleTaskReminders(getApplication(), updated)
                }
                val active = taskRepository.getActiveTasksSync()
                espRepository.syncActiveTasks(active)
                WidgetUpdater.updateAllWidgets(getApplication())
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), taskId)
            taskRepository.deleteTask(taskId)
            val active = taskRepository.getActiveTasksSync()
            espRepository.syncActiveTasks(active)
            WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    private fun buildCalendarDays(
        year: Int,
        month: Int,
        selectedDateMs: Long,
        nowMs: Long,
        allTasks: List<Task>
    ): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Sunday = 1, Monday = 2 ... Saturday = 7 in Calendar
        // Map to Monday = 0, Tuesday = 1 ... Sunday = 6
        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

        // Prepend previous month days
        val prevCal = cal.clone() as Calendar
        prevCal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)
        for (i in 0 until firstDayOfWeek) {
            val ts = prevCal.timeInMillis
            val count = countTasksForDay(ts, allTasks)
            days.add(
                CalendarDay(
                    dayNumber = prevCal.get(Calendar.DAY_OF_MONTH),
                    timestamp = ts,
                    isCurrentMonth = false,
                    isSelected = isSameDay(ts, selectedDateMs),
                    isToday = isSameDay(ts, nowMs),
                    taskCount = count
                )
            )
            prevCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Current month days
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val ts = cal.timeInMillis
            val count = countTasksForDay(ts, allTasks)
            days.add(
                CalendarDay(
                    dayNumber = day,
                    timestamp = ts,
                    isCurrentMonth = true,
                    isSelected = isSameDay(ts, selectedDateMs),
                    isToday = isSameDay(ts, nowMs),
                    taskCount = count
                )
            )
        }

        // Postpend next month days to complete trailing row (multiples of 7)
        val remainder = days.size % 7
        if (remainder != 0) {
            val trailingCount = 7 - remainder
            val nextCal = cal.clone() as Calendar
            nextCal.set(Calendar.DAY_OF_MONTH, daysInMonth)
            for (i in 1..trailingCount) {
                nextCal.add(Calendar.DAY_OF_MONTH, 1)
                val ts = nextCal.timeInMillis
                val count = countTasksForDay(ts, allTasks)
                days.add(
                    CalendarDay(
                        dayNumber = nextCal.get(Calendar.DAY_OF_MONTH),
                        timestamp = ts,
                        isCurrentMonth = false,
                        isSelected = isSameDay(ts, selectedDateMs),
                        isToday = isSameDay(ts, nowMs),
                        taskCount = count
                    )
                )
            }
        }

        return days
    }

    private fun countTasksForDay(dayTimestamp: Long, allTasks: List<Task>): Int {
        var count = 0
        for (task in allTasks) {
            val target = task.scheduledTime ?: task.deadline
            if (target != null && isSameDay(target, dayTimestamp)) {
                count++
            }
        }
        return count
    }

    private fun buildTimelineEntries(
        selectedDateMs: Long,
        isViewingToday: Boolean,
        nowMs: Long,
        allTasks: List<Task>
    ): List<TimelineEntry> {
        val dayTasks = allTasks.filter { task ->
            val target = task.scheduledTime ?: task.deadline
            if (target != null) {
                isSameDay(target, selectedDateMs)
            } else {
                // If viewing today, show active unscheduled tasks in today's planner
                isViewingToday && !task.completed
            }
        }

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val currentMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

        val taskEntries = dayTasks.map { task ->
            val target = task.scheduledTime ?: task.deadline
            val (timeLabel, minutes) = if (target != null) {
                val tCal = Calendar.getInstance().apply { timeInMillis = target }
                val mins = tCal.get(Calendar.HOUR_OF_DAY) * 60 + tCal.get(Calendar.MINUTE)
                Pair(TimeFormatter.formatUserTime(target), mins)
            } else {
                Pair("Anytime", 1439)
            }

            val isPast = if (isViewingToday) {
                minutes < currentMinutes || task.completed
            } else {
                selectedDateMs < getStartOfDay(nowMs) || task.completed
            }

            val isCurrent = isViewingToday && !task.completed &&
                    minutes in (currentMinutes - 20)..(currentMinutes + 45)

            val isUpcoming = !isPast && !isCurrent

            TimelineEntry.TaskEntry(
                task = task,
                timeLabel = timeLabel,
                minutesFromMidnight = minutes,
                isCurrent = isCurrent,
                isPast = isPast,
                isUpcoming = isUpcoming
            )
        }.sortedBy { it.minutesFromMidnight }

        val result = mutableListOf<TimelineEntry>()
        var markerInserted = false

        for (entry in taskEntries) {
            if (isViewingToday && !markerInserted && entry.minutesFromMidnight > currentMinutes) {
                result.add(
                    TimelineEntry.CurrentTimeMarker(
                        timeLabel = TimeFormatter.formatUserTime(nowMs),
                        minutesFromMidnight = currentMinutes
                    )
                )
                markerInserted = true
            }
            result.add(entry)
        }

        if (isViewingToday && !markerInserted) {
            result.add(
                TimelineEntry.CurrentTimeMarker(
                    timeLabel = TimeFormatter.formatUserTime(nowMs),
                    minutesFromMidnight = currentMinutes
                )
            )
        }

        return result
    }

    private fun getCurrentYearMonth(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
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

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getCurrentTime(): String {
        return TimeFormatter.formatUserTime(System.currentTimeMillis())
    }
}
