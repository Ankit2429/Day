package com.day.app.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.day.app.DayApplication
import com.day.app.domain.model.Task

object AlarmScheduler {

    const val ACTION_TRIGGER_REMINDER = "com.day.app.ACTION_TRIGGER_REMINDER"
    const val ACTION_TRIGGER_DEADLINE = "com.day.app.ACTION_TRIGGER_DEADLINE"
    const val ACTION_TRIGGER_ESCALATION_10M = "com.day.app.ACTION_TRIGGER_ESCALATION_10M"
    const val ACTION_TRIGGER_ESCALATION_30M = "com.day.app.ACTION_TRIGGER_ESCALATION_30M"
    const val EXTRA_TASK_ID = "EXTRA_TASK_ID"

    fun scheduleTaskReminders(context: Context, task: Task) {
        if (task.completed) {
            cancelAlarm(context, task.id)
            return
        }

        val app = context.applicationContext as? DayApplication
        val settingsRepo = app?.settingsRepository
        val now = System.currentTimeMillis()

        // 1. Primary Reminder at scheduled time
        if (task.reminderEnabled && task.scheduledTime != null && task.scheduledTime > now) {
            scheduleAlarmWithAction(
                context = context,
                task = task,
                triggerTimeMillis = task.scheduledTime,
                action = ACTION_TRIGGER_REMINDER,
                requestCode = (task.id * 100 + 1).toInt()
            )

            // Optional Reminder Escalation
            if (settingsRepo?.isReminderEscalationEnabled() == true) {
                val t30 = task.scheduledTime - 30 * 60 * 1000L
                if (t30 > now) {
                    scheduleAlarmWithAction(
                        context = context,
                        task = task,
                        triggerTimeMillis = t30,
                        action = ACTION_TRIGGER_ESCALATION_30M,
                        requestCode = (task.id * 100 + 10).toInt()
                    )
                }

                val t10 = task.scheduledTime - 10 * 60 * 1000L
                if (t10 > now) {
                    scheduleAlarmWithAction(
                        context = context,
                        task = task,
                        triggerTimeMillis = t10,
                        action = ACTION_TRIGGER_ESCALATION_10M,
                        requestCode = (task.id * 100 + 20).toInt()
                    )
                }
            }
        }

        // 2. Deadline Alert if configured and in future
        if (task.deadline != null && task.deadline > now) {
            scheduleAlarmWithAction(
                context = context,
                task = task,
                triggerTimeMillis = task.deadline,
                action = ACTION_TRIGGER_DEADLINE,
                requestCode = (task.id * 100 + 2).toInt()
            )
        }
    }

    fun scheduleExactAlarm(
        context: Context,
        task: Task,
        triggerTimeMillis: Long,
        isDeadline: Boolean = false
    ) {
        val action = if (isDeadline) ACTION_TRIGGER_DEADLINE else ACTION_TRIGGER_REMINDER
        val requestCode = if (isDeadline) (task.id * 100 + 2).toInt() else (task.id * 100 + 1).toInt()
        scheduleAlarmWithAction(context, task, triggerTimeMillis, action, requestCode)
    }

    private fun scheduleAlarmWithAction(
        context: Context,
        task: Task,
        triggerTimeMillis: Long,
        action: String,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TASK_ID, task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExact) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val actionsWithCodes = listOf(
            ACTION_TRIGGER_REMINDER to (taskId * 100 + 1).toInt(),
            ACTION_TRIGGER_DEADLINE to (taskId * 100 + 2).toInt(),
            ACTION_TRIGGER_ESCALATION_10M to (taskId * 100 + 20).toInt(),
            ACTION_TRIGGER_ESCALATION_30M to (taskId * 100 + 10).toInt()
        )

        for ((action, code) in actionsWithCodes) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                this.action = action
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
