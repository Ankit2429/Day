package com.day.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.day.app.DayApplication
import com.day.app.domain.model.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true,
    val reminderEscalation: Boolean = false,
    val persistentCriticalAlerts: Boolean = true,
    val defaultSnoozeMinutes: Long = 10L,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 23,
    val quietHoursEnd: Int = 7,
    val defaultPriority: Priority = Priority.MEDIUM,
    val defaultReminderMinutes: Long = 15L,
    val esp8266Ip: String = "192.168.1.150",
    val autoSyncEsp: Boolean = true,
    val telegramServerUrl: String = "DEFAULT",
    val telegramChatId: String = "",
    val version: String = "DAY • V1.0.0"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayApplication
    private val settingsRepository = app.settingsRepository

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            notificationsEnabled = settingsRepository.isNotificationsEnabled(),
            notificationSound = settingsRepository.isNotificationSoundEnabled(),
            notificationVibration = settingsRepository.isNotificationVibrationEnabled(),
            reminderEscalation = settingsRepository.isReminderEscalationEnabled(),
            persistentCriticalAlerts = settingsRepository.isPersistentCriticalAlertsEnabled(),
            defaultSnoozeMinutes = settingsRepository.getDefaultSnoozeMinutes(),
            quietHoursEnabled = settingsRepository.isQuietHoursEnabled(),
            quietHoursStart = settingsRepository.getQuietHoursStart(),
            quietHoursEnd = settingsRepository.getQuietHoursEnd(),
            defaultPriority = settingsRepository.getDefaultPriority(),
            defaultReminderMinutes = settingsRepository.getDefaultReminderMinutes(),
            esp8266Ip = settingsRepository.getEsp8266Ip(),
            autoSyncEsp = settingsRepository.isAutoSyncEspEnabled(),
            telegramServerUrl = settingsRepository.getTelegramServerUrl(),
            telegramChatId = settingsRepository.getTelegramChatId()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleNotifications(enabled: Boolean) {
        settingsRepository.setNotificationsEnabled(enabled)
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }

    fun toggleNotificationSound(enabled: Boolean) {
        settingsRepository.setNotificationSoundEnabled(enabled)
        _uiState.value = _uiState.value.copy(notificationSound = enabled)
    }

    fun toggleNotificationVibration(enabled: Boolean) {
        settingsRepository.setNotificationVibrationEnabled(enabled)
        _uiState.value = _uiState.value.copy(notificationVibration = enabled)
    }

    fun toggleReminderEscalation(enabled: Boolean) {
        settingsRepository.setReminderEscalationEnabled(enabled)
        _uiState.value = _uiState.value.copy(reminderEscalation = enabled)
    }

    fun togglePersistentCriticalAlerts(enabled: Boolean) {
        settingsRepository.setPersistentCriticalAlertsEnabled(enabled)
        _uiState.value = _uiState.value.copy(persistentCriticalAlerts = enabled)
    }

    fun setDefaultSnoozeMinutes(minutes: Long) {
        settingsRepository.setDefaultSnoozeMinutes(minutes)
        _uiState.value = _uiState.value.copy(defaultSnoozeMinutes = minutes)
    }

    fun toggleQuietHours(enabled: Boolean) {
        settingsRepository.setQuietHoursEnabled(enabled)
        _uiState.value = _uiState.value.copy(quietHoursEnabled = enabled)
    }

    fun setDefaultPriority(priority: Priority) {
        settingsRepository.setDefaultPriority(priority)
        _uiState.value = _uiState.value.copy(defaultPriority = priority)
    }

    fun setDefaultReminder(minutes: Long) {
        settingsRepository.setDefaultReminderMinutes(minutes)
        _uiState.value = _uiState.value.copy(defaultReminderMinutes = minutes)
    }

    fun toggleAutoSyncEsp(enabled: Boolean) {
        settingsRepository.setAutoSyncEspEnabled(enabled)
        _uiState.value = _uiState.value.copy(autoSyncEsp = enabled)
    }

    fun updateTelegramServerUrl(url: String) {
        settingsRepository.setTelegramServerUrl(url)
        _uiState.value = _uiState.value.copy(telegramServerUrl = url)
    }

    fun updateTelegramChatId(chatId: String) {
        settingsRepository.setTelegramChatId(chatId)
        _uiState.value = _uiState.value.copy(telegramChatId = chatId)
    }

    fun updateEsp8266Ip(ip: String) {
        settingsRepository.setEsp8266Ip(ip)
        _uiState.value = _uiState.value.copy(esp8266Ip = ip)
    }
}
