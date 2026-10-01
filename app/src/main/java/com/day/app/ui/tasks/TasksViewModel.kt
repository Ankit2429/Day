package com.day.app.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.day.app.DayApplication
import com.day.app.alarms.AlarmScheduler
import com.day.app.domain.model.Task
import com.day.app.domain.model.Urgency
import com.day.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TaskFilter(val title: String) {
    ALL("ALL"),
    TODAY("TODAY"),
    URGENT("URGENT"),
    COMPLETED("COMPLETED")
}

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val allTasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val searchQuery: String = "",
    val allCount: Int = 0,
    val todayCount: Int = 0,
    val urgentCount: Int = 0,
    val completedCount: Int = 0
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayApplication
    private val taskRepository = app.taskRepository
    private val espRepository = app.esp8266Repository

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<TasksUiState> = combine(
        taskRepository.getAllTasks(),
        _filter,
        _searchQuery
    ) { allTasks, filter, query ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis

        val allActive = allTasks.filter { !it.completed }
        val allCompleted = allTasks.filter { it.completed }
        val todayTasks = allActive.filter { task ->
            val time = task.scheduledTime ?: task.deadline ?: task.createdAt
            time in startOfDay..endOfDay
        }
        val urgentTasks = allActive.filter { it.urgency == Urgency.URGENT || it.urgency == Urgency.OVERDUE }

        val filteredByTab = when (filter) {
            TaskFilter.ALL -> allActive
            TaskFilter.TODAY -> todayTasks
            TaskFilter.URGENT -> urgentTasks
            TaskFilter.COMPLETED -> allCompleted
        }

        val finalTasks = if (query.isNotBlank()) {
            filteredByTab.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
        } else {
            filteredByTab
        }

        TasksUiState(
            tasks = finalTasks,
            allTasks = allTasks,
            filter = filter,
            searchQuery = query,
            allCount = allActive.size,
            todayCount = todayTasks.size,
            urgentCount = urgentTasks.size,
            completedCount = allCompleted.size
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TasksUiState()
    )

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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
            taskRepository.deleteTask(taskId)
            AlarmScheduler.cancelAlarm(getApplication(), taskId)
            val active = taskRepository.getActiveTasksSync()
            espRepository.syncActiveTasks(active)
            WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun snoozeTask(taskId: Long, hours: Int = 2) {
        viewModelScope.launch {
            val task = taskRepository.getTaskByIdSync(taskId) ?: return@launch
            val baseTime = task.scheduledTime ?: System.currentTimeMillis()
            val newTime = baseTime + (hours * 3600 * 1000L)
            val updated = task.copy(scheduledTime = newTime, updatedAt = System.currentTimeMillis())
            taskRepository.updateTask(updated)
            AlarmScheduler.scheduleTaskReminders(getApplication(), updated)
            WidgetUpdater.updateAllWidgets(getApplication())
        }
    }
}
