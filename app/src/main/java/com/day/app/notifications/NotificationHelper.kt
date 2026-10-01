package com.day.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.day.app.DayApplication
import com.day.app.MainActivity
import com.day.app.R
import com.day.app.domain.model.Priority
import com.day.app.domain.model.Task
import com.day.app.domain.model.Urgency
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * THE DAY — Notification Priority Levels
 */
enum class NotificationPriorityLevel {
    NORMAL,
    IMPORTANT,
    CRITICAL
}

/**
 * Stages of reminder escalation
 */
enum class ReminderStage {
    UPCOMING_30M,
    UPCOMING_10M,
    AT_TIME,
    DEADLINE_OVERDUE
}

object NotificationHelper {

    const val ACTION_TASK_COMPLETE = "com.day.app.ACTION_TASK_COMPLETE"
    const val ACTION_TASK_SNOOZE = "com.day.app.ACTION_TASK_SNOOZE"
    const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
    const val EXTRA_SNOOZE_MINUTES = "EXTRA_SNOOZE_MINUTES"

    private const val GROUP_KEY_TASKS = "com.day.app.TASK_REMINDERS"

    fun showTaskReminderNotification(
        context: Context,
        task: Task,
        stage: ReminderStage = ReminderStage.AT_TIME
    ) {
        val app = context.applicationContext as? DayApplication
        val settingsRepo = app?.settingsRepository

        // If master switch disabled, do not show
        if (settingsRepo?.isNotificationsEnabled() == false) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channels are created
        NotificationChannels.createChannels(context)

        // 1. Determine Level
        val level = determinePriorityLevel(task, stage)

        // 2. Check Quiet Hours
        val inQuietHours = settingsRepo?.let { isInQuietHours(it) } ?: false

        // 3. Channel Selection
        val channelId = when (level) {
            NotificationPriorityLevel.NORMAL -> NotificationChannels.CHANNEL_NORMAL
            NotificationPriorityLevel.IMPORTANT -> NotificationChannels.CHANNEL_IMPORTANT
            NotificationPriorityLevel.CRITICAL -> NotificationChannels.CHANNEL_CRITICAL
        }

        // 4. Formulate Short Direct Title & Content
        val scheduledStr = task.scheduledTime?.let { com.day.app.util.TimeFormatter.formatUserTime(it) }
        val deadlineStr = task.deadline?.let { com.day.app.util.TimeFormatter.formatUserTime(it) }

        val title: String
        val content: String

        when (stage) {
            ReminderStage.DEADLINE_OVERDUE -> {
                title = "OVERDUE"
                content = "${task.title}\nDeadline: ${deadlineStr ?: "Passed"}"
            }
            ReminderStage.UPCOMING_30M -> {
                title = "UPCOMING"
                content = "${task.title}\nStarts in 30 minutes (${scheduledStr ?: ""})"
            }
            ReminderStage.UPCOMING_10M -> {
                title = "UP NEXT"
                content = "${task.title}\nStarts in 10 minutes (${scheduledStr ?: ""})"
            }
            ReminderStage.AT_TIME -> {
                when (level) {
                    NotificationPriorityLevel.CRITICAL -> {
                        title = "⚠ TIME TO START"
                        content = "${task.title}\n${scheduledStr?.let { "Scheduled for $it" } ?: (deadlineStr?.let { "Deadline: $it" } ?: "")}".trim()
                    }
                    NotificationPriorityLevel.IMPORTANT -> {
                        title = "Important · ${task.title}"
                        content = scheduledStr?.let { "Scheduled for $it" } ?: (deadlineStr?.let { "Deadline: $it" } ?: "Due now")
                    }
                    NotificationPriorityLevel.NORMAL -> {
                        title = task.title
                        content = scheduledStr?.let { "Scheduled for $it" } ?: (deadlineStr?.let { "Deadline: $it" } ?: "Reminder")
                    }
                }
            }
        }

        // 5. Open Task Intent (Launched with smooth deep link to task details)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TASK_ID", task.id)
            putExtra("EXTRA_HIGHLIGHT_TASK", true)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 6. Complete Action Intent
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_TASK_COMPLETE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 7. Snooze Action Intent (Uses configured snooze default: 5, 10, 30 min)
        val defaultSnooze = settingsRepo?.getDefaultSnoozeMinutes() ?: 10L
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_TASK_SNOOZE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_SNOOZE_MINUTES, defaultSnooze)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 8. Construct Notification Builder
        val isCritical = level == NotificationPriorityLevel.CRITICAL
        val isImportant = level == NotificationPriorityLevel.IMPORTANT

        val isPersistent = isCritical && (settingsRepo?.isPersistentCriticalAlertsEnabled() != false)

        val soundEnabled = settingsRepo?.isNotificationSoundEnabled() != false && !inQuietHours
        val vibrationEnabled = settingsRepo?.isNotificationVibrationEnabled() != false && !inQuietHours

        val vibrationPattern = when {
            !vibrationEnabled -> longArrayOf(0)
            isCritical -> NotificationChannels.VIBRATION_CRITICAL
            isImportant -> NotificationChannels.VIBRATION_IMPORTANT
            else -> NotificationChannels.VIBRATION_NORMAL
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_day)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(
                when (level) {
                    NotificationPriorityLevel.CRITICAL -> NotificationCompat.PRIORITY_MAX
                    NotificationPriorityLevel.IMPORTANT -> NotificationCompat.PRIORITY_HIGH
                    NotificationPriorityLevel.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setCategory(
                if (isCritical) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent)
            .setOngoing(isPersistent)
            .setAutoCancel(!isPersistent)
            .setVibrate(vibrationPattern)
            .setGroup(GROUP_KEY_TASKS)

        if (!soundEnabled) {
            builder.setSilent(true)
        }

        // Action Buttons: DONE, SNOOZE, OPEN
        builder.addAction(
            android.R.drawable.checkbox_on_background,
            "DONE",
            completePendingIntent
        )
        builder.addAction(
            android.R.drawable.ic_popup_reminder,
            "SNOOZE (${defaultSnooze}M)",
            snoozePendingIntent
        )
        builder.addAction(
            android.R.drawable.ic_menu_agenda,
            "OPEN",
            openPendingIntent
        )

        notificationManager.notify(task.id.toInt(), builder.build())
    }

    fun cancelNotification(context: Context, taskId: Long) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.toInt())
    }

    private fun determinePriorityLevel(task: Task, stage: ReminderStage): NotificationPriorityLevel {
        return when (stage) {
            ReminderStage.DEADLINE_OVERDUE -> NotificationPriorityLevel.CRITICAL
            ReminderStage.UPCOMING_10M -> NotificationPriorityLevel.IMPORTANT
            ReminderStage.UPCOMING_30M -> NotificationPriorityLevel.NORMAL
            ReminderStage.AT_TIME -> {
                when {
                    task.priority == Priority.HIGH || task.urgency == Urgency.URGENT || task.urgency == Urgency.OVERDUE ->
                        NotificationPriorityLevel.CRITICAL
                    task.priority == Priority.MEDIUM || task.urgency == Urgency.IMPORTANT ->
                        NotificationPriorityLevel.IMPORTANT
                    else ->
                        NotificationPriorityLevel.NORMAL
                }
            }
        }
    }

    private fun isInQuietHours(repository: com.day.app.data.repository.SettingsRepository): Boolean {
        if (!repository.isQuietHoursEnabled()) return false
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val start = repository.getQuietHoursStart()
        val end = repository.getQuietHoursEnd()
        return if (start > end) {
            hour >= start || hour < end
        } else {
            hour in start until end
        }
    }
}
