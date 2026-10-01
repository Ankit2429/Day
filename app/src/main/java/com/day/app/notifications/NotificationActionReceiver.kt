package com.day.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.day.app.DayApplication
import com.day.app.alarms.AlarmScheduler
import com.day.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val app = context.applicationContext as DayApplication
        val repository = app.taskRepository
        val espRepository = app.esp8266Repository

        when (intent.action) {
            NotificationHelper.ACTION_TASK_COMPLETE -> {
                NotificationHelper.cancelNotification(context, taskId)
                AlarmScheduler.cancelAlarm(context, taskId)

                CoroutineScope(Dispatchers.IO).launch {
                    espRepository.clearDisplay()
                    val completed = repository.completeTask(taskId, true)
                    if (completed != null) {
                        WidgetUpdater.updateAllWidgets(context)
                        espRepository.sendTaskCompletion(taskId, completed.title)
                        val active = repository.getActiveTasksSync()
                        espRepository.syncActiveTasks(active)
                    }
                }
            }

            NotificationHelper.ACTION_TASK_SNOOZE -> {
                val minutes = intent.getLongExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, 15L)
                NotificationHelper.cancelNotification(context, taskId)

                CoroutineScope(Dispatchers.IO).launch {
                    espRepository.clearDisplay()
                    val task = repository.getTaskByIdSync(taskId)
                    if (task != null) {
                        val snoozeTriggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                        AlarmScheduler.scheduleExactAlarm(context, task, snoozeTriggerTime)
                        val active = repository.getActiveTasksSync()
                        espRepository.syncActiveTasks(active)
                    }
                }
            }
        }
    }
}
