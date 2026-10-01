package com.day.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.day.app.domain.model.Priority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("day_settings_prefs", Context.MODE_PRIVATE)

    private val _espIpFlow = MutableStateFlow(getEsp8266Ip())
    val espIpFlow: Flow<String> = _espIpFlow.asStateFlow()

    fun getEsp8266Ip(): String {
        val saved = prefs.getString("KEY_ESP_IP", null)
        if (saved.isNullOrBlank() || saved == "192.168.1.150" || saved == "192.168.1.100" || saved == "192.168.1.105") {
            return "192.168.29.16"
        }
        return saved
    }

    fun setEsp8266Ip(ip: String) {
        val clean = ip.trim()
        prefs.edit().putString("KEY_ESP_IP", clean).apply()
        _espIpFlow.value = clean
    }

    fun getDefaultPriority(): Priority {
        val name = prefs.getString("KEY_DEFAULT_PRIORITY", Priority.MEDIUM.name)
        return Priority.fromString(name)
    }

    fun setDefaultPriority(priority: Priority) {
        prefs.edit().putString("KEY_DEFAULT_PRIORITY", priority.name).apply()
    }

    fun getDefaultReminderMinutes(): Long {
        return prefs.getLong("KEY_DEFAULT_REMINDER_MINUTES", 15L)
    }

    fun setDefaultReminderMinutes(minutes: Long) {
        prefs.edit().putLong("KEY_DEFAULT_REMINDER_MINUTES", minutes).apply()
    }

    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean("KEY_NOTIFICATIONS_ENABLED", true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("KEY_NOTIFICATIONS_ENABLED", enabled).apply()
    }

    fun isNotificationSoundEnabled(): Boolean {
        return prefs.getBoolean("KEY_NOTIFICATION_SOUND_ENABLED", true)
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("KEY_NOTIFICATION_SOUND_ENABLED", enabled).apply()
    }

    fun isNotificationVibrationEnabled(): Boolean {
        return prefs.getBoolean("KEY_NOTIFICATION_VIBRATION_ENABLED", true)
    }

    fun setNotificationVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("KEY_NOTIFICATION_VIBRATION_ENABLED", enabled).apply()
    }

    fun isReminderEscalationEnabled(): Boolean {
        return prefs.getBoolean("KEY_REMINDER_ESCALATION_ENABLED", false)
    }

    fun setReminderEscalationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("KEY_REMINDER_ESCALATION_ENABLED", enabled).apply()
    }

    fun isPersistentCriticalAlertsEnabled(): Boolean {
        return prefs.getBoolean("KEY_PERSISTENT_CRITICAL_ALERTS", true)
    }

    fun setPersistentCriticalAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("KEY_PERSISTENT_CRITICAL_ALERTS", enabled).apply()
    }

    fun getDefaultSnoozeMinutes(): Long {
        return prefs.getLong("KEY_DEFAULT_SNOOZE_MINUTES", 10L)
    }

    fun setDefaultSnoozeMinutes(minutes: Long) {
        prefs.edit().putLong("KEY_DEFAULT_SNOOZE_MINUTES", minutes).apply()
    }

    fun isQuietHoursEnabled(): Boolean {
        return prefs.getBoolean("KEY_QUIET_HOURS_ENABLED", false)
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("KEY_QUIET_HOURS_ENABLED", enabled).apply()
    }

    fun getQuietHoursStart(): Int {
        return prefs.getInt("KEY_QUIET_HOURS_START", 23)
    }

    fun setQuietHoursStart(hour: Int) {
        prefs.edit().putInt("KEY_QUIET_HOURS_START", hour).apply()
    }

    fun getQuietHoursEnd(): Int {
        return prefs.getInt("KEY_QUIET_HOURS_END", 7)
    }

    fun setQuietHoursEnd(hour: Int) {
        prefs.edit().putInt("KEY_QUIET_HOURS_END", hour).apply()
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong("KEY_LAST_SYNC_TIMESTAMP", 0L)
    }

    fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong("KEY_LAST_SYNC_TIMESTAMP", timestamp).apply()
    }

    fun isAutoSyncEspEnabled(): Boolean {
        return prefs.getBoolean("KEY_AUTO_SYNC_ESP", true)
    }

    fun setAutoSyncEspEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("KEY_AUTO_SYNC_ESP", enabled).apply()
    }

    fun getTelegramServerUrl(): String {
        return prefs.getString("KEY_TELEGRAM_SERVER_URL", "DEFAULT") ?: "DEFAULT"
    }

    fun setTelegramServerUrl(url: String) {
        prefs.edit().putString("KEY_TELEGRAM_SERVER_URL", url.trim()).apply()
    }

    fun getTelegramChatId(): String {
        return prefs.getString("KEY_TELEGRAM_CHAT_ID", "") ?: ""
    }

    fun setTelegramChatId(chatId: String) {
        prefs.edit().putString("KEY_TELEGRAM_CHAT_ID", chatId.trim()).apply()
    }

    fun getEspBrightness(): Int {
        return prefs.getInt("KEY_ESP_BRIGHTNESS", 8)
    }

    fun setEspBrightness(value: Int) {
        prefs.edit().putInt("KEY_ESP_BRIGHTNESS", value.coerceIn(0, 15)).apply()
    }

    fun getSavedMatrixPatterns(): Map<String, List<Int>> {
        val raw = prefs.getString("KEY_SAVED_PATTERNS", null) ?: return emptyMap()
        val result = mutableMapOf<String, List<Int>>()
        try {
            // Format: name1=r0,r1..r7;name2=r0,r1..r7
            raw.split(";").forEach { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) {
                    val name = parts[0]
                    val rows = parts[1].split(",").mapNotNull { it.toIntOrNull() }
                    if (rows.size == 8) {
                        result[name] = rows
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    fun saveMatrixPattern(name: String, rows: List<Int>) {
        if (rows.size != 8) return
        val current = getSavedMatrixPatterns().toMutableMap()
        current[name.trim()] = rows
        val serialized = current.entries.joinToString(";") { "${it.key}=${it.value.joinToString(",")}" }
        prefs.edit().putString("KEY_SAVED_PATTERNS", serialized).apply()
    }

    fun deleteMatrixPattern(name: String) {
        val current = getSavedMatrixPatterns().toMutableMap()
        current.remove(name)
        val serialized = current.entries.joinToString(";") { "${it.key}=${it.value.joinToString(",")}" }
        prefs.edit().putString("KEY_SAVED_PATTERNS", serialized).apply()
    }
}
