package com.day.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {
    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SmallWidget().updateAll(context)
                LargeWidget().updateAll(context)
            } catch (e: Exception) {
                // Ignore widget update errors if Glance receiver isn't registered yet
            }
        }
    }
}
