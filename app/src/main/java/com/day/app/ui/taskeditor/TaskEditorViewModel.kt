package com.day.app.ui.taskeditor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.day.app.DayApplication
import com.day.app.alarms.AlarmScheduler
import com.day.app.domain.model.Priority
import com.day.app.domain.model.ReminderEndCondition
import com.day.app.domain.model.RepeatType
import com.day.app.domain.model.Task
import com.day.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class TaskEditorUiState(
    val taskId: Long = 0L,
    val title: String = "",
    val description: String = "",
    val scheduledTime: Long? = null,
    val deadline: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val repeatType: RepeatType = RepeatType.NONE,
    val reminderEnabled: Boolean = true,
    val reminderInterval: Long? = null, // in minutes
    val reminderEndCondition: ReminderEndCondition = ReminderEndCondition.COMPLETED,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    val isEditMode: Boolean = false
)

class TaskEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayApplication
    private val taskRepository = app.taskRepository
    private val settingsRepository = app.settingsRepository
    private val espRepository = app.esp8266Repository

    private val _uiState = MutableStateFlow(
        TaskEditorUiState(
            priority = settingsRepository.getDefaultPriority(),
            reminderInterval = settingsRepository.getDefaultReminderMinutes()
        )
    )
    val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()

    fun loadTask(id: Long) {
        if (id <= 0L) return
        viewModelScope.launch {
            val task = taskRepository.getTaskByIdSync(id)
            if (task != null) {
                _uiState.value = TaskEditorUiState(
                    taskId = task.id,
                    title = task.title,
                    description = task.description,
                    scheduledTime = task.scheduledTime,
                    deadline = task.deadline,
                    priority = task.priority,
                    repeatType = task.repeatType,
                    reminderEnabled = task.reminderEnabled,
                    reminderInterval = task.reminderInterval,
                    reminderEndCondition = task.reminderEndCondition,
                    isEditMode = true
                )
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title, errorMessage = null)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateScheduledTime(time: Long?) {
        _uiState.value = _uiState.value.copy(scheduledTime = time, errorMessage = null)
    }

    fun updateDeadline(deadline: Long?) {
        _uiState.value = _uiState.value.copy(deadline = deadline, errorMessage = null)
    }

    fun updatePriority(priority: Priority) {
        _uiState.value = _uiState.value.copy(priority = priority)
    }

    fun updateRepeatType(repeatType: RepeatType) {
        _uiState.value = _uiState.value.copy(repeatType = repeatType)
    }

    fun updateReminderEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(reminderEnabled = enabled)
    }

    fun updateReminderInterval(intervalMinutes: Long?) {
        _uiState.value = _uiState.value.copy(reminderInterval = intervalMinutes)
    }

    fun updateReminderEndCondition(condition: ReminderEndCondition) {
        _uiState.value = _uiState.value.copy(reminderEndCondition = condition)
    }

    fun saveTask(onSuccess: () -> Unit) {
        val current = _uiState.value
        val trimmedTitle = current.title.trim()

        if (trimmedTitle.isEmpty()) {
            _uiState.value = current.copy(errorMessage = "Task title cannot be empty")
            return
        }

        // Validate: deadline cannot be before reminder time if both set
        if (current.scheduledTime != null && current.deadline != null && current.deadline < current.scheduledTime) {
            _uiState.value = current.copy(errorMessage = "Deadline cannot be earlier than reminder time")
            return
        }

        viewModelScope.launch {
            val task = Task(
                id = current.taskId,
                title = trimmedTitle,
                description = current.description.trim(),
                scheduledTime = current.scheduledTime,
                deadline = current.deadline,
                priority = current.priority,
                repeatType = current.repeatType,
                reminderEnabled = current.reminderEnabled,
                reminderInterval = current.reminderInterval,
                reminderEndCondition = current.reminderEndCondition
            )

            val savedId = taskRepository.saveTask(task)
            val updatedTask = task.copy(id = savedId)

            // Reschedule alarm
            AlarmScheduler.cancelAlarm(getApplication(), savedId)
            AlarmScheduler.scheduleTaskReminders(getApplication(), updatedTask)

            // Sync widgets & ESP8266
            WidgetUpdater.updateAllWidgets(getApplication())
            val active = taskRepository.getActiveTasksSync()
            espRepository.syncActiveTasks(active)

            _uiState.value = current.copy(isSaved = true)
            onSuccess()
        }
    }
}
