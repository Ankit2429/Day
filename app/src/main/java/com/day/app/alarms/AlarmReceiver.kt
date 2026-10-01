package com.day.app.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.day.app.DayApplication
import com.day.app.domain.model.Priority
import com.day.app.domain.model.ReminderEndCondition
import com.day.app.domain.model.Urgency
import com.day.app.notifications.NotificationHelper
import com.day.app.notifications.ReminderStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val app = context.applicationContext as DayApplication
        val repository = app.taskRepository
        val espRepository = app.esp8266Repository

        val stage = when (intent.action) {
            AlarmScheduler.ACTION_TRIGGER_DEADLINE -> ReminderStage.DEADLINE_OVERDUE
            AlarmScheduler.ACTION_TRIGGER_ESCALATION_10M -> ReminderStage.UPCOMING_10M
            AlarmScheduler.ACTION_TRIGGER_ESCALATION_30M -> ReminderStage.UPCOMING_30M
            else -> ReminderStage.AT_TIME
        }

        CoroutineScope(Dispatchers.IO).launch {
            val task = repository.getTaskByIdSync(taskId) ?: return@launch
            if (task.completed) {
                AlarmScheduler.cancelAlarm(context, taskId)
                return@launch
            }

            // 1. Fire Prominent High-Attention Notification
            NotificationHelper.showTaskReminderNotification(context, task, stage)

            // 2. Broadcast Alert to Physical MAX7219 Matrix
            if (task.priority == Priority.HIGH || task.urgency == Urgency.URGENT ||
                task.urgency == Urgency.OVERDUE || stage == ReminderStage.DEADLINE_OVERDUE ||
                stage == ReminderStage.AT_TIME
            ) {
                val targetMs = task.scheduledTime ?: task.deadline ?: System.currentTimeMillis()
                val timeStr = com.day.app.util.TimeFormatter.formatUserTime(targetMs)
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = targetMs }
                val hr = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val mn = cal.get(java.util.Calendar.MINUTE)
                espRepository.sendReminderAlert(task.title, task.urgency, timeStr, hr, mn)
            }

            // 3. Repeated Reminders Handling:
            val intervalMinutes = task.reminderInterval
            if (intervalMinutes != null && intervalMinutes > 0L) {
                val now = System.currentTimeMillis()
                val shouldContinue = when (task.reminderEndCondition) {
                    ReminderEndCondition.COMPLETED -> !task.completed
                    ReminderEndCondition.DEADLINE -> task.deadline == null || now < task.deadline
                    ReminderEndCondition.CUSTOM -> true
                }

                if (shouldContinue) {
                    val nextTrigger = now + (intervalMinutes * 60 * 1000L)
                    AlarmScheduler.scheduleExactAlarm(context, task, nextTrigger)
                }
            }
        }
    }
}
