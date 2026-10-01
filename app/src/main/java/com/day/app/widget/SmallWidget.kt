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
import androidx.glance.appwidget.cornerRadius
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

class SmallWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DayApplication
        val activeTasks = app.taskRepository.getActiveTasksSync()
        val firstTask = activeTasks.firstOrNull()
        val remainingCount = if (activeTasks.size > 1) activeTasks.size - 1 else 0

        val todayDate = SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date()).uppercase()

        provideContent {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(NeoPaper)
                    .padding(8.dp)
                    .clickable(actionStartActivity(mainIntent))
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(10.dp)
                ) {
                    // Header Tag
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .background(NeoYellow)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TODAY",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = androidx.glance.unit.ColorProvider(NeoBlack)
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    if (firstTask != null) {
                        Text(
                            text = firstTask.title.uppercase(),
                            maxLines = 1,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = androidx.glance.unit.ColorProvider(NeoBlack)
                            )
                        )

                        val timeStr = firstTask.formattedScheduledTime()
                            ?: firstTask.formattedDeadline()
                            ?: "TODAY"

                        Text(
                            text = timeStr,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = androidx.glance.unit.ColorProvider(Color(0xFF444444))
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        if (remainingCount > 0) {
                            Box(
                                modifier = GlanceModifier
                                    .background(NeoTeal)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$remainingCount MORE TASKS",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = androidx.glance.unit.ColorProvider(NeoBlack)
                                    )
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "NO TASKS",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = androidx.glance.unit.ColorProvider(NeoBlack)
                            )
                        )
                        Text(
                            text = "ALL CLEAR",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = androidx.glance.unit.ColorProvider(Color(0xFF666666))
                            )
                        )
                    }
                }
            }
        }
    }
}

class SmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallWidget()
}
