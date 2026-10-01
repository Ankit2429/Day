package com.day.app.esp8266.api

import android.util.Log
import com.day.app.data.repository.SettingsRepository
import com.day.app.esp8266.model.EspStatusResponse
import com.day.app.esp8266.repository.ConnectionStatus
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * THE DAY — ESP8266 WebSocket Connection Engine
 *
 * Requirements:
 * - Target: ws://192.168.29.16:81
 * - Clear TAG: THE_DAY_ESP
 * - Handshake chain: onOpen -> status -> status response -> ping -> pong -> CONNECTION VERIFIED
 * - Backoff reconnect: 1s, 2s, 5s, 10s
 */
class Esp32ConnectionManager(
    private val settingsRepository: SettingsRepository,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    companion object {
        const val TAG = "THE_DAY_ESP"
        private val RECONNECT_DELAYS = listOf(1000L, 2000L, 5000L, 10000L)
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private var isUserInitiatedDisconnect = false

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _matrixAckFlow = MutableStateFlow<MatrixAckEvent?>(null)
    val matrixAckFlow: StateFlow<MatrixAckEvent?> = _matrixAckFlow.asStateFlow()

    private val _lastStatusResponse = MutableStateFlow<EspStatusResponse?>(null)
    val lastStatusResponse: StateFlow<EspStatusResponse?> = _lastStatusResponse.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(settingsRepository.getLastSyncTimestamp())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    // Verification latches for TEST CONNECTION flow
    val statusReceivedFlow = MutableStateFlow(false)
    val pongReceivedFlow = MutableStateFlow(false)

    fun connect(rawIp: String? = null) {
        val ip = (rawIp ?: settingsRepository.getEsp8266Ip()).trim()
        if (ip.isBlank()) {
            _connectionStatus.value = ConnectionStatus.OFFLINE
            return
        }

        isUserInitiatedDisconnect = false
        reconnectJob?.cancel()

        // Close any existing connection cleanly first
        webSocket?.close(1000, "Reconnecting")
        webSocket = null

        _connectionStatus.value = if (reconnectAttempt > 0) ConnectionStatus.RECONNECTING else ConnectionStatus.CONNECTING
        Log.d(TAG, "CONNECT_START $ip")

        val wsUrl = buildWsUrl(ip)
        Log.d(TAG, "CONNECT_URL $wsUrl")

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "ESP_CONNECTED $wsUrl")
                _connectionStatus.value = ConnectionStatus.CONNECTED
                reconnectAttempt = 0
                recordSync()

                // Immediately send status request
                requestStatus()

                // Start lightweight heartbeat (ping/pong every 15s)
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "CONNECT_MESSAGE: $text")
                recordSync()
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "CONNECT_CLOSING: code=$code, reason=$reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "ESP_DISCONNECTED code=$code, reason=$reason")
                stopHeartbeat()
                if (!isUserInitiatedDisconnect) {
                    _connectionStatus.value = ConnectionStatus.RECONNECTING
                    scheduleReconnect()
                } else {
                    _connectionStatus.value = ConnectionStatus.IDLE
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "ESP_DISCONNECTED failure=${t.message}", t)
                stopHeartbeat()
                if (!isUserInitiatedDisconnect) {
                    _connectionStatus.value = ConnectionStatus.RECONNECTING
                    scheduleReconnect()
                } else {
                    _connectionStatus.value = ConnectionStatus.OFFLINE
                }
            }
        })
    }

    fun disconnect() {
        Log.d(TAG, "CONNECT_CLOSED: user initiated disconnect")
        isUserInitiatedDisconnect = true
        reconnectJob?.cancel()
        stopHeartbeat()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionStatus.value = ConnectionStatus.IDLE
    }

    fun isConnected(): Boolean {
        return _connectionStatus.value == ConnectionStatus.CONNECTED && webSocket != null
    }

    // ── PROTOCOL SEND METHODS ──────────────────────────────────────────────────

    fun sendMessage(json: String): Boolean {
        val ws = webSocket
        if (ws != null && _connectionStatus.value == ConnectionStatus.CONNECTED) {
            Log.d(TAG, "SEND_MESSAGE: $json")
            val sent = ws.send(json)
            if (sent) {
                recordSync()
            }
            return sent
        }
        Log.w(TAG, "SEND_MESSAGE FAILED (not connected or ws null): $json")
        return false
    }

    fun requestStatus(): Boolean {
        return sendMessage("""{"type":"status"}""")
    }

    fun sendPing(): Boolean {
        Log.d(TAG, "PING_SENT: {\"type\":\"ping\"}")
        return sendMessage("""{"type":"ping"}""")
    }

    fun sendClock(): Boolean {
        return sendMessage("""{"type":"clock"}""")
    }

    fun sendClockSync(): Boolean {
        return sendMessage("""{"type":"clock_sync"}""")
    }

    fun sendClear(): Boolean {
        return sendMessage("""{"type":"clear"}""")
    }

    fun sendTest(): Boolean {
        return sendMessage("""{"type":"test"}""")
    }

    fun sendMatrix(rows: List<Int>): Boolean {
        val payload = JsonObject().apply {
            addProperty("type", "matrix")
            val arr = JsonArray()
            rows.forEach { arr.add(it) }
            add("rows", arr)
        }
        Log.d(TAG, "MATRIX_SEND $rows")
        return sendMessage(payload.toString())
    }

    fun sendBrightness(value: Int): Boolean {
        val clamped = value.coerceIn(0, 15)
        val payload = JsonObject().apply {
            addProperty("type", "brightness")
            addProperty("value", clamped)
        }
        return sendMessage(payload.toString())
    }

    fun sendUrgency(urgency: String): Boolean {
        val payload = JsonObject().apply {
            addProperty("type", "urgency")
            addProperty("urgency", urgency)
        }
        return sendMessage(payload.toString())
    }

    fun sendTask(
        title: String,
        time: String,
        status: String? = null,
        urgency: String? = null,
        hour: Int? = null,
        minute: Int? = null
    ): Boolean {
        val payload = JsonObject().apply {
            addProperty("type", "task")
            addProperty("title", title)
            addProperty("time", time)
            if (hour != null) addProperty("hour", hour)
            if (minute != null) addProperty("minute", minute)
            if (status != null) {
                addProperty("status", status)
            }
            if (urgency != null) {
                addProperty("urgency", urgency)
            }
        }
        return sendMessage(payload.toString())
    }

    fun sendAlert(
        title: String,
        time: String,
        urgency: String? = null,
        hour: Int? = null,
        minute: Int? = null
    ): Boolean {
        val payload = JsonObject().apply {
            addProperty("type", "alert")
            addProperty("title", title)
            addProperty("time", time)
            if (hour != null) addProperty("hour", hour)
            if (minute != null) addProperty("minute", minute)
            if (urgency != null) {
                addProperty("urgency", urgency)
            }
        }
        return sendMessage(payload.toString())
    }

    fun sendCustomText(text: String): Boolean {
        val payload = JsonObject().apply {
            addProperty("type", "custom")
            addProperty("text", text)
        }
        return sendMessage(payload.toString())
    }

    // ── COMPLETE TEST CONNECTION VERIFICATION FLOW ────────────────────────────

    suspend fun verifyConnection(timeoutMs: Long = 7000L): Result<String> {
        val ip = settingsRepository.getEsp8266Ip().trim()
        if (ip.isBlank()) return Result.failure(Exception("IP address is blank"))

        // Reset latches
        statusReceivedFlow.value = false
        pongReceivedFlow.value = false

        // 1. Connect if not currently connected
        if (!isConnected()) {
            connect(ip)
        }

        // Wait for onOpen
        val openOk = withTimeoutOrNull(timeoutMs) {
            while (!isConnected()) {
                delay(100)
            }
            true
        } ?: false

        if (!openOk) {
            val endpoint = buildWsUrl(ip)
            return Result.failure(Exception("Failed to open WebSocket at $endpoint\n\nVerify:\n• ESP8266 is powered on\n• Wi-Fi connection\n• Phone and ESP8266 on same network"))
        }

        // 2. Send status request
        requestStatus()

        // 3. Wait for status response
        val statusOk = withTimeoutOrNull(timeoutMs) {
            while (!statusReceivedFlow.value) {
                delay(100)
            }
            true
        } ?: false

        if (!statusOk) {
            return Result.failure(Exception("WebSocket connected, but ESP8266 did not return status response."))
        }

        // 4. Send ping
        sendPing()

        // 5. Wait for pong
        val pongOk = withTimeoutOrNull(timeoutMs) {
            while (!pongReceivedFlow.value) {
                delay(100)
            }
            true
        } ?: false

        if (!pongOk) {
            return Result.failure(Exception("Status received, but ESP8266 did not reply with pong."))
        }

        return Result.success("CONNECTION VERIFIED")
    }

    // ── INTERNAL HELPERS ───────────────────────────────────────────────────────

    private fun handleIncomingMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java) ?: return
            val type = json.get("type")?.asString ?: ""

            when (type.lowercase(Locale.US)) {
                "status" -> {
                    val device = json.get("device")?.asString ?: "ESP8266"
                    val connected = json.get("connected")?.asBoolean ?: true
                    val wifi = json.get("wifi")?.asBoolean ?: true
                    val ip = json.get("ip")?.asString ?: settingsRepository.getEsp8266Ip()
                    val mode = json.get("mode")?.asString ?: "CLOCK"
                    val time = json.get("time")?.asString
                    val timeSynced = json.get("time_synced")?.asBoolean
                    val status = json.get("status")?.asString ?: "OK"
                    val uptime = json.get("uptime")?.asLong
                    val tasks = json.get("tasks")?.asInt
                    val freeHeap = json.get("freeHeap")?.asLong

                    _lastStatusResponse.value = EspStatusResponse(
                        type = "status",
                        device = device,
                        connected = connected,
                        wifi = wifi,
                        ip = ip,
                        mode = mode,
                        time = time,
                        timeSynced = timeSynced,
                        status = status,
                        uptime = uptime,
                        activeTasks = tasks,
                        freeHeap = freeHeap
                    )
                    statusReceivedFlow.value = true
                }
                "pong" -> {
                    Log.d(TAG, "PONG_RECEIVED")
                    pongReceivedFlow.value = true
                    recordSync()
                }
                "matrix_ack" -> {
                    val success = json.get("success")?.asBoolean ?: true
                    val rowsList = mutableListOf<Int>()
                    val rowsArr = json.getAsJsonArray("rows")
                    if (rowsArr != null) {
                        for (element in rowsArr) {
                            rowsList.add(element.asInt)
                        }
                    }
                    Log.d(TAG, "MATRIX_ACK $rowsList")
                    _matrixAckFlow.value = MatrixAckEvent(success, rowsList)
                    recordSync()
                }
                "ack" -> {
                    recordSync()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse incoming JSON: ${e.message}")
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isConnected()) {
                delay(15000L) // Ping every 15 seconds
                if (isActive && isConnected()) {
                    sendPing()
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        val delayMs = RECONNECT_DELAYS.getOrElse(reconnectAttempt) { 10000L }
        reconnectAttempt = (reconnectAttempt + 1).coerceAtMost(RECONNECT_DELAYS.size)

        _connectionStatus.value = ConnectionStatus.RECONNECTING
        Log.d(TAG, "RECONNECTING in ${delayMs}ms (attempt $reconnectAttempt)")
        reconnectJob = scope.launch {
            delay(delayMs)
            if (isActive && !isUserInitiatedDisconnect && _connectionStatus.value != ConnectionStatus.CONNECTED) {
                connect()
            }
        }
    }

    private fun recordSync() {
        val now = System.currentTimeMillis()
        _lastSyncTimestamp.value = now
        settingsRepository.setLastSyncTimestamp(now)
    }

    fun buildWsUrl(rawIp: String): String {
        var clean = rawIp.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("ws://")
            .removePrefix("wss://")
            .removeSuffix("/")

        // Ensure port is present
        if (!clean.contains(":")) {
            clean = "$clean:81"
        }

        return "ws://$clean"
    }
}

data class MatrixAckEvent(
    val success: Boolean,
    val rows: List<Int>,
    val timestamp: Long = System.currentTimeMillis()
)
