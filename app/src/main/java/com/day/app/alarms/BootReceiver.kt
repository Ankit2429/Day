package com.day.app.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.day.app.DayApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val app = context.applicationContext as DayApplication
            val repository = app.taskRepository

            CoroutineScope(Dispatchers.IO).launch {
                val activeTasks = repository.getActiveTasksSync()
                for (task in activeTasks) {
                    AlarmScheduler.scheduleTaskReminders(context, task)
                }
            }
        }
    }
}
