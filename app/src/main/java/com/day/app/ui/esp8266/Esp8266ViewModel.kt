package com.day.app.ui.esp8266

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.day.app.DayApplication
import com.day.app.esp8266.model.EspStatusResponse
import com.day.app.esp8266.model.EspTaskUrgency
import com.day.app.esp8266.repository.ConnectionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MatrixTool {
    DRAW,
    ERASE
}

enum class DeviceMode(val wireValue: String) {
    CLOCK("CLOCK"),
    TASKS("TASKS"),
    ALERT("ALERT"),
    CUSTOM("CUSTOM")
}

data class Esp8266UiState(
    val ipAddress: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val lastSyncFormatted: String = "NEVER",
    val statusResponse: EspStatusResponse? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
    val isAutoSync: Boolean = true,
    // Matrix drawing state
    val matrixPixels: List<Boolean> = List(64) { false },
    val currentTool: MatrixTool = MatrixTool.DRAW,
    val brightness: Int = 8,
    val activeMode: DeviceMode = DeviceMode.CLOCK,
    val savedPatterns: Map<String, List<Int>> = emptyMap(),
    val runningClockFormatted: String = "",
    val isLiveSynced: Boolean = false
)

class Esp8266ViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayApplication
    private val espRepository = app.esp8266Repository
    private val settingsRepository = app.settingsRepository

    private var matrixSendJob: Job? = null
    private var lastMatrixSendTime = 0L
    private val THROTTLE_MS = 35L

    // Preset patterns
    val presetPatterns: Map<String, List<Int>> = mapOf(
        "Test Matrix" to listOf(255, 129, 165, 153, 153, 165, 129, 255),
        "Diamond" to listOf(24, 36, 66, 129, 66, 36, 24, 0),
        "Heart" to listOf(102, 255, 255, 255, 126, 60, 24, 0),
        "Smile" to listOf(60, 66, 165, 129, 165, 153, 66, 60),
        "Arrow" to listOf(24, 60, 126, 255, 24, 24, 24, 24),
        "Check" to listOf(0, 1, 3, 6, 140, 216, 112, 32),
        "Box" to listOf(255, 129, 129, 129, 129, 129, 129, 255)
    )

    private val _uiState = MutableStateFlow(
        Esp8266UiState(
            ipAddress = settingsRepository.getEsp8266Ip(),
            isAutoSync = settingsRepository.isAutoSyncEspEnabled(),
            lastSyncFormatted = formatTimestamp(settingsRepository.getLastSyncTimestamp()),
            brightness = settingsRepository.getEspBrightness(),
            savedPatterns = settingsRepository.getSavedMatrixPatterns(),
            runningClockFormatted = formatRunningClock()
        )
    )
    val uiState: StateFlow<Esp8266UiState> = _uiState.asStateFlow()

    init {
        // Realtime connection status updates
        var lastStatus = ConnectionStatus.IDLE
        viewModelScope.launch {
            espRepository.connectionStatus.collect { status ->
                val prev = lastStatus
                lastStatus = status

                _uiState.value = _uiState.value.copy(
                    connectionStatus = status,
                    isLiveSynced = if (status == ConnectionStatus.CONNECTED) _uiState.value.isLiveSynced else false,
                    errorMessage = if (status == ConnectionStatus.OFFLINE &&
                        (_uiState.value.connectionStatus == ConnectionStatus.CONNECTING ||
                         _uiState.value.connectionStatus == ConnectionStatus.RECONNECTING)
                    ) {
                        "DEVICE OFFLINE\n\nCheck:\n• ESP8266 power\n• Wi-Fi connection\n• ws://${_uiState.value.ipAddress}:81"
                    } else _uiState.value.errorMessage,
                    feedbackMessage = if (status == ConnectionStatus.RECONNECTING) {
                        "RECONNECTING..."
                    } else if (status == ConnectionStatus.CONNECTED && _uiState.value.feedbackMessage == "RECONNECTING...") {
                        "ESP8266 CONNECTED"
                    } else _uiState.value.feedbackMessage
                )

                // Requirement 16: Auto-resync when reconnecting so the drawing is never lost
                if (prev != ConnectionStatus.CONNECTED && status == ConnectionStatus.CONNECTED) {
                    if (_uiState.value.activeMode == DeviceMode.CUSTOM || _uiState.value.matrixPixels.any { it }) {
                        val rows = pixelsToRows(_uiState.value.matrixPixels)
                        Log.d("THE_DAY_ESP", "RECONNECT_RESYNC sending latest matrix $rows")
                        espRepository.sendMatrix(rows)
                    }
                }
            }
        }

        // Status response updates from ESP8266
        viewModelScope.launch {
            espRepository.lastStatusResponse.collect { resp ->
                if (resp != null) {
                    val modeEnum = when (resp.mode?.uppercase(Locale.ROOT)) {
                        "CUSTOM" -> DeviceMode.CUSTOM
                        "TASKS" -> DeviceMode.TASKS
                        "ALERT" -> DeviceMode.ALERT
                        else -> DeviceMode.CLOCK
                    }
                    _uiState.value = _uiState.value.copy(
                        statusResponse = resp,
                        activeMode = modeEnum,
                        errorMessage = null
                    )
                }
            }
        }

        // Matrix Acknowledgement listener (Requirement 14 & 15)
        viewModelScope.launch {
            espRepository.matrixAck.collect { ack ->
                if (ack != null && ack.success) {
                    val currentRows = pixelsToRows(_uiState.value.matrixPixels)
                    if (ack.rows == currentRows && _uiState.value.connectionStatus == ConnectionStatus.CONNECTED) {
                        _uiState.value = _uiState.value.copy(isLiveSynced = true)
                    }
                }
            }
        }

        // Sync timestamp updates
        viewModelScope.launch {
            espRepository.lastSyncTimestamp.collect { ts ->
                _uiState.value = _uiState.value.copy(
                    lastSyncFormatted = formatTimestamp(ts)
                )
            }
        }

        // Continuous local running clock ticker for Asia/Kolkata (Requirement 29)
        viewModelScope.launch {
            var lastClockLogged = ""
            while (isActive) {
                val formatted = formatRunningClock()
                if (formatted != lastClockLogged) {
                    lastClockLogged = formatted
                    Log.d("THE_DAY_ESP", "CLOCK_UPDATE $formatted")
                }
                _uiState.value = _uiState.value.copy(
                    runningClockFormatted = formatted
                )
                delay(1000)
            }
        }
    }

    // ── NETWORK CONNECTION CONTROLS ───────────────────────────────────────────

    fun updateIpAddress(ip: String) {
        val clean = ip.trim()
        settingsRepository.setEsp8266Ip(clean)
        _uiState.value = _uiState.value.copy(ipAddress = clean, errorMessage = null)
    }

    fun toggleAutoSync(enabled: Boolean) {
        settingsRepository.setAutoSyncEspEnabled(enabled)
        _uiState.value = _uiState.value.copy(isAutoSync = enabled)
    }

    fun connect() {
        val ip = _uiState.value.ipAddress.trim()
        if (ip.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter the ESP8266 IP address (e.g. 192.168.29.16)"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            feedbackMessage = "Connecting to ws://$ip:81..."
        )
        settingsRepository.setEsp8266Ip(ip)
        espRepository.connect(ip)
    }

    fun disconnect() {
        espRepository.disconnect()
        _uiState.value = _uiState.value.copy(
            feedbackMessage = "Disconnected from ESP8266",
            errorMessage = null
        )
    }

    fun testConnection() {
        viewModelScope.launch {
            val ip = _uiState.value.ipAddress.trim()
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "TESTING: onOpen -> status -> ping -> pong...",
                errorMessage = null
            )
            val result = espRepository.verifyConnection()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = "CONNECTION VERIFIED",
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = null,
                    errorMessage = result.exceptionOrNull()?.message ?: "Could not verify connection to ws://$ip:81"
                )
            }
        }
    }

    // ── HARDWARE DISPLAY COMMANDS ──────────────────────────────────────────────

    fun testDisplay() {
        viewModelScope.launch {
            val sent = espRepository.sendTest()
            if (sent) {
                _uiState.value = _uiState.value.copy(feedbackMessage = "TEST DISPLAY SENT")
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "ESP8266 not connected. Cannot send test display.")
            }
        }
    }

    fun sendClock() {
        viewModelScope.launch {
            val sent = espRepository.sendClock()
            if (sent) {
                _uiState.value = _uiState.value.copy(
                    activeMode = DeviceMode.CLOCK,
                    feedbackMessage = "CLOCK MODE ACTIVE"
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "ESP8266 not connected. Cannot switch to clock.")
            }
        }
    }

    fun sendClockSync() {
        viewModelScope.launch {
            val sent = espRepository.esp32ConnectionManager.sendClockSync()
            if (sent) {
                _uiState.value = _uiState.value.copy(feedbackMessage = "CLOCK SYNC SENT")
            }
        }
    }

    fun clearDisplay() {
        viewModelScope.launch {
            val result = espRepository.clearDisplay()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    activeMode = DeviceMode.CLOCK,
                    feedbackMessage = "CLEAR COMMAND SENT"
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to clear display.")
            }
        }
    }

    fun sendTestTask() {
        viewModelScope.launch {
            val sent = espRepository.sendTestTask("ROBOTICS", "18:30")
            if (sent) {
                _uiState.value = _uiState.value.copy(
                    activeMode = DeviceMode.TASKS,
                    feedbackMessage = "TEST TASK SENT (ROBOTICS @ 18:30)"
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "ESP8266 not connected. Cannot send task.")
            }
        }
    }

    fun sendTestAlert() {
        viewModelScope.launch {
            val sent = espRepository.sendTestAlert("ROBOTICS", "18:30")
            if (sent) {
                _uiState.value = _uiState.value.copy(
                    activeMode = DeviceMode.ALERT,
                    feedbackMessage = "TEST ALERT SENT (ROBOTICS @ 18:30)"
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "ESP8266 not connected. Cannot send alert.")
            }
        }
    }

    fun setDeviceMode(mode: DeviceMode) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(activeMode = mode)
            when (mode) {
                DeviceMode.CLOCK -> espRepository.sendClock()
                DeviceMode.TASKS -> espRepository.sendTestTask("STUDY", "18:30")
                DeviceMode.ALERT -> espRepository.sendTestAlert("STUDY", "18:30")
                DeviceMode.CUSTOM -> scheduleMatrixSync(immediate = true)
            }
        }
    }

    fun setBrightness(level: Int) {
        val clamped = level.coerceIn(0, 15)
        _uiState.value = _uiState.value.copy(brightness = clamped)
        viewModelScope.launch {
            espRepository.sendBrightness(clamped)
        }
    }

    // ── INTERACTIVE MATRIX DRAWING CONTROLS ────────────────────────────────────

    fun setTool(tool: MatrixTool) {
        _uiState.value = _uiState.value.copy(currentTool = tool)
    }

    fun togglePixel(row: Int, col: Int) {
        if (row !in 0..7 || col !in 0..7) return
        Log.d("THE_DAY_ESP", "MATRIX_TOUCH row=$row, col=$col, action=TOGGLE")
        Log.d("THE_DAY_ESP", "MATRIX_ROW $row")
        Log.d("THE_DAY_ESP", "MATRIX_COL $col")

        val index = row * 8 + col
        val current = _uiState.value.matrixPixels.toMutableList()
        current[index] = !current[index]
        _uiState.value = _uiState.value.copy(
            matrixPixels = current,
            activeMode = DeviceMode.CUSTOM,
            isLiveSynced = false
        )
        scheduleMatrixSync()
    }

    fun paintPixel(row: Int, col: Int, isDraw: Boolean) {
        if (row !in 0..7 || col !in 0..7) return
        val index = row * 8 + col
        val current = _uiState.value.matrixPixels.toMutableList()
        if (current[index] != isDraw) {
            Log.d("THE_DAY_ESP", "MATRIX_TOUCH row=$row, col=$col, action=${if (isDraw) "DRAW" else "ERASE"}")
            Log.d("THE_DAY_ESP", "MATRIX_ROW $row")
            Log.d("THE_DAY_ESP", "MATRIX_COL $col")

            current[index] = isDraw
            _uiState.value = _uiState.value.copy(
                matrixPixels = current,
                activeMode = DeviceMode.CUSTOM,
                isLiveSynced = false
            )
            scheduleMatrixSync()
        }
    }

    fun onMatrixTouchRelease() {
        // Guarantee final drawn frame is sent when finger lifts
        matrixSendJob?.cancel()
        matrixSendJob = null
        if (espRepository.isConnected()) {
            val rows = pixelsToRows(_uiState.value.matrixPixels)
            lastMatrixSendTime = System.currentTimeMillis()
            espRepository.sendMatrix(rows)
        }
    }

    fun clearMatrix() {
        _uiState.value = _uiState.value.copy(
            matrixPixels = List(64) { false },
            activeMode = DeviceMode.CUSTOM,
            isLiveSynced = false
        )
        scheduleMatrixSync(immediate = true)
    }

    fun invertMatrix() {
        val inverted = _uiState.value.matrixPixels.map { !it }
        _uiState.value = _uiState.value.copy(
            matrixPixels = inverted,
            activeMode = DeviceMode.CUSTOM,
            isLiveSynced = false
        )
        scheduleMatrixSync(immediate = true)
    }

    fun applyTestMatrix() {
        // Requirement 38: [255, 129, 165, 153, 153, 165, 129, 255]
        loadPattern(listOf(255, 129, 165, 153, 153, 165, 129, 255))
    }

    fun loadPattern(rows: List<Int>) {
        if (rows.size != 8) return
        val pixels = rowsToPixels(rows)
        _uiState.value = _uiState.value.copy(
            matrixPixels = pixels,
            activeMode = DeviceMode.CUSTOM,
            feedbackMessage = "PATTERN LOADED",
            isLiveSynced = false
        )
        scheduleMatrixSync(immediate = true)
    }

    fun saveCurrentPattern(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val rows = pixelsToRows(_uiState.value.matrixPixels)
        espRepository.saveMatrixPattern(clean, rows)
        _uiState.value = _uiState.value.copy(
            savedPatterns = espRepository.getSavedMatrixPatterns(),
            feedbackMessage = "PATTERN SAVED: $clean"
        )
    }

    fun deletePattern(name: String) {
        espRepository.deleteMatrixPattern(name)
        _uiState.value = _uiState.value.copy(
            savedPatterns = espRepository.getSavedMatrixPatterns(),
            feedbackMessage = "PATTERN REMOVED"
        )
    }

    private fun scheduleMatrixSync(immediate: Boolean = false) {
        val rows = pixelsToRows(_uiState.value.matrixPixels)
        Log.d("THE_DAY_ESP", "MATRIX_BITMAP $rows")

        if (!espRepository.isConnected()) {
            return
        }

        val now = System.currentTimeMillis()
        val timeSinceLast = now - lastMatrixSendTime

        if (immediate || timeSinceLast >= THROTTLE_MS) {
            matrixSendJob?.cancel()
            matrixSendJob = null
            lastMatrixSendTime = now
            espRepository.sendMatrix(rows)
        } else {
            if (matrixSendJob == null || !matrixSendJob!!.isActive) {
                matrixSendJob = viewModelScope.launch {
                    val waitTime = (THROTTLE_MS - (System.currentTimeMillis() - lastMatrixSendTime)).coerceAtLeast(5L)
                    delay(waitTime)
                    lastMatrixSendTime = System.currentTimeMillis()
                    val currentRows = pixelsToRows(_uiState.value.matrixPixels)
                    espRepository.sendMatrix(currentRows)
                }
            }
        }
    }

    // ── BITMAP MATRIX CONVERTERS ──────────────────────────────────────────────

    fun pixelsToRows(pixels: List<Boolean>): List<Int> {
        val rows = mutableListOf<Int>()
        for (r in 0 until 8) {
            var rowByte = 0
            for (c in 0 until 8) {
                val idx = r * 8 + c
                if (idx < pixels.size && pixels[idx]) {
                    rowByte = rowByte or (1 shl c)
                }
            }
            rows.add(rowByte)
        }
        return rows
    }

    fun rowsToPixels(rows: List<Int>): List<Boolean> {
        val pixels = BooleanArray(64)
        for (r in 0 until 8) {
            val rowByte = if (r < rows.size) rows[r] else 0
            for (c in 0 until 8) {
                pixels[r * 8 + c] = ((rowByte shr c) and 1) == 1
            }
        }
        return pixels.toList()
    }

    fun dismissFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun formatTimestamp(millis: Long): String {
        if (millis <= 0L) return "NEVER"
        val diff = System.currentTimeMillis() - millis
        return if (diff < 60_000L) {
            "just now"
        } else {
            com.day.app.util.TimeFormatter.formatUserTimeWithSeconds(millis)
        }
    }

    private fun formatRunningClock(): String {
        return com.day.app.util.TimeFormatter.formatUserTime(System.currentTimeMillis())
    }
}
