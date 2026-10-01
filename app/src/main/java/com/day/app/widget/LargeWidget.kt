package com.day.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.day.app.DayApplication
import com.day.app.MainActivity
import com.day.app.ui.theme.NeoBlack
import com.day.app.ui.theme.NeoPaper
import com.day.app.ui.theme.NeoTeal
import com.day.app.ui.theme.NeoYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LargeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DayApplication
        val activeTasks = app.taskRepository.getActiveTasksSync()
        val displayTasks = activeTasks.take(3)

        val todayDate = SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date()).uppercase()

        provideContent {
            val addTaskIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_NAVIGATE_ADD_TASK", true)
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(NeoPaper)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .background(NeoYellow)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "TODAY • $todayDate",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = androidx.glance.unit.ColorProvider(NeoBlack)
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        Box(
                            modifier = GlanceModifier
                                .background(NeoTeal)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${activeTasks.size} ACTIVE",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = androidx.glance.unit.ColorProvider(NeoBlack)
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    if (displayTasks.isEmpty()) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(NeoPaper)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "ALL TASKS COMPLETED",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = androidx.glance.unit.ColorProvider(NeoBlack)
                                )
                            )
                        }
                    } else {
                        displayTasks.forEach { task ->
                            val taskIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("EXTRA_NAVIGATE_TASK_ID", task.id)
                            }

                            Column(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable(actionStartActivity(taskIntent))
                            ) {
                                Text(
                                    text = task.title.uppercase(),
                                    maxLines = 1,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = androidx.glance.unit.ColorProvider(NeoBlack)
                                    )
                                )

                                val timeStr = buildString {
                                    task.formattedScheduledTime()?.let { append(it) }
                                    task.formattedDeadline()?.let {
                                        if (isNotEmpty()) append(" • ")
                                        append("DEADLINE $it")
                                    }
                                }

                                if (timeStr.isNotEmpty()) {
                                    Text(
                                        text = timeStr,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            color = androidx.glance.unit.ColorProvider(Color(0xFF555555))
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = GlanceModifier.height(3.dp))
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // "+ ADD TASK" Button
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(NeoBlack)
                            .padding(vertical = 8.dp)
                            .clickable(actionStartActivity(addTaskIntent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ ADD TASK",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = androidx.glance.unit.ColorProvider(Color.White)
                            )
                        )
                    }
                }
            }
        }
    }
}

class LargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeWidget()
}
