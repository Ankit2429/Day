package com.day.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

/**
 * THE DAY — Dedicated Notification Channels
 *
 * Configures three distinct tiers of attention:
 * 1. THE DAY · Normal: Standard reminders, quiet tone, short vibration (IMPORTANCE_DEFAULT)
 * 2. THE DAY · Important: Heads-up alerts, two-part vibration, lights (IMPORTANCE_HIGH)
 * 3. THE DAY · Critical: High-attention heads-up, strong multi-pulse vibration (IMPORTANCE_HIGH)
 */
object NotificationChannels {
    const val CHANNEL_NORMAL = "com.day.app.channel.normal"
    const val CHANNEL_IMPORTANT = "com.day.app.channel.important"
    const val CHANNEL_CRITICAL = "com.day.app.channel.critical"

    // Vibration patterns
    val VIBRATION_NORMAL = longArrayOf(0, 150)
    val VIBRATION_IMPORTANT = longArrayOf(0, 250, 150, 250)
    val VIBRATION_CRITICAL = longArrayOf(0, 300, 150, 300, 150, 600)

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Remove legacy channels if present
            try {
                notificationManager.deleteNotificationChannel("NORMAL_REMINDERS")
                notificationManager.deleteNotificationChannel("IMPORTANT_REMINDERS")
                notificationManager.deleteNotificationChannel("URGENT_REMINDERS")
            } catch (_: Exception) {}

            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            // 1. THE DAY · Normal
            val normalChannel = NotificationChannel(
                CHANNEL_NORMAL,
                "THE DAY · Normal",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Standard scheduled task reminders"
                enableVibration(true)
                vibrationPattern = VIBRATION_NORMAL
                setSound(defaultSound, audioAttributes)
            }

            // 2. THE DAY · Important
            val importantChannel = NotificationChannel(
                CHANNEL_IMPORTANT,
                "THE DAY · Important",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up reminders for important tasks and approaching deadlines"
                enableVibration(true)
                vibrationPattern = VIBRATION_IMPORTANT
                enableLights(true)
                lightColor = Color.WHITE
                setSound(defaultSound, audioAttributes)
            }

            // 3. THE DAY · Critical
            val criticalChannel = NotificationChannel(
                CHANNEL_CRITICAL,
                "THE DAY · Critical",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-attention alerts for critical tasks and time-to-start events"
                enableVibration(true)
                vibrationPattern = VIBRATION_CRITICAL
                enableLights(true)
                lightColor = Color.WHITE
                setSound(defaultSound, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(
                listOf(normalChannel, importantChannel, criticalChannel)
            )
        }
    }
}
