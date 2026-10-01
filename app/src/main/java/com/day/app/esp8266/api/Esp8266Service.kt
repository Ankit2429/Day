package com.day.app.esp8266.api

import com.day.app.esp8266.model.EspAlertPayload
import com.day.app.esp8266.model.EspCompletePayload
import com.day.app.esp8266.model.EspDisplayMode
import com.day.app.esp8266.model.EspModePayload
import com.day.app.esp8266.model.EspStatusResponse
import com.day.app.esp8266.model.EspTasksPayload
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class Esp8266Service {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .writeTimeout(2500, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getStatus(ip: String): Result<EspStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "http://${cleanIp(ip)}/status"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val data = gson.fromJson(body, EspStatusResponse::class.java)
                    Result.success(data ?: EspStatusResponse(status = "OK", ip = ip))
                } else {
                    Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendTasks(ip: String, payload: EspTasksPayload): Result<Unit> = withContext(Dispatchers.IO) {
        postJson("http://${cleanIp(ip)}/tasks", gson.toJson(payload))
    }

    suspend fun setMode(ip: String, mode: EspDisplayMode): Result<Unit> = withContext(Dispatchers.IO) {
        val payload = EspModePayload(mode = mode.code)
        postJson("http://${cleanIp(ip)}/mode", gson.toJson(payload))
    }

    suspend fun sendAlert(ip: String, title: String, urgency: String): Result<Unit> = withContext(Dispatchers.IO) {
        val payload = EspAlertPayload(title = title, urgency = urgency)
        postJson("http://${cleanIp(ip)}/task", gson.toJson(payload))
    }

    suspend fun sendComplete(ip: String, taskId: Long, title: String): Result<Unit> = withContext(Dispatchers.IO) {
        val payload = EspCompletePayload(taskId = taskId, title = title)
        postJson("http://${cleanIp(ip)}/complete", gson.toJson(payload))
    }

    suspend fun clearDisplay(ip: String): Result<Unit> = withContext(Dispatchers.IO) {
        postJson("http://${cleanIp(ip)}/clear", "{}")
    }

    private fun postJson(url: String, json: String): Result<Unit> {
        return try {
            val body = json.toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanIp(ip: String): String {
        return ip.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
    }
}
