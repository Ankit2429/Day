package com.day.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DayDestructive
import com.day.app.ui.theme.DayInfo
import com.day.app.ui.theme.DaySuccess
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayWarning

enum class NeoAlertVariant {
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}

@Composable
fun NeoAlertBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    variant: NeoAlertVariant = NeoAlertVariant.INFO,
    onDismiss: (() -> Unit)? = null
) {
    val (iconColor, icon) = when (variant) {
        NeoAlertVariant.SUCCESS -> DaySuccess to Icons.Default.CheckCircle
        NeoAlertVariant.WARNING -> DayWarning to Icons.Default.Warning
        NeoAlertVariant.ERROR -> DayDestructive to Icons.Default.Warning
        NeoAlertVariant.INFO -> DayInfo to Icons.Default.Info
    }

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DaySurfaceElevated, shape)
            .border(1.dp, DayBorderSubtle, shape)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = DayTextPrimary,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = message,
                    color = DayTextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                )
            }

            if (onDismiss != null) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = DayTextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                )
            }
        }
    }
}
