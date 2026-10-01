package com.day.app

import android.app.Application
import com.day.app.data.local.DayDatabase
import com.day.app.data.repository.DocumentRepository
import com.day.app.data.repository.DocumentRepositoryImpl
import com.day.app.data.repository.NoteRepository
import com.day.app.data.repository.NoteRepositoryImpl
import com.day.app.data.repository.SettingsRepository
import com.day.app.data.repository.TaskRepository
import com.day.app.data.repository.TaskRepositoryImpl
import com.day.app.data.storage.LocalStorageProvider
import com.day.app.data.storage.TelegramStorageProvider
import com.day.app.esp8266.api.Esp8266Service
import com.day.app.esp8266.repository.Esp8266Repository
import com.day.app.notifications.NotificationChannels
import kotlinx.coroutines.launch

class DayApplication : Application() {

    lateinit var database: DayDatabase
        private set

    lateinit var taskRepository: TaskRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var localStorageProvider: LocalStorageProvider
        private set

    lateinit var telegramStorageProvider: TelegramStorageProvider
        private set

    lateinit var documentRepository: DocumentRepository
        private set

    lateinit var noteRepository: NoteRepository
        private set

    lateinit var esp8266Service: Esp8266Service
        private set

    lateinit var esp32ConnectionManager: com.day.app.esp8266.api.Esp32ConnectionManager
        private set

    lateinit var esp8266Repository: Esp8266Repository
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Notification Channels
        NotificationChannels.createChannels(this)

        // 2. Initialize Database & Repositories
        database = DayDatabase.getInstance(this)
        taskRepository = TaskRepositoryImpl(database.taskDao())
        settingsRepository = SettingsRepository(this)
        localStorageProvider = LocalStorageProvider(this)
        telegramStorageProvider = TelegramStorageProvider(settingsRepository, localStorageProvider)
        documentRepository = DocumentRepositoryImpl(database.documentDao(), telegramStorageProvider, this)
        noteRepository = NoteRepositoryImpl(database.noteDao())
        esp8266Service = Esp8266Service()
        esp32ConnectionManager = com.day.app.esp8266.api.Esp32ConnectionManager(settingsRepository)
        esp8266Repository = Esp8266Repository(esp8266Service, settingsRepository, esp32ConnectionManager)

        // 3. Background non-blocking ESP32 connection attempt (never blocks startup)
        val configuredIp = settingsRepository.getEsp8266Ip().trim()
        if (configuredIp.isNotBlank() && settingsRepository.isAutoSyncEspEnabled()) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                esp32ConnectionManager.connect(configuredIp)
            }
        }
    }
}

