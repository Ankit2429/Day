package com.day.app.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.day.app.DayApplication
import com.day.app.alarms.AlarmScheduler
import com.day.app.domain.model.Task
import com.day.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskDetailsUiState(
    val task: Task? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val hardwareSyncMessage: String? = null
)

class TaskDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayApplication
    private val taskRepository = app.taskRepository
    private val espRepository = app.esp8266Repository

    private val _uiState = MutableStateFlow(TaskDetailsUiState())
    val uiState: StateFlow<TaskDetailsUiState> = _uiState.asStateFlow()

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            val task = taskRepository.getTaskByIdSync(taskId)
            _uiState.value = TaskDetailsUiState(task = task, isLoading = false)
        }
    }

    fun toggleComplete(completed: Boolean) {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            val updated = taskRepository.completeTask(currentTask.id, completed)
            if (updated != null) {
                if (completed) {
                    AlarmScheduler.cancelAlarm(getApplication(), currentTask.id)
                    espRepository.sendTaskCompletion(currentTask.id, currentTask.title)
                } else {
                    AlarmScheduler.scheduleTaskReminders(getApplication(), updated)
                }
                val active = taskRepository.getActiveTasksSync()
                espRepository.syncActiveTasks(active)
                WidgetUpdater.updateAllWidgets(getApplication())

                _uiState.value = _uiState.value.copy(task = updated)
            }
        }
    }

    fun deleteTask(onSuccess: () -> Unit) {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), currentTask.id)
            taskRepository.deleteTask(currentTask.id)
            val active = taskRepository.getActiveTasksSync()
            espRepository.syncActiveTasks(active)
            WidgetUpdater.updateAllWidgets(getApplication())
            _uiState.value = _uiState.value.copy(isDeleted = true)
            onSuccess()
        }
    }

    fun testPushToEspMatrix() {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            val result = espRepository.sendReminderAlert(currentTask.title, currentTask.urgency)
            _uiState.value = _uiState.value.copy(
                hardwareSyncMessage = if (result.isSuccess) "FLASHED TO ESP8266 MATRIX" else "ESP8266 UNREACHABLE"
            )
        }
    }

    fun clearHardwareMessage() {
        _uiState.value = _uiState.value.copy(hardwareSyncMessage = null)
    }
}
