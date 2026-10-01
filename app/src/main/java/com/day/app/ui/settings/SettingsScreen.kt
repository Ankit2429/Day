package com.day.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.components.NeoButton
import com.day.app.ui.components.NeoButtonVariant
import com.day.app.ui.components.NeoHeader
import com.day.app.ui.components.NeoInput
import com.day.app.ui.components.NeoToggle
import com.day.app.ui.theme.DayBackground
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DayDivider
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DayTextDisabled
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToEsp8266: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DayBackground)
    ) {
        NeoHeader(title = "SETTINGS")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // 1. APPEARANCE
            SectionHeader("APPEARANCE")
            SettingRow(
                title = "Appearance",
                subtitle = "Minimal Dark Monochrome",
                action = {
                    Text(
                        text = "Active",
                        color = DayTextTertiary,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp
                    )
                }
            )
            HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

            SettingRow(
                title = "Time Format",
                subtitle = "12-hour (h:mm AM/PM)",
                action = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DaySurfaceElevated)
                            .border(0.5.dp, DayBorderSubtle, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "12-HOUR",
                            color = DayTextPrimary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            )
            HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(28.dp))

            // 2. NOTIFICATIONS
            SectionHeader("NOTIFICATIONS")
            SettingRow(
                title = "Notifications",
                subtitle = "Alert notifications for tasks and reminders",
                action = {
                    NeoToggle(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) }
                    )
                }
            )
            HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

            if (state.notificationsEnabled) {
                // Link to Android System Notification Settings
                val context = androidx.compose.ui.platform.LocalContext.current
                SettingRow(
                    title = "System Notification Settings",
                    subtitle = "Manage Android channels, lock screen & importance",
                    onClick = {
                        val intent = android.content.Intent().apply {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            } else {
                                action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    action = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Settings",
                            tint = DayTextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

                SettingRow(
                    title = "Notification Sound",
                    subtitle = "Play sound for task reminders",
                    action = {
                        NeoToggle(
                            checked = state.notificationSound,
                            onCheckedChange = { viewModel.toggleNotificationSound(it) }
                        )
                    }
                )
                HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

                SettingRow(
                    title = "Notification Vibration",
                    subtitle = "Haptic feedback for alerts",
                    action = {
                        NeoToggle(
                            checked = state.notificationVibration,
                            onCheckedChange = { viewModel.toggleNotificationVibration(it) }
                        )
                    }
                )
                HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

                SettingRow(
                    title = "Persistent Critical Alerts",
                    subtitle = "Keep critical alerts visible until Done or Snooze",
                    action = {
                        NeoToggle(
                            checked = state.persistentCriticalAlerts,
                            onCheckedChange = { viewModel.togglePersistentCriticalAlerts(it) }
                        )
                    }
                )
                HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

                SettingRow(
                    title = "Reminder Escalation",
                    subtitle = "30m (normal) → 10m (important) → On time (critical)",
                    action = {
                        NeoToggle(
                            checked = state.reminderEscalation,
                            onCheckedChange = { viewModel.toggleReminderEscalation(it) }
                        )
                    }
                )
                HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

                SettingRow(
                    title = "Quiet Hours",
                    subtitle = "23:00 — 07:00 (Mutes non-critical alerts)",
                    action = {
                        NeoToggle(
                            checked = state.quietHoursEnabled,
                            onCheckedChange = { viewModel.toggleQuietHours(it) }
                        )
                    }
                )
                HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

                // Snooze duration selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Default Snooze",
                            color = DayTextPrimary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Quick snooze duration on notifications",
                            color = DayTextSecondary,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5L, 10L, 30L).forEach { mins ->
                            val isSelected = state.defaultSnoozeMinutes == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF26262B) else Color(0xFF121214))
                                    .border(
                                        0.5.dp,
                                        if (isSelected) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setDefaultSnoozeMinutes(mins) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${mins}M",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else DayTextTertiary
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = DayDivider, thickness = 0.5.dp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. STORAGE
            SectionHeader("STORAGE")
            SettingRow(
                title = "Storage",
                subtitle = "Local Database & Documents",
                action = {
                    Text(
                        text = "Encrypted",
                        color = DayTextTertiary,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp
                    )
                }
            )
            HorizontalDivider(color = DayDivider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))
            NeoInput(
                value = state.telegramServerUrl,
                onValueChange = { viewModel.updateTelegramServerUrl(it) },
                label = "TELEGRAM STORAGE ENDPOINT",
                placeholder = "https://your-relay-server.com/api"
            )
            Spacer(modifier = Modifier.height(10.dp))
            NeoInput(
                value = state.telegramChatId,
                onValueChange = { viewModel.updateTelegramChatId(it) },
                label = "STORAGE CHANNEL ID",
                placeholder = "@my_storage_channel"
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(28.dp))

            // 4. DEVICE
            SectionHeader("DEVICE")
            SettingRow(
                title = "ESP8266",
                subtitle = "MAX7219 matrix · WebSocket (Port 81)",
                onClick = onNavigateToEsp8266,
                action = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Configure",
                        tint = DayTextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            HorizontalDivider(color = DayDivider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))
            NeoInput(
                value = state.esp8266Ip,
                onValueChange = { viewModel.updateEsp8266Ip(it) },
                label = "ESP8266 IP",
                placeholder = "192.168.1.105"
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(28.dp))

            // 5. PRIVACY & SECURITY
            SectionHeader("PRIVACY")
            SettingRow(
                title = "Privacy & Security",
                subtitle = "On-device processing with zero analytics"
            )
            HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(28.dp))

            // 6. ABOUT
            SectionHeader("ABOUT")
            SettingRow(
                title = "DAY",
                subtitle = "Version 1.0.0 · Your Daily Operating System",
                action = {
                    Text(
                        text = "Build 2026.09",
                        color = DayTextTertiary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            )
            HorizontalDivider(color = DayDivider, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = DayTextTertiary,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DayTextPrimary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = DayTextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp
                )
            }
        }

        if (action != null) {
            action()
        }
    }
}
