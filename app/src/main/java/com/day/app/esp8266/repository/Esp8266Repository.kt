package com.day.app.esp8266.repository

import com.day.app.data.repository.SettingsRepository
import com.day.app.domain.model.Task
import com.day.app.domain.model.Urgency
import com.day.app.esp8266.api.Esp32ConnectionManager
import com.day.app.esp8266.api.Esp8266Service
import com.day.app.esp8266.model.EspDisplayMode
import com.day.app.esp8266.model.EspStatusResponse
import com.day.app.esp8266.model.EspTaskItem
import com.day.app.esp8266.model.EspTaskUrgency
import com.day.app.esp8266.model.EspTasksPayload
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.day.app.util.TimeFormatter

enum class ConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    OFFLINE
}

class Esp8266Repository(
    private val espService: Esp8266Service,
    private val settingsRepository: SettingsRepository,
    val esp32ConnectionManager: Esp32ConnectionManager
) {
    val connectionStatus: StateFlow<ConnectionStatus> = esp32ConnectionManager.connectionStatus
    val lastStatusResponse: StateFlow<EspStatusResponse?> = esp32ConnectionManager.lastStatusResponse
    val lastSyncTimestamp: StateFlow<Long> = esp32ConnectionManager.lastSyncTimestamp
    val matrixAck: StateFlow<com.day.app.esp8266.api.MatrixAckEvent?> = esp32ConnectionManager.matrixAckFlow

    fun connect(rawIp: String? = null) {
        esp32ConnectionManager.connect(rawIp)
    }

    fun disconnect() {
        esp32ConnectionManager.disconnect()
    }

    fun isConnected(): Boolean = esp32ConnectionManager.isConnected()

    suspend fun checkStatus(): Result<EspStatusResponse> {
        val ip = settingsRepository.getEsp8266Ip().trim()
        if (ip.isBlank()) {
            return Result.failure(IllegalArgumentException("IP address is blank"))
        }

        // If WebSocket is already connected, request status
        if (esp32ConnectionManager.isConnected()) {
            val sent = esp32ConnectionManager.requestStatus()
            val cached = esp32ConnectionManager.lastStatusResponse.value
            return if (sent && cached != null) {
                Result.success(cached)
            } else if (sent) {
                Result.success(
                    EspStatusResponse(
                        device = "ESP32",
                        connected = true,
                        wifi = true,
                        ip = ip,
                        mode = "CLOCK"
                    )
                )
            } else {
                Result.failure(Exception("Failed to send status request"))
            }
        }

        // Connect WebSocket
        esp32ConnectionManager.connect(ip)

        // Also test HTTP status as fallback/instant check
        val httpResult = espService.getStatus(ip)
        if (httpResult.isSuccess) {
            settingsRepository.setLastSyncTimestamp(System.currentTimeMillis())
            return httpResult
        }

        return if (esp32ConnectionManager.isConnected()) {
            Result.success(
                esp32ConnectionManager.lastStatusResponse.value
                    ?: EspStatusResponse(
                        device = "ESP32",
                        connected = true,
                        wifi = true,
                        ip = ip,
                        mode = "CLOCK"
                    )
            )
        } else {
            Result.failure(Exception("Could not connect to ESP32 at $ip"))
        }
    }

    suspend fun syncActiveTasks(tasks: List<Task>): Result<Unit> {
        if (!settingsRepository.isAutoSyncEspEnabled()) {
            return Result.success(Unit)
        }
        val ip = settingsRepository.getEsp8266Ip()

        // Filter incomplete tasks
        val activeTasks = tasks.filter { !it.completed }
        if (activeTasks.isEmpty()) {
            if (esp32ConnectionManager.isConnected()) {
                esp32ConnectionManager.sendClear()
            }
            return Result.success(Unit)
        }

        // Rank by urgency priority: Overdue/Ignored -> Due Now -> Approaching -> Normal
        val now = System.currentTimeMillis()
        val rankedTasks = activeTasks.map { task ->
            val urgency = EspTaskUrgency.calculate(
                scheduledTime = task.scheduledTime,
                deadline = task.deadline,
                reminderFiredTime = null,
                isCompleted = task.completed,
                now = now
            )
            Pair(task, urgency)
        }.sortedWith(
            compareByDescending<Pair<Task, EspTaskUrgency>> { it.second.ordinal }
                .thenByDescending { it.first.priority.ordinal }
                .thenBy { it.first.deadline ?: it.first.scheduledTime ?: Long.MAX_VALUE }
        )

        val topItem = rankedTasks.firstOrNull()
        if (topItem != null && esp32ConnectionManager.isConnected()) {
            val topTask = topItem.first
            val urgency = topItem.second
            val timeStr = formatTaskTime(topTask)
            esp32ConnectionManager.sendTask(
                title = topTask.title,
                time = timeStr,
                status = "active",
                urgency = urgency.wireValue
            )
        }

        val topTasks = rankedTasks.map { it.first }.take(8)

        val espTaskItems = mutableListOf<EspTaskItem>()
        val rowBytes = mutableListOf<Int>()

        for (row in 0 until 8) {
            if (row < topTasks.size) {
                val task = topTasks[row]
                val leds = task.matrixLedCount().coerceIn(1, 8)
                espTaskItems.add(
                    EspTaskItem(
                        id = task.id,
                        title = task.title,
                        row = row,
                        ledCount = leds,
                        urgency = task.urgency.name
                    )
                )
                val bitmask = (1 shl leds) - 1
                rowBytes.add(bitmask)
            } else {
                rowBytes.add(0)
            }
        }

        val payload = EspTasksPayload(
            mode = "TASK_PRIORITY",
            tasks = espTaskItems,
            rowBytes = rowBytes
        )

        return if (esp32ConnectionManager.isConnected()) {
            Result.success(Unit)
        } else {
            espService.sendTasks(ip, payload)
        }
    }

    suspend fun setDisplayMode(mode: EspDisplayMode): Result<Unit> {
        val ip = settingsRepository.getEsp8266Ip()
        if (esp32ConnectionManager.isConnected()) {
            when (mode) {
                EspDisplayMode.CLOCK -> esp32ConnectionManager.sendClockSync()
                EspDisplayMode.TASK_PRIORITY -> esp32ConnectionManager.sendTask("TASKS", "MODE", "upcoming")
                EspDisplayMode.REMINDER -> esp32ConnectionManager.sendAlert("REMINDER", "")
                EspDisplayMode.COMPLETED -> esp32ConnectionManager.sendTask("DONE", "100%", "completed")
                EspDisplayMode.IDLE, EspDisplayMode.CLEAR -> esp32ConnectionManager.sendClear()
            }
            return Result.success(Unit)
        }
        return espService.setMode(ip, mode)
    }

    suspend fun sendReminderAlert(
        title: String,
        urgency: Urgency,
        time: String? = null,
        hour: Int? = null,
        minute: Int? = null
    ): Result<Unit> {
        val ip = settingsRepository.getEsp8266Ip()
        val formattedTime = time ?: TimeFormatter.formatUserTime(System.currentTimeMillis())

        if (esp32ConnectionManager.isConnected()) {
            val sent = esp32ConnectionManager.sendAlert(
                title = title,
                time = formattedTime,
                hour = hour,
                minute = minute
            )
            if (sent) return Result.success(Unit)
        }

        return espService.sendAlert(ip, title, urgency.name)
    }

    suspend fun sendTaskCompletion(taskId: Long, title: String): Result<Unit> {
        val ip = settingsRepository.getEsp8266Ip()
        if (esp32ConnectionManager.isConnected()) {
            esp32ConnectionManager.sendClear()
            return Result.success(Unit)
        }
        return espService.sendComplete(ip, taskId, title)
    }

    suspend fun clearDisplay(): Result<Unit> {
        val ip = settingsRepository.getEsp8266Ip()
        if (esp32ConnectionManager.isConnected()) {
            val sent = esp32ConnectionManager.sendClear()
            if (sent) return Result.success(Unit)
        }
        return espService.clearDisplay(ip)
    }

    suspend fun verifyConnection(): Result<String> {
        return esp32ConnectionManager.verifyConnection()
    }

    fun sendTest(): Boolean {
        return if (esp32ConnectionManager.isConnected()) {
            esp32ConnectionManager.sendTest()
        } else {
            false
        }
    }

    fun sendClock(): Boolean {
        return if (esp32ConnectionManager.isConnected()) {
            esp32ConnectionManager.sendClock()
        } else {
            false
        }
    }

    fun sendTestTask(title: String = "ROBOTICS", time: String = "18:30"): Boolean {
        return if (esp32ConnectionManager.isConnected()) {
            esp32ConnectionManager.sendTask(title, time)
        } else {
            false
        }
    }

    fun sendTestAlert(title: String = "ROBOTICS", time: String = "18:30"): Boolean {
        return if (esp32ConnectionManager.isConnected()) {
            esp32ConnectionManager.sendAlert(title, time)
        } else {
            false
        }
    }

    fun sendTaskUpdate(task: Task, time: String? = null): Boolean {
        if (!esp32ConnectionManager.isConnected()) return false
        val timeStr = time ?: formatTaskTime(task)
        val timestamp = task.scheduledTime ?: task.deadline
        var hour: Int? = null
        var minute: Int? = null
        if (timestamp != null && timestamp > 0L) {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            hour = cal.get(Calendar.HOUR_OF_DAY)
            minute = cal.get(Calendar.MINUTE)
        }
        return esp32ConnectionManager.sendTask(
            title = task.title,
            time = timeStr,
            status = if (task.completed) "completed" else "upcoming",
            hour = hour,
            minute = minute
        )
    }

    fun sendMatrix(rows: List<Int>): Boolean {
        return if (esp32ConnectionManager.isConnected()) {
            esp32ConnectionManager.sendMatrix(rows)
        } else {
            false
        }
    }

    fun sendBrightness(value: Int): Boolean {
        settingsRepository.setEspBrightness(value)
        return if (esp32ConnectionManager.isConnected()) {
            esp32ConnectionManager.sendBrightness(value)
        } else {
            false
        }
    }

    fun getBrightness(): Int = settingsRepository.getEspBrightness()

    fun sendUrgency(urgency: EspTaskUrgency): Boolean {
        return if (esp32ConnectionManager.isConnected()) {
            esp32ConnectionManager.sendUrgency(urgency.wireValue)
        } else {
            false
        }
    }

    fun getSavedMatrixPatterns(): Map<String, List<Int>> {
        return settingsRepository.getSavedMatrixPatterns()
    }

    fun saveMatrixPattern(name: String, rows: List<Int>) {
        settingsRepository.saveMatrixPattern(name, rows)
    }

    fun deleteMatrixPattern(name: String) {
        settingsRepository.deleteMatrixPattern(name)
    }

    private fun formatTaskTime(task: Task): String {
        val timestamp = task.scheduledTime ?: task.deadline
        return if (timestamp != null && timestamp > 0L) {
            TimeFormatter.formatUserTime(timestamp)
        } else {
            TimeFormatter.formatUserTime(System.currentTimeMillis())
        }
    }
}
