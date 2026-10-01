package com.day.app.esp8266.model

import com.google.gson.annotations.SerializedName

data class EspStatusResponse(
    @SerializedName("type") val type: String? = "status",
    @SerializedName("device") val device: String? = "ESP8266",
    @SerializedName("connected") val connected: Boolean = true,
    @SerializedName("wifi") val wifi: Boolean = true,
    @SerializedName("ip") val ip: String? = null,
    @SerializedName("mode") val mode: String? = "CLOCK",
    @SerializedName("time") val time: String? = null,
    @SerializedName("time_synced") val timeSynced: Boolean? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("uptime") val uptime: Long? = null,
    @SerializedName("tasks") val activeTasks: Int? = null,
    @SerializedName("freeHeap") val freeHeap: Long? = null
)

data class EspTaskItem(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("row") val row: Int,          // 0 to 7 (row index on MAX7219)
    @SerializedName("leds") val ledCount: Int,     // 1 to 8 (number of LEDs lit on this row)
    @SerializedName("urgency") val urgency: String
)

data class EspTasksPayload(
    @SerializedName("mode") val mode: String = "TASK_PRIORITY",
    @SerializedName("tasks") val tasks: List<EspTaskItem>,
    @SerializedName("rowBytes") val rowBytes: List<Int> // 8 bytes (0-255) for direct matrix hardware rendering
)

data class EspModePayload(
    @SerializedName("mode") val mode: String
)

data class EspAlertPayload(
    @SerializedName("title") val title: String,
    @SerializedName("urgency") val urgency: String,
    @SerializedName("durationMs") val durationMs: Int = 5000
)

data class EspCompletePayload(
    @SerializedName("taskId") val taskId: Long,
    @SerializedName("title") val title: String
)
