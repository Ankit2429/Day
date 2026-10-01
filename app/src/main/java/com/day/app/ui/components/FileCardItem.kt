package com.day.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.app.domain.model.Document
import com.day.app.ui.theme.DayBorderSubtle
import com.day.app.ui.theme.DaySurfaceElevated
import com.day.app.ui.theme.DayTextPrimary
import com.day.app.ui.theme.DayTextSecondary
import com.day.app.ui.theme.DayTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileCardItem(
    document: Document,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(document.createdAt))
    val ext = document.extension.uppercase()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Very small format tag: [PDF], [DOC], [IMG]
        val tagShape = RoundedCornerShape(4.dp)
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(22.dp)
                .clip(tagShape)
                .background(Color(0x18FFFFFF), tagShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ext.take(3),
                color = DayTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name & Meta
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = document.displayName.ifBlank { document.fileName },
                color = DayTextPrimary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${document.formattedSize()} · $formattedDate",
                color = DayTextTertiary,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp
            )
        }

        // Storage / Sync Status Badge (LOCAL vs TELEGRAM)
        val isTelegram = document.storageProvider == "TELEGRAM" || document.telegramFileId != null
        val statusText = if (isTelegram) "TELEGRAM" else "LOCAL"
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isTelegram) Color(0x18FFFFFF) else Color(0x0CFFFFFF))
                .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = statusText,
                color = if (isTelegram) DayTextPrimary else DayTextTertiary,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}
